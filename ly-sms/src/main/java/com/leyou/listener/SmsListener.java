package com.leyou.listener;

import com.leyou.properties.SmsProperties;
import com.leyou.utils.SmsUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@EnableConfigurationProperties(SmsProperties.class)
public class SmsListener {

    @Autowired
    private SmsProperties prop;

    @Autowired
    private SmsUtil smsUtil;

    /**
     * 注册时手机手机验证码的发送
     * @param msg
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "ly.sms.verify.queue"),
            exchange = @Exchange(name = "ly.sms.exchange",type = ExchangeTypes.TOPIC),
            key = "sms.verify.code"))
    public void listenVerifyCode(Map<String,String> msg){
        if(msg==null){
            return;
        }
        String phone = msg.get("phone");
        if(StringUtils.isBlank(phone)){
            return;
        }
        //移除msg中的手机号参数，只留下短信需要的数据
        msg.remove("phone");
        smsUtil.sendSms(prop.getSignName(),
                prop.getVerifyCodeTemplate(),phone,msg);

    }

}
