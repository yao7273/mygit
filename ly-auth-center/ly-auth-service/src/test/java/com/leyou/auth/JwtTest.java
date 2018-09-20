package com.leyou.auth;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.auth.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.security.PrivateKey;
import java.security.PublicKey;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JwtTest {

    //私钥与公钥的存放地址
    private static String priKeyPath ="D:\\leyou\\rsa\\rsa.pri";
    private static String pubKeyPath ="D:\\leyou\\rsa\\rsa.pub";

    private PublicKey publicKey;
    private PrivateKey privateKey;

    //测试根据密文获取公钥与私钥,并放入到指定地址的文件中
    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath,priKeyPath,"hello");
    }
    //获取指定地址中的私钥与密钥
    @Before
    public void testGetRsa() throws Exception {
        publicKey = RsaUtils.getPublicKey(pubKeyPath);
        privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    //使用私钥对用户信息进行加密,得到token
    @Test
    public void testGenerateToken() throws Exception {
        publicKey = RsaUtils.getPublicKey(pubKeyPath);
        privateKey = RsaUtils.getPrivateKey(priKeyPath);
        String token = JwtUtils.generateToken(new UserInfo(2l, "zhangsan"), privateKey, 5);
        System.out.println("token = " + token);

    }

    //使用公钥对token进行解密
    @Test
    public void testGetInfoFromToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MiwidXNlcm5hbWUiOiJ6aGFuZ3NhbiIsImV4cCI6MTUzNTg3MDgyMX0.UVch0WlRCrQ6VhBH36DvOIfWiJCbV_r5r7vExiao-_jJ5R6e-HS2eAinyDLJnSrPgq4p5WyrBdfzZSSd61ZcEsvLDrwBWDh_3pycJ-gM6uL1ksWdDxSVHsGcPCXyvpJaTooDafc1j8oPOeCSFNHggkP9hHca3g9xGsBM4BHFd_c";
        UserInfo userInfo = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("userInfo = " + userInfo);

    }

}
