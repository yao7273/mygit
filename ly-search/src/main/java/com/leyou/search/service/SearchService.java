package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchService {

    //使用feign客户端调用远程服务
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    //spu与goods的转换
    public Goods buildGoods(Spu spu) {

        //拼接搜索过滤的字段，有标题，分类名称，品牌名称
            //1.先查询分类名称
        List<String> cnames = categoryClient.queryByCids(Arrays
                .asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                .stream().map(category -> category.getName()).collect(Collectors.toList());
            //2.查询品牌名称
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
            //3.拼接为过滤字符串
        String all = spu.getTitle()+" "+ StringUtils.join(cnames," ")
                +" "+brand.getName();
        //**********************************************************
        //查询sku
        List<Sku> skus = goodsClient.querySkusByPid(spu.getId());
            //1.价格的集合(价格用于过滤字段查询，所以不需要重复)
        Set<Long> prices = new TreeSet<>();
            //设置sku集合的json字符串格式，页面显示只需要的sku中id，title。image，price
        List<Map<String,Object>> skuJson = new ArrayList<>();
            //遍历集合将其放入到list中
        for (Sku sku : skus) {
            //添加价格
            prices.add(sku.getPrice());

            Map<String,Object> map = new HashMap<>();
             //只添加页面需要的数据
            map.put("id",sku.getId());
            map.put("title",sku.getTitle());
            map.put("price",sku.getPrice());
            map.put("image",
                    StringUtils.isBlank(sku.getImages())
                        ? "" : StringUtils.substringBefore(sku.getImages(),","));

            skuJson.add(map);
        }

        //******************************************************
        //查询规格参数(可以用来过滤的规格参数)，将规格参数信息与详情信息里的值拼成map
            //1.先获取所有规格参数信息
        List<SpecParam> specParams = specificationClient.querySpecParam(null, spu.getCid3(), null, true);
            //2.或spuDetail，详情数据
        SpuDetail spuDetail = goodsClient.querySpuDetailByPid(spu.getId());
            //先解析spuDetail中的通用规格信息（spu中的）
        Map<String, Object> genericSpec = JsonUtils.nativeRead(spuDetail.getGenericSpec(),
                new TypeReference<Map<String, Object>>() {});
            //解析特有的规格参数信息（即sku中特有的）
        Map<String, List<Object>> specicalSpec = JsonUtils.nativeRead(spuDetail.getSpecialSpec(),
                new TypeReference<Map<String, List<Object>>>() {
                });

        //拼写规格参数map，其中key为规格参数名称，值为spudetail中数据
        Map<String,Object> specs = new HashMap<>();
        for (SpecParam specParam : specParams) {
            String key = specParam.getName();
            Object value = null;
            //判断是否为通用规格参数信息
            if(specParam.getGeneric()){
                //是，则从genericSpec中查找value
                value = genericSpec.get(specParam.getId().toString());
                //再判断是否为数值类型，用于分段信息
                if(specParam.getNumeric()){
                    //是数值类型，用于分段
                    value = chooseSegment(value.toString(),specParam);
                }
            }else {
                //不通用
                value = specicalSpec.get(specParam.getId().toString());
            }
            if(value == null){
                value = "其他";
            }
            specs.put(key,value);
        }
        //*************************************
        Goods goods = new Goods();

        goods.setId(spu.getId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setSubTitle(spu.getSubTitle());
        goods.setPrice(prices);  //当前spu下的所有sku的价格的集合
        goods.setSkus(JsonUtils.toString(skuJson));  // sku集合的json字符串
        goods.setSpecs(specs);  //o 当前分类下，所有可以用来搜索的规格参数
        goods.setAll(all);  // 搜索过滤的字段


        return goods;
    }

    //是数值类型，用于分段
    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    /**
     * 对索引库的数据的搜索与过滤
     * @param searchRequest
     * @return
     */
    public PageResult<Goods> search(SearchRequest searchRequest) {

        //先对搜索条件判断
        String key = searchRequest.getKey();
        if(StringUtils.isBlank(key)){
            throw new LyException(HttpStatus.BAD_REQUEST,"查询条件不能为空");
        }

        //创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //添加分页
        int page = searchRequest.getPage() - 1;
        int size = searchRequest.getSize();
        queryBuilder.withPageable(PageRequest.of(page, size));

        //控制返回的字段信息,只需要从索引库中提取用于页面展示的数据即可
        queryBuilder.withSourceFilter(
                new FetchSourceFilter(new String[]{"id","skus","subTitle"},null));
        //添加基本搜索条件,根据关键字查询全部
            //定义一个函数，添加基本搜索条件，在其基础上再添加过滤字段
        QueryBuilder basicQuery = handlerBasicQuery(searchRequest);
        queryBuilder.withQuery(basicQuery);

        //添加聚合条件，（用于聚合分类与品牌）
            //聚合分类
        String categoryAggsName = "categoryAggs";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggsName).field("cid3"));
            //聚合品牌
        String brandAggsName = "brandAggs";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggsName).field("brandId"));
        //获取聚合结果
       // Page<Goods> goodsPage = goodsRepository.search(queryBuilder.build());
        AggregatedPage<Goods> goodsPage = elasticsearchTemplate.queryForPage(queryBuilder.build(), Goods.class);
        //封装结果，返回页面
        long totalElements = goodsPage.getTotalElements();
        int totalPages = goodsPage.getTotalPages();
        List<Goods> goodsList = goodsPage.getContent();

        //解析聚合结果，将集合后的桶中数据取出
        Aggregations aggregations = goodsPage.getAggregations();
            //1.取出category聚合结果
            //1.1处理其中的结果，返回list<category>集合，
        List<Category> categories = handlerCategory( aggregations.get(categoryAggsName));

            //2.取出brand聚合结果
            //2.2处理结果，返回List<Brand>
        List<Brand> brands = handlerBrand(aggregations.get(brandAggsName));

        //对规格参数的聚合，先判断是否需要进行规格参数的聚合（当得到的分类结果个数为1时，才会进行聚合）
            //1.封装规格参数聚合后的的数据转换
        List<Map<String,Object>> specs = null;
        if(categories !=null && categories.size() ==1){
                //规格参数的聚合需要在搜索完的基础上进行，并且要根根据商品的分类进行
            specs = handleSpecs(categories.get(0).getId(),basicQuery);
        }

        //对查询的结果进行判断
        if(CollectionUtils.isEmpty(goodsList)){
            throw new LyException(HttpStatus.NOT_FOUND,"未找到相应数据");
        }

        return new SearchResult(totalElements,Long.valueOf(totalPages),goodsList,categories,brands,specs);
    }

    /**
     * 使用bool方式进行基本搜索与过滤
     * @param searchRequest
     * @return
     */
    private QueryBuilder handlerBasicQuery(SearchRequest searchRequest) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.添加基本搜索条件
        boolQuery.must(QueryBuilders.matchQuery("all",searchRequest.getKey()));
        //2.进行过滤
            //1.获取过滤条件
        Map<String, String> filterMap = searchRequest.getFilter();
            //2.遍历map进行过滤
        for (Map.Entry<String, String> entry : filterMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if(!"cid3".equals(key) && !"brandId".equals(key)){
                key = "specs."+key+".keyword";
            }
           // Long valueLong = null;
            if("cid3".equals(key) || "brandId".equals(key)){
                //将字符串转换为数值
               value  = Long.valueOf(value.toString());

            }
            // Object valueLast = valueLong==null ? value : valueLong ;
            //3.对每个词条都进行遍历
            boolQuery.filter(QueryBuilders.termQuery(key,value));
        }
        return boolQuery;
    }

    //对商品单个分类的规格参数进行聚合
    private List<Map<String, Object>> handleSpecs(Long cid, QueryBuilder basicQuery) {
        List<Map<String,Object>> specs = new ArrayList<>();
        //根据分类的id获取需要聚合的规格参数集合
        List<SpecParam> specParams = specificationClient.querySpecParam(null, cid, null, true);

        //添加聚合条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
            //1.在搜索条件的基础上聚合
        queryBuilder.withQuery(basicQuery);
            //2.减少查询的数量，在kibana中可以都设置为0，spring-data的整和中最少为1
        queryBuilder.withPageable(PageRequest.of(0,1));
            //3.遍历规格参数集合，对每个规格参数进行聚合
        for (SpecParam param : specParams) {
            String name = param.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs."+name+".keyword"));
        }

        //获取聚合结果,还要根据规格参数名取出聚合结果,并进行解析
            //1.获取分页与聚合的结果
        AggregatedPage<Goods> goods = elasticsearchTemplate.queryForPage(queryBuilder.build(), Goods.class);
            //2.只获取聚合结果
        Aggregations aggregations = goods.getAggregations();
        for (SpecParam param : specParams) {
            //3.获取单个聚合结果
            StringTerms terms = aggregations.get(param.getName());
            //4.使用流将每个桶内的key对应的值封装到集合中,同时提出其中的空值
            // List<String> options = terms.getBuckets().stream().map(bucket ->bucket.getKeyAsString()).filter(Objects::nonNull).collect(Collectors.toList());
            List<String> options = terms.getBuckets().stream().map(bucket ->{
                if(StringUtils.isBlank( bucket.getKeyAsString())){
                    return null;
                }
                return  bucket.getKeyAsString();
            }).filter(Objects::nonNull).collect(Collectors.toList());

            //5.将数据封装到map中，再添加到specs集合中
            Map<String,Object> map = new HashMap<>();
            map.put("k",param.getName());
            map.put("options",options);

            specs.add(map);
        }
        return specs;
    }

    private List<Brand> handlerBrand(LongTerms terms) {

        try {
            //从桶中获取数据，使用流的方式获取id集合
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());

            List<Brand> brands = brandClient.queryBrandsByIds(ids);
            return brands;
        } catch (Exception e) {
            log.error("解析品牌数据出错",e);
            return null;
        }
    }

    private List<Category> handlerCategory(LongTerms terms) {
        try {
            //从桶中获取数据，使用流的方式获取id集合
            List<Long> ids = terms.getBuckets().stream().map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());

            List<Category> categories = categoryClient.queryByCids(ids);
            return categories;
        } catch (Exception e) {
            log.error("解析分类数据出错",e);
            return null;
        }
    }

    /**
     * 监听商品微服务中的消息，完成索引库中数据的增改
     * @param id
     */
    public void insertOrUpdate(Long id) {
        //先根据spu_id查询出对应的spu信息
        Spu spu = goodsClient.querySpuByPid(id);
        //非空判断
        if(spu == null){
            log.error("索引对应的spuId在数据库中不存在，spuId：{}",id);
            //抛出异常，让消息回滚
            throw new RuntimeException();
        }
        //将spu转换成索引库中的goods
        Goods goods = buildGoods(spu);

        //????保存数据到索引库
        goodsRepository.save(goods);
    }

    /**
     * 监听商品微服务中的消息，完成索引库中数据的删除
     * @param id
     */
    public void delete(Long id) {
        goodsRepository.deleteById(id);
    }
}
