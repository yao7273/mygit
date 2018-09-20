package com.leyou.order.filter;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.order.config.JwtProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 拦截请求判断用户状态是否登陆
 */


public class LoginInterceptor implements HandlerInterceptor {


    private JwtProperties prop;

    public LoginInterceptor(JwtProperties prop){
        this.prop = prop;
    }


    //创建线程域，用于存放同一个线程中的u用户信息，线程内（从controller到mapper）数据，userInfo
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 重写springmvc拦截器的前置拦截器，拦截请求判断用户状态是否登陆
     * @param request
     * @param response
     * @param handler
     * @return
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //获取cookie中的token值
        String token = CookieUtils.getCookieValue(request, prop.getCookieName());
        if(StringUtils.isBlank(token)){
            //再拦截器中异常直接通过response对向传递
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        try {
            //获取token
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            //直接将user对象放入线程域中，本质为k-v结构，默认key为当前线程
            THREAD_LOCAL.set(userInfo);
            return true;
        } catch (Exception e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
    }

    //线程完成后的方法，最后需要将线程域中的用户信息清除
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        //删除当前线程中的对象
        THREAD_LOCAL.remove();
    }

    //对外提供静态方法，用于获取线程域中的当前线程的用户信息数据
    public static UserInfo getUserInfoFromThread(){
        return THREAD_LOCAL.get();
    }
}
