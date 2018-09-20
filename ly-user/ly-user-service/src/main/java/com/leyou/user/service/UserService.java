package com.leyou.user.service;

import com.leyou.common.exception.LyException;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapeper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapeper userMapeper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    private static String KEY_PREFIX = "user:verify:phone";

    public Boolean checkUserData(String data, Integer type) {

        User user = new User();
        switch (type) {
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
            default:
                throw new LyException(HttpStatus.BAD_REQUEST, "请求参数有误");
        }
        int count = userMapeper.selectCount(user);
        //不存在返回true可用，false该用户不可用
        return count==0;
    }

    /**
     * 根据用户输入的手机号，生成随机验证码，
     * 长度为6位，纯数字。并且调用短信服务，发送验证码到用户手机。
     * @param phone
     */
    public void sendCode(String phone) {

        //首先标记手机号码为注册手机号，以免与登陆，订单等混淆
        String key = KEY_PREFIX + phone;
        //判断存在redis中的手机号码是否已存在，（因为设置手机号存在redis中）分钟，防止频繁输入手机号
        if(redisTemplate.hasKey(key)){
            throw new LyException(HttpStatus.BAD_REQUEST,"发送短信过于频繁");
        }
        //验证手机号码的格式
        if(!phone.matches("^1[3456789]\\d{9}$")){
            throw new LyException(HttpStatus.BAD_REQUEST,"手机号码格式不正确");
        }

        //生成6位数验证码
        String code = NumberUtils.generateCode(6);
        //将手机号与验证码暂时存储到redis，一份钟，用于之后的判断
        redisTemplate.opsForValue().set(key,code,5, TimeUnit.MINUTES);

        //封装手机号与code
        Map<String,String> msg = new HashMap<>();
        msg.put("phone",phone);
        msg.put("code",code);
        //发送消息，调用发短信服务
        amqpTemplate.convertAndSend("ly.sms.exchange","sms.verify.code",msg);
    }

    /**
     * 注册用户
     * @param user
     * @param code
     */
    public void register(User user, String code) {

        //判断验证码是否正确
            //获取redis中的手机号与验证码数据
        String key = KEY_PREFIX + user.getPhone();
        String codeCache = redisTemplate.opsForValue().get(key);
        if(!code.equals(codeCache)){
            throw new LyException(HttpStatus.BAD_REQUEST,"验证码错误");
        }

        //防止地址栏中直接进行参数传递，再次判断用户名是否已存在
        if(!checkUserData(user.getUsername(), 1)){
            throw new LyException(HttpStatus.BAD_REQUEST,"请求参数有误，用户名已存在");
        }
        //设置usr对象中的初始化时间
        user.setCreated(new Date());
        user.setId(null);

        //密码的加密处理，先加盐
        String salt = CodecUtils.generateSalt();
        String md5Password = CodecUtils.md5Hex(user.getPassword(), salt);
        user.setSalt(salt);
        user.setPassword(md5Password);

        //存入数据库
        int i = userMapeper.insertSelective(user);
        if(i !=1){
            throw new LyException(HttpStatus.BAD_REQUEST,"请求参数有误");
        }
        //删除redis中的验证码
        redisTemplate.delete(key);


    }

    public User queryByUsernameAndPassword(String username, String password) {
        //先根据用户名查询出该用户的salt盐值
        User user = new User();
        user.setUsername(username);
        User userCache = userMapeper.selectOne(user);
        if(userCache == null){
            throw new LyException(HttpStatus.BAD_REQUEST,"用户名不存在");
        }
        //先对密码与盐值进行MD5加密，然后与后密码进行比较
        //正确则返回数据
        if(!userCache.getPassword().equals(CodecUtils.md5Hex(password, userCache.getSalt()))){
            throw new LyException(HttpStatus.BAD_REQUEST,"密码错误");
        }

        return userCache;
    }
}
