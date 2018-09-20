package com.leyou.user.controller;

import com.leyou.user.pojo.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserControllerTest {

    @Autowired
    private UserController userController;


    //测试,校验用户名与手机号是否存在
    @Test
    public void checkUserData() {
        ResponseEntity<Boolean> data = userController.checkUserData("zhangsan", 2);
        Boolean body = data.getBody();
        System.out.println("body = " + body);
    }

    //测试，校验手机号发送验证码
    @Test
    public void sendCode(){
        userController.sendCode("18222322532");
    }

    @Test
    public void queryByUsernameAndPassword() {

        ResponseEntity<User> zhangsi = userController.queryByUsernameAndPassword("zhangwu", "zhangwu");
        User user = zhangsi.getBody();
        System.out.println("user = " + user);


    }
}