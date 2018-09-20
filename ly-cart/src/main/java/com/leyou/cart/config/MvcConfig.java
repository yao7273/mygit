package com.leyou.cart.config;

import com.leyou.cart.filter.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 自定义springmvc的配置类，在其中添加自定义的拦截器，使其生效
 *
 * 注意：不能在配置类中使用spring注入拦截器类，
 *  因为springboot中配置类会优先于其他在注入在spring中bean
 *  所以在要通过new注入
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class MvcConfig implements WebMvcConfigurer {



    @Autowired
    private JwtProperties prop;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //拦截路径为/**，拦截一切路径
        registry.addInterceptor(new LoginInterceptor(prop)).addPathPatterns("/**");
    }
}
