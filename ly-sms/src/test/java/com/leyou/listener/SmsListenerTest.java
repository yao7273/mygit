package com.leyou.listener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SmsListenerTest {

    @Autowired
    private SmsListener smsListener;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Test
    public void listenVerifyCode() throws InterruptedException {

        HashMap<String, String> map = new HashMap<>();
        map.put("phone","18222322532");
        map.put("code","66666");

        amqpTemplate.convertAndSend("ly.sms.exchange","sms.verify.code",map);

        Thread.sleep(5000);
    }
}