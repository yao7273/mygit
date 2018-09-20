package com.leyou.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ly.sms")
@Data
public class SmsProperties {

    //使用配置类调用属性
    String accessKeyId;
    String accessKeySecret;
    String signName;
    String verifyCodeTemplate;


}
