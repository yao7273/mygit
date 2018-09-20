package com.leyou.search.listener;

import com.leyou.search.service.SearchService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoodsListener {

    @Autowired
    private SearchService searchService;

    /**
     * 添加注解的方式监听消息发送端，x新增数据或是修改数据
     * @param id
     */
    @RabbitListener(bindings = @QueueBinding(
            //队列的设置，durable是否持久化
            value = @Queue(name = "ly.search.insert.queue",durable = "true"),
            //交换机的配置，topic类型
            exchange = @Exchange(name = "ly.item.exchange",type = ExchangeTypes.TOPIC,ignoreDeclarationExceptions = "true"),
            key = {"item.insert","item.update"}))
    public void listenInsert(Long id){
        if(id !=null){
            searchService.insertOrUpdate(id);

        }
    }

    /**
     * 接收队列中的消息，删除索引库中的数据
     * @param id
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "ly.search.delete.queue",durable = "true"),
            exchange = @Exchange(name = "ly.item.exchange",type = ExchangeTypes.TOPIC, ignoreDeclarationExceptions = "true"),
            key = "item.delete"))
    public void listenDelete(Long id){

        if(id != null){
            searchService.delete(id);
        }

    }


}
