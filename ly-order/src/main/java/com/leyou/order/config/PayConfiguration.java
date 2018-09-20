package com.leyou.order.config;

import com.leyou.order.utils.PayHelper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayConfiguration {

    //通过set的方式往WXPayConfigImpl类中注入配置文件application中的属性数据
    @Bean
    @ConfigurationProperties(prefix = "ly.pay")
    public WXPayConfigImpl wxPayConfig(){
        return new WXPayConfigImpl();
    }

    //向spring容器注入payHelper类，初始化对象
    @Bean
    public PayHelper payHelper(WXPayConfigImpl wxPayConfig){
        return new PayHelper(wxPayConfig);
    }

}
