package com.leyou.order.controller;

import com.leyou.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class PayNotifyController {


    @Autowired
    private OrderService orderService;

    /**
     *接收微信传递用户支付信息参数，并返回信息接收成功
     *
     * 其中参数与返回值都是xml格式，引入了解析xml格式的依赖，jackson-***-xml，
     * 可以将xml格式解析成map格式
     *
     * @param map
     * @return
     */
    @PostMapping("wxpay/notify")
    public ResponseEntity<String> payNotify(@RequestBody Map<String,String> map ){

        //处理回调的结果
        orderService.payNotify(map);

        // 没有异常，则返回成功
        String result = "<xml>\n" +
                "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                "</xml>";
        return ResponseEntity.ok(result);
    }

}
