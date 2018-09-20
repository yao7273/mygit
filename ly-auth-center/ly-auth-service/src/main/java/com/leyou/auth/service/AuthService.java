package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties prop;

    public String login(String username, String password) {
        try {
            //查询用户信息
            User user = userClient.queryByUsernameAndPassword(username, password);
            if(user == null){
                return null;
            }
            //转换成userinfo实体，用户生成token
            UserInfo userInfo = new UserInfo(user.getId(), username);
            //生成token
            String token = JwtUtils.generateToken(userInfo,prop.getPrivateKey(),prop.getExpire());

            return token;
        } catch (Exception e) {
            return null;
        }
    }

}
