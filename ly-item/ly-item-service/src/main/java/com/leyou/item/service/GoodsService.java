package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.CartDTO;
import com.leyou.item.mapper.GoodsMapper;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 分页查询商品信息
     * @param key
     * @param saleable
     * @param page
     * @param rows
     * @return
     */
    public PageResult<Spu> querySpuByPage(String key, Boolean saleable, Integer page, Integer rows) {

        //开启分页
        PageHelper.startPage(page, rows);

        //使用通用mapper查询
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();

        //过滤

        //判断商品逻辑删除,没有删除进行查询（0为false，1为true）
        criteria.andEqualTo("valid", true);
        //先判断传输得数据是否为空，数据库判断判断商品是否下架,下架不进行查询
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        //判断搜索条件
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }

        List<Spu> spuList = goodsMapper.selectByExample(example);

        //判断查询的结果是否为空
        if (CollectionUtils.isEmpty(spuList)) {
            throw new LyException(HttpStatus.NO_CONTENT, "没有查询到商品信息");
        }

        //因为前端要求的商品分类为三个级别类别的字符串组合，所以要根据结果中的cid查询分类名称
        handlerCategoryAndBrandName(spuList);

        //该步骤主要作用是将spuList集合转换成Page类型
        PageInfo<Spu> pageInfo = new PageInfo<>(spuList);

        return new PageResult<>(pageInfo.getTotal(), spuList);
    }


    //因为前端要求的商品分类为三个级别类别的字符串组合，所以要根据结果中的cid查询分类名称
    private void handlerCategoryAndBrandName(List<Spu> spuList) {

        for (Spu spu : spuList) {

            //根据商品分类id查询分类，属于商品分类的业务，
            List<Category> categoryList = categoryService.selectByIdList(
                    Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            //判断查询到的分类信息是否为空
            if (CollectionUtils.isEmpty(categoryList)) {
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "商品所属分类不存在");
            }

            //使用jdk8的方法，获取集合中的name集合
            List<String> nameList = categoryList.stream().map(c -> c.getName()).collect(Collectors.toList());
            //将name集合组成字符串,并修改spu中的canme
            spu.setCname(StringUtils.join(nameList, "/"));

            //根据品牌id查询品牌
            Brand brand = brandService.queryByid(spu.getBrandId());
            //再判断查询到的数据是否存在
            if (brand == null) {
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "商品所属品牌不存在");
            }
            //修改spu中的bname
            spu.setBname(brand.getName());
        }
    }

    /**
     * 向数据库中插入Sku数据，spu，detail，scokt
     *
     * @param spu
     */
    @Transactional//添加事务
    public void saveGoods(Spu spu) {
        //新增spu表数据,同时对其他信息进行设置,前台已经传输，不需要自定义（*****先测试）
        spu.setId(null);

        spu.setSaleable(true);
        spu.setValid(true);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        goodsMapper.insertSelective(spu);
        //新增spu_detial表数据,由于detial表中的spu_id需要，（spu插入数据库后会出现主键回显）
        Long spuId = spu.getId();
        spu.getSpuDetail().setSpuId(spuId);
        spuDetailMapper.insertSelective(spu.getSpuDetail());
        //新增sku表数据，（sku表中也有需要后台自定义的参数信息）
        saveSkuAndStock(spu);

        //发送消息mq
        sendMessage("insert",spuId);


        //1.在for循环中进行数据的插入，不好，效率低，且会与数据库进行多次交互
        //2.并且在使用insertList批量向数据库插入的数据，不会又主键回显，所以其sku_id要自己再回传
        /*Stock stock = new Stock();
        for (Sku sku : spu.getSkus()) {
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockMapper.insertSelective(stock);
        }*/


    }


    //将新增sku与stock单独取出为一个方法,再生产中最好不要有太多的对象，消耗性能
    private void saveSkuAndStock(Spu spu) {
        Long spuId = spu.getId();
        //不适用该方法 2.并且在使用insertList批量向数据库插入的数据，不会又主键回显，所以其sku_id要自己再回传，
        //skuMapper.insertList(skuList1);//

        //新增stock表数据（库存表与sku向关联）
            /*
                思路：由于库存数据量值存在sku的实体类中，sku的数据表中没有该字段，
                库存表中需要存储sku的id与库存量的值，
                只能在向数据库插入sku时，利用主键回显得到spu的id，
                再进行stock的插入
             */
        //创建stock集合对象，用于批量插入stock数据
        List<Stock> stocks = new ArrayList<>();
        //利用for循环，向数据库中插入数据
        for (Sku sku : spu.getSkus()) {
            sku.setSpuId(spuId);
            sku.setId(null);
            sku.setEnable(true);
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            //向数据库插入数据
            skuMapper.insertSelective(sku);

            Stock stock = new Stock();
            stock.setStock(sku.getStock());
            stock.setSkuId(sku.getId());
            stocks.add(stock);
        }

        //批量向数据库插入stock数据
        stockMapper.insertList(stocks);
    }

    /**
     * 根据spu的id查询商品详情信息
     * @param pid
     * @return
     */
    public SpuDetail querySpuDetailByPid(Long pid) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(pid);

        if(spuDetail == null){
            throw new LyException(HttpStatus.NOT_FOUND,"没有找到商品详情信息");
        }
        return spuDetail;
    }


    /**
     * 根据spu_id查询skus（商品信息）,同时将库存数据也一起查询处放入到sku对象中
     * @param id
     * @return
     */
    public List<Sku> querySkusByPid(Long id) {

        Sku sku = new Sku();
        sku.setSpuId(id);
        List<Sku> skus = skuMapper.select(sku);
        //异常的健壮性处理
        if(CollectionUtils.isEmpty(skus)){
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR,"没找到相应的商品");
        }
        //根据sku_id查找数据库，为skus集合中的每个 sku对象附上库存数据,还是因为for循环中进行数据库的操作不好，所以修改
       /* for (Sku skus1 : skus) {
            Stock stock = stockMapper.selectByPrimaryKey(skus1.getId());
            if(stock == null){
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR,"商品库存查找失败");
            }
            skus1.setStock(stock.getStock());
        }*/

       //修改的方法1.先根据sku_ids查询stock的集合-->java处理
        List<Long> idList = skus.stream().map(s -> s.getId()).collect(Collectors.toList());
        List<Stock> stockList = stockMapper.selectByIdList(idList);

        //将stockList集合转换成map集合，即可实现id与库存值得对应
        Map<Long,Integer> map = new HashMap<>();
        for (Stock stock : stockList) {
            map.put(stock.getSkuId(),stock.getStock());
        }

        //修改sku集合中得stock值
        for (Sku skus1 : skus) {
            skus1.setStock(map.get(skus1.getId()));
        }
        return skus;
    }

    /**
     * 更新商品信息。其中商品的sku表中的数据需要全部先删除然后再新增新的数据
     * @param spu
     */
    @Transactional
    public void updateGoods(Spu spu) {
        //先判断传如的数据是否有spu_id
        if(spu.getId()==null){
            throw new LyException(HttpStatus.BAD_REQUEST,"商品id不能为空");
        }
        
        //删除spu下的sku数据(因为要删除库存数据要需要sku的id，所以要先查询处sku)
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        List<Sku> skus = skuMapper.select(sku);
        //判断sku是否存在，存在则删除
        if(CollectionUtils.isNotEmpty(skus)){
            //删除相应的库存数据
            List<Long> idList = skus.stream().map(s -> s.getId()).collect(Collectors.toList());
            stockMapper.deleteByIdList(idList);
            //删除sku表数据
            skuMapper.delete(sku);
        }

        //更新spu中的数据
            //先修改其中的数据
        spu.setLastUpdateTime(new Date());
        spu.setValid(null);
        spu.setCreateTime(null);
        spu.setSaleable(null);

        goodsMapper.updateByPrimaryKeySelective(spu);
        //更新spu_detial中商品详情信息
        spuDetailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());

        //新增sku与stock
        saveSkuAndStock(spu);

        //发送修改商品数据得消息
        sendMessage("update", spu.getId());
    }

    /**
     * 删除商品（逻辑删除）
     * @param spuid
     */
    public void deleteGood(Long spuid) {

        Spu spu = new Spu();
        spu.setId(spuid);
        spu.setValid(false);
        goodsMapper.updateByPrimaryKeySelective(spu);

        //发送商品删除消息
        this.sendMessage("delete",spuid);
    }

    /**
     * 下架商品
     * @param spuid
     */
    public void soldOutGood(Long spuid) {
        Spu spu = new Spu();
        spu.setId(spuid);
        spu.setSaleable(false);
        goodsMapper.updateByPrimaryKeySelective(spu);

        //发送商品下架消息
        this.sendMessage("delete",spuid);

    }

    /**
     * 上架商品
     * @param spuid
     */
    public void soldUpGood(Long spuid) {
        Spu spu = new Spu();
        spu.setId(spuid);
        spu.setSaleable(true);
        goodsMapper.updateByPrimaryKeySelective(spu);

        //发送商品上架消息
        this.sendMessage("insert",spuid);


    }

    /**
     * 根据spuId查询spu数据,同时查询skus与spudetial数据封装在spu实体中
     * @param id
     * @return
     */
    public Spu querySpuByPid(Long id) {
        //查询spu数据
        Spu spu = goodsMapper.selectByPrimaryKey(id);
        //查询skus数据，封装
        spu.setSkus(this.querySkusByPid(id));
        //查询spuDetial数据，封装在spu中
        spu.setSpuDetail(this.querySpuDetailByPid(id));
        if(spu == null){
            throw new LyException(HttpStatus.NOT_FOUND,"商品spu没有查找到");
        }
        return spu;
    }

    /**
     * 封装发送mq消息的方法
     * @param type
     * @param spuId
     */
    private void sendMessage(String type,Long spuId) {
        //此处消息的 生产者，只传递了routetype与需要的消息数据，没有指定exchange交换机，因此此处使用默认的交换机ly.item.exchange
        try {
            //同时考虑到业务逻辑，若是此处没有try异常，
            // 则如果出现了发送消息的异常则会影响到正常的数据插入业务，
            // 所以要有预感的将此处出现的一切异常多抓起来
            amqpTemplate.convertAndSend("item."+type,spuId);
        } catch (AmqpException e) {
            log.error("{}商品消息发送异常，商品id：{}",type,spuId,e.getMessage(),e);
        }
    }

    public List<Sku> querySkusByIds(List<Long> ids) {
        if(CollectionUtils.isEmpty(ids)){
            throw new LyException(HttpStatus.NOT_FOUND,"没有添加任何商品");
        }
        //查询sku商品集合
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(skus)){
            throw new LyException(HttpStatus.NOT_FOUND,"没有找到相应商品");
        }
        //查询相应商品的库存
        List<Stock> stocks = stockMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(stocks)){
            throw new LyException(HttpStatus.NOT_FOUND,"库存查询失败");
        }
        Map<Long,Integer> map = new HashMap<>();
        for (Stock stock : stocks) {
            map.put(stock.getSkuId(),stock.getStock());
        }
        for (Sku sku : skus) {
            sku.setStock(map.get(sku.getId()));
        }

        return skus;
    }

    /**
     * 此处会有线程安全问题  ，在skuMapper中
     * 这里减库存并没有采用先查询库存，判断充足才减库存的方案，
     * 那样会有线程安全问题，当然可以通过加锁解决。
     * 不过我们此处为了效率，并没有使用悲观锁，而是对库存采用了乐观锁方案
     *
     * 即在数据库减少库存时，如果库存少于要减少的数量，则修改失败，返回0
     */
    public void decreaseStock(List<CartDTO> cartDTOS) {

        for (CartDTO cartDTO : cartDTOS) {
            int i = skuMapper.decreaseStock(cartDTO.getSkuId(), cartDTO.getNum());
            if(i != 1){
                //如果返回值不为1，则修改失败，抛出异常
                throw new RuntimeException("库存不足");
            }
        }
    }

    public List<Spu> querySpusBySpuids(String ids) {
        String[] strings = StringUtils.split(ids, ",");
        List<String> stringIds = Arrays.asList(strings);
        List<Long> idss = stringIds.stream().map(s -> Long.valueOf(s)).collect(Collectors.toList());
        System.out.println("idss = " + idss.toString());
        List<Spu> spus = goodsMapper.selectByIdList(idss);
        if(CollectionUtils.isEmpty(spus)){
            throw new LyException(HttpStatus.NOT_FOUND,"查询spus失败");
        }
        return spus;
    }
}
