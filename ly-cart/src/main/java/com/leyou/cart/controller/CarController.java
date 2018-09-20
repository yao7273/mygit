package com.leyou.cart.controller;

import com.leyou.cart.pojo.Cart;
import com.leyou.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CarController {

    @Autowired
    private CartService cartService;

    /**
     * 登陆状态添加商品到购物车，购物车存到redis
     * @param cart
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> addCart(@RequestBody Cart cart){
        cartService.addCart(cart);

        System.out.println("cart = " + cart);
        return ResponseEntity.ok().build();
    }

    /**
     * 接收用户loaclStorage中的购物车数据，添加到redis中
     * @param carts
     * @return
     */
    @PostMapping("localCart")
    public ResponseEntity<Void> addLocalCart(@RequestBody List<Cart> carts){

        //System.out.println("carts = " + JsonUtils.toString(carts));

        cartService.addLocalCart(carts);

        return ResponseEntity.ok().build();

    }

    /**
     * 加载购物车数据，同时查询localStorage中的购物车数据，将其加入到redis中，
     */
    @GetMapping("list")
    public ResponseEntity<List<Cart>> queryCartsFromRedis(){

        return ResponseEntity.ok(cartService.queryCartsFromRedis());

    }

    /**
     * 修改redis中购物车商品的数量
     * @param id  商品的id
     * @param num  最终的商品数量
     * @return
     */
    @PutMapping
    public ResponseEntity<Void> changeSkuNum(
            @RequestParam("id")Long id,@RequestParam("num")Integer num){
        cartService.changeSkuNum(id,num);
        return ResponseEntity.ok().build();
    }


    /**
     * 根据商品id删除购物车中的商品
     * @param id
     * @return
     */
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteSku(@PathVariable("id")Long id){
        cartService.deleteSku(id);
        return ResponseEntity.ok().build();
    }

}
