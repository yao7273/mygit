package com.leyou.user.api;

import com.leyou.user.pojo.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


public interface UserApi {
    /**
     * 查询功能，根据参数中的用户名和密码查询指定用户
     * @param username
     * @param password
     * @return
     */
    @GetMapping("/query")
    User queryByUsernameAndPassword(
            @RequestParam("username")String username,
            @RequestParam("password")String password);

    }
