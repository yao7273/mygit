package com.leyou.order.controller;

import com.leyou.order.dto.OrderDTO;
import com.leyou.order.pojo.Order;
import com.leyou.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;


    /**
     * 创建订单，接收订单信息，其中主要接收订单商品的或是收获地址的id，
     *          具体数据需要到数据库自己查询，防止恶意篡改，
     *         返回值为订单的编号，id
     * @param orderDTO
     * @return
     */
    @PostMapping("order")
    public ResponseEntity<Long> addOrder(@RequestBody OrderDTO orderDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.addOrder(orderDTO));
    }


    /**
     * 根据订单id，获取订单对象。用于渲染订单支付页
     * @param id
     * @return
     */
    @GetMapping("order/{id}")
    public ResponseEntity<Order> queryOrderById(@PathVariable("id")Long id){

        return ResponseEntity.ok(orderService.queryOrderById(id));

    }

    /**
     * 根据订单id获取二维码链接
     * @param id
     * @return
     */
    @GetMapping("order/url/{id}")
    public ResponseEntity<String> generateUrl(@PathVariable("id")Long id){

        return ResponseEntity.ok(orderService.generateUrl(id));
    }

    @GetMapping("order/state/{id}")
    public ResponseEntity<Integer> queryOrderStatusById(@PathVariable("id")Long id){
        return ResponseEntity.ok(orderService.queryOrderStatusById(id));
    }

}
