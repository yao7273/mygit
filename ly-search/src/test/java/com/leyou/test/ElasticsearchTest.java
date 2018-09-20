package com.leyou.test;

import com.leyou.LySearchApplication;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Spu;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.repository.GoodsRepository;
import com.leyou.search.service.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LySearchApplication.class)
public class ElasticsearchTest {

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private SearchService searchService;

    @Test
    public void createIndex(){
        //创建索引与映射
        template.createIndex(Goods.class);
        template.putMapping(Goods.class);
    }

    //查询数据库中的数据，封装为goods对象
    @Test
    public void buildGoods(){

        //循环查询索引库中的数据
        int page = 1;
        int rows = 100;
        int size = 0;
        do {
            //查询数据库中的spu数据，转换为goods数据，在存在索引库中

            PageResult<Spu> result = goodsClient.querySpuByPage(null, true, page, rows);
            List<Spu> spus = result.getItems();
            //非空判断
            if (CollectionUtils.isEmpty(spus)) {
                break;
            }

            List<Goods> goodsList = spus.stream().map(spu ->
                    searchService.buildGoods(spu)).collect(Collectors.toList());

            goodsRepository.saveAll(goodsList);
            page++;
            size = spus.size(); //查询到的每页的条数
        }while (size == rows);  //如果当前页数据小于rows，则为最后一夜，跳出循环
    }




}
