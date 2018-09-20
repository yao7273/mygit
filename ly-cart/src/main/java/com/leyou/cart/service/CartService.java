package com.leyou.cart.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.filter.LoginInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GoodsClient goodsClient;

    //设置商品存入redis时的key的前缀，用于区别其他服务中存入redis的用户id
    private static String PRE_KEY = "ly:cart:uid";

    public void addCart(Cart cart) {
        if(cart == null){
            throw new LyException(HttpStatus.BAD_REQUEST,"添加购物车失败");
        }
        Integer num = cart.getNum();
        //获取前台的cart中商品id
        String skuId = cart.getSkuId().toString();

        //根据用户信息查询redis中购物车数据，判断要插入的商品是否已经存在
        UserInfo userInfo = LoginInterceptor.getUserInfoFromThread();
        String userInfoId = userInfo.getId().toString();

        //设置存入redis中购物车模块的数据的key
        String key = PRE_KEY + userInfoId;
        //获取要存入的购物车的商品的数量

        //获取指定key的hash结构的value部分
        //在redis中的hash结构，判断
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        //查询redis。
        if(ops.hasKey(skuId)){
            //如果已经存在，修改redis中该订单的数量
            cart = JsonUtils.toBean(ops.get(skuId).toString(),Cart.class);
            cart.setNum(cart.getNum()+num);
        }
        //如果redis中没有存入该商品则直接加入||把修改后的cart加入到redis，相同key会覆盖
        ops.put(cart.getSkuId().toString(),JsonUtils.toString(cart));

    }

    public void addLocalCart(List<Cart> carts) {
        //如果本地数位空，直接返回
        if(CollectionUtils.isEmpty(carts)){
            return;
        }
         //查询当前用户
        UserInfo userInfo = LoginInterceptor.getUserInfoFromThread();
        //用户存入redis的key
        String uid = userInfo.getId().toString();  //todo 此处报空指针异常
        String key = PRE_KEY + uid;

        System.out.println("key = " + key);

        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        //遍历redis中的购物车，判断本地购物车中的商品是否存在其中
        for (Cart cart : carts) {
            String ckey = cart.getSkuId().toString();
            Integer num = cart.getNum();
            //如果redis中已经存在该商品，则增加商品数量
            if (ops.hasKey(ckey)){
                cart = JsonUtils.toBean(ops.get(ckey).toString(), Cart.class);
                cart.setNum(cart.getNum()+num);
            }
            //如果redis中没有存入该商品则直接加入||把修改后的cart加入到redis，相同key会覆盖
            ops.put(cart.getSkuId().toString(),JsonUtils.toString(cart));
        }


    }

    public List<Cart> queryCartsFromRedis() {
        //获取线程域中的用户对象
        UserInfo userInfo = LoginInterceptor.getUserInfoFromThread();
        String key = PRE_KEY + userInfo.getId().toString();

        //获取hash结构redis中对象
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        //判断redis中的购物车是否为空
        if (CollectionUtils.isEmpty(ops.keys())) {
            //如果为空，抛异常，前台显示购物车为空
            throw new LyException(HttpStatus.NOT_FOUND,"购物车为空");
        }
        //获取redis中购物车中的商品集合
        List<Cart> carts = ops.values().stream()
                .map(c -> JsonUtils.toBean(c.toString(), Cart.class)).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(carts)){
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR,"解析购物车数据异常");
        }
        //查询购物车中商品是否已下架
        //根据skuids查询sku
        List<Sku> skus = goodsClient.querySkusByIds(carts.stream().map(cart -> cart.getSkuId()).collect(Collectors.toList()));
        //根据spuids查询spus
        List<Long> spuids = skus.stream().map(sku -> sku.getSpuId()).collect(Collectors.toList());
        System.out.println("spuids = " + spuids.toString());
        String join = StringUtils.join(spuids, ",");
        List<Spu> spus = goodsClient.querySpusBySpuids(join);
        Map<Long,Spu> spuMap = new HashMap<>();
        for (Spu spu : spus) {
            spuMap.put(spu.getId(),spu);
        }
        Map<Long,Spu> skuSpuMap = new HashMap<>();
        for (Sku sku : skus) {
            skuSpuMap.put(sku.getId(),spuMap.get(sku.getSpuId()));
        }
        //设置购物车中是否上架
        for (Cart cart : carts) {
            cart.setSaleable(skuSpuMap.get(cart.getSkuId()).getSaleable());
        }
        return carts;
    }

    public void changeSkuNum(Long id, Integer num) {

        UserInfo userInfo = LoginInterceptor.getUserInfoFromThread();
        String key = PRE_KEY + userInfo.getId().toString();

        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        //查询redis购物车是否用于该商品数据
        if(!ops.hasKey(id.toString())){
            //如果没有，返回异常
            throw new LyException(HttpStatus.NOT_FOUND,"没有找到需要修改的商品");
        }
        //有则修改商品数量
        Cart cart = JsonUtils.toBean(ops.get(id.toString()).toString(), Cart.class);
        cart.setNum(num);
        //在插入redis
        ops.put(id.toString(),JsonUtils.toString(cart));
    }

    public void deleteSku(Long id) {
        UserInfo userInfo = LoginInterceptor.getUserInfoFromThread();
        String key = PRE_KEY + userInfo.getId().toString();

        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        if(!ops.hasKey(id.toString())){
            throw new LyException(HttpStatus.NOT_FOUND,"该商品已从购物车删除");
        }
        //删除该商品
        Long i = ops.delete(id.toString());
        if(i != 1){
            //返回值不等于1，删除失败
            throw new LyException(HttpStatus.EXPECTATION_FAILED,"删除购物车中商品失败");
        }
    }
}
