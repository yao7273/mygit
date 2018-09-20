package com.leyou.auth.controller;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.CookieUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {

    @Autowired
    private JwtProperties prop; //加载配置类

    @Autowired
    private AuthService authService;

    /**
     * 查询用户信息是否存在，返回token值存入cookie
     * @param username
     * @param password
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestParam("username")String username,
            @RequestParam("password")String password,
            HttpServletRequest request, HttpServletResponse response){
        String token = authService.login(username,password);
        if(StringUtils.isBlank(token)){
            throw new LyException(HttpStatus.BAD_REQUEST,"账号或密码错误");
        }
        //将token写入cookie··········
        CookieUtils.newBuilder(response)
                .httpOnly().request(request).build(prop.getCookieName(),token);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取用户的cookie中token验证用户信息
     * @param token
     * @return
     */
    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(
            @CookieValue("LY_TOKEN")String token,
            HttpServletResponse response,HttpServletRequest request){

        //对token进行解析
        try {
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, prop.getPublicKey());

            //因为token的过期时间设置为30分钟，到期则用户会失效，
            // 所以当用户再操作时对用户的token进行刷新，即重新创建，然后赋予用户cookie
            String newToken = JwtUtils.generateToken(userInfo, prop.getPrivateKey(), prop.getExpire());
            //再次将token写入cookie，并指定httpOnly，防止通过js获取与修改
            CookieUtils.newBuilder(response).request(request).httpOnly()
                    .build(prop.getCookieName(),newToken);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }


}
