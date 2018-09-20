package com.leyou.page.service;

import com.leyou.item.pojo.Brand;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.Spu;
import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class PageService {

    //创建线程池。
    private static final ExecutorService es = Executors.newFixedThreadPool(20);


    //使用feign客户端调用远程服务
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;

    //自定义的静态资源存放地址
    @Value("${ly.page.destPath}")
    public String destPath;

    //模板引擎 templateEngine.process("模板名", context, writer);
    //可以将静态资源通过流输出到本地
    @Autowired
    private TemplateEngine templateEngine;


    /**
     * 根据spuid查询页面需要的数据，封装返回
     * @param id
     * @return
     */
    public Map<String, Object> toGoodsPage(Long id) {

        Map<String,Object> map = new HashMap<>();
        try {
            //页面需要的数据
            //1.spu
            Spu spu = goodsClient.querySpuByPid(id);
                //简化前台spu的数据
            Map<String,String> map1 = new HashMap<>();
            map1.put("title",spu.getTitle());
            map1.put("subTitle",spu.getSubTitle());
            map.put("spu",spu);
            //2.skus
            map.put("skus",spu.getSkus());
            //3.detial
            map.put("detail",spu.getSpuDetail());
            //4.三级分类的数据
            List<Category> categories = categoryClient.queryByCids(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            map.put("categories",categories);
            //5.品牌数据
            Brand brand = brandClient.queryBrandById(spu.getBrandId());
            map.put("brand",brand);
            //6.规格参数数据
            List<SpecGroup> specGroups = specificationClient.querySpecsByCid(spu.getCid3());
            map.put("specs",specGroups);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("加载数据失败",e);
        }
    }

    //通过新线程执行静态页面的创建
    public void asyncCreateHtml(Long spuId){
        es.execute(() ->createHtml(spuId));
    }
    public void asyncCreateHtml(Long spuId,Map<String,Object> data){

        es.execute(() -> createHtml(spuId,data));
    }



    //方法的重载，
    public void createHtml(Long spuId)  {
        createHtml(spuId,toGoodsPage(spuId));
    }

        //创建静态文件，写入到本地
    //模板引擎 templateEngine.process("模板名", context, writer);
    public void createHtml(Long spuId,Map<String,Object> data) {

        //先创建上下文，将后台查询到的数据添加进去
        Context context = new Context();
        context.setVariables(data);

        //获取目标文件的路径(存储的绝对路径)，用于流的输出目的
        File file = getDestFile(spuId);

        //判断文件是否已经存在，存在则删除，重新创建
        if(file.exists()) file.delete();

        //准备流
       try( PrintWriter writer = new PrintWriter(file)){
           //使用模板引擎输出静态页面
           templateEngine.process("item",context,writer);
       } catch (Exception e) {
           /**
            * 在spring整合AMQP消息队列时，期间出现的异常都要转换成！！运行期异常！！抛出去，
            只有这样才能触发spring自己配置的ack消息确认机制中的消息的回滚，
            要是自己try捕捉了异常，则spring不知道有异常，正常调用ack，消费消息，就会出现消息的丢失
            */
           log.error("静态页面生成异常 spuId:{}",spuId,e.getMessage(),e);
           throw new RuntimeException("静态页面生成异常",e);
       }
    }

    private File getDestFile(Long spuId) {
        //获取目标文件的路径(存储的绝对路径)，用于流的输出目的
        File dir = new File(destPath);
        //如果文件夹不存在，则创建
        if(!dir.exists()) dir.mkdirs();
        //创建文件的地址
        return new File(dir, spuId + ".html");
    }

    /**
     * 删除静态页面
     * @param id
     */
    public void deleteHtml(Long id) {
        File file = new File(this.destPath, id + ".html");
        if(file.exists()){
            file.delete();
        }

    }

}
