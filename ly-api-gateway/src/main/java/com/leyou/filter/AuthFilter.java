package com.leyou.filter;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.config.FilterProperties;
import com.leyou.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 网关的对用户身份验证的过滤器，即再网关统一对（需要登陆状态的服务）用户的token进行校验，不需要再每个微服务都进行校验
 */
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
@Component
public class AuthFilter extends ZuulFilter {

    @Autowired
    private JwtProperties jwtProp;

    //白名单过滤配置类对象
    @Autowired
    private FilterProperties filterProp;


    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER - 1;
    }

    //对请求路径进行过滤，对符合白名单内的路径放行，即不拦截
    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        //返回除去host（域名或者ip）部分的路径
        String uri = request.getRequestURI();

        return !isAllowPath(uri);
    }
    //对请求路径的判断
    private boolean isAllowPath(String uri) {
        //获取所有允许放行的路径
        List<String> allowPaths = filterProp.getAllowPaths();
        for (String s : allowPaths ) {
            if(uri.startsWith(s)){
                return true;
            }
        }
        return false;
    }

    @Override
    public Object run()  {

        //获取上下文
        RequestContext requestContext = RequestContext.getCurrentContext();
        //获取request对象
        HttpServletRequest request = requestContext.getRequest();
        //获取cookie中token值
        String token = CookieUtils.getCookieValue(request, jwtProp.getCookieName());
        try {
            //通过jet对token校验,校验通过就放行
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, jwtProp.getPublicKey());

            //TODO 获取到用户数据后就可以做用户级别的鉴定，权限的鉴定等操作

        } catch (Exception e) {
            //校验失败，拦截请求
            requestContext.setSendZuulResponse(false);
            requestContext.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
        }
        return null;
    }
}
