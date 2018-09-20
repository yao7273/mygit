package com.leyou.user.controller;

import com.leyou.common.exception.LyException;
import com.leyou.user.pojo.User;
import com.leyou.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 实现用户数据的校验，主要包括对：手机号、用户名的唯一性校验。
     *
     * @param data
     * @param type ：1，用户名；2，手机
     * @return
     */
    @GetMapping("/check/{data}/{type}")
    public ResponseEntity<Boolean> checkUserData(
            @PathVariable("data") String data,
            @PathVariable(value = "type", required = false) Integer type) {
        if (type == null) type = 1;
        Boolean aBoolean = userService.checkUserData(data, type);
        return ResponseEntity.ok(aBoolean);
    }


    /**
     * 根据用户输入的手机号，生成随机验证码，
     * 长度为6位，纯数字。并且调用短信服务，发送验证码到用户手机。
     *
     * @param phone
     * @return
     */
    @PostMapping("/code")
    public ResponseEntity<Void> sendCode(@RequestParam("phone") String phone) {

        userService.sendCode(phone);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 实现用户注册功能，需要对用户密码进行加密存储，使用MD5加密，
     * 加密过程中使用随机码作为salt加盐。另外还需要对用户输入的短信验证码进行校验。
     *
     * @param user
     * @param code
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid  User user, @RequestParam("code") String code) {
        //在使用实体类接收表单中的参数信息时，在User前加上@Valid注解,即表示
        // 使用hibernate-validator，验证器，在实体类上通过注解的方式验证传输的数据是否符合条件
        //无需再方法中再判断
        /*//对用户传输的数据健壮性判断
        if (user == null || user.getUsername().length() < 4 || user.getPassword().length() < 4 || StringUtils.isBlank(code)) {
            throw new LyException(HttpStatus.BAD_REQUEST, "请求参数错误");
        }*/
        userService.register(user, code);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 查询功能，根据参数中的用户名和密码查询指定用户
     * @param username
     * @param password
     * @return
     */
    @GetMapping("/query")
    public ResponseEntity<User> queryByUsernameAndPassword(
            @RequestParam("username")String username,
            @RequestParam("password")String password){
        if(StringUtils.isBlank(username) ||StringUtils.isBlank(password)){
            throw new LyException(HttpStatus.BAD_REQUEST, "请求参数错误");
        }
        User user =  userService.queryByUsernameAndPassword(username,password);
        return ResponseEntity.ok(user);


    }



}
