package com.leyou.page.linstener;

import com.leyou.page.service.PageService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PageLinsten {

    @Autowired
    private PageService pageService;

    /**
     * 接收商品微服务得增改消息，增改静态页面
     * @param id
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "ly.page.insertOrUpdate.queue",durable = "true"),
            exchange = @Exchange(name = "ly.item.exchange",type = ExchangeTypes.TOPIC,
            ignoreDeclarationExceptions = "true"),
            key = {"item.insert","item.update"}))
    public void listenInsertOrUpdate(Long id){
        //静态页面得修改本质就是先删除再新增
        pageService.createHtml(id);
    }

    /**
     * 接收商品微服务得增改消息，删除静态页面
     * @param id
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "ly.page.delete.queue",durable = "true"),
            exchange = @Exchange(name = "ly.item.exchange",type = ExchangeTypes.TOPIC,
            ignoreDeclarationExceptions = "true"),
            key = "item.delete"))
    public void linstenDelete(Long id){
        pageService.deleteHtml(id);
    }



}
