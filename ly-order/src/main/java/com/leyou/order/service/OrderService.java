package com.leyou.order.service;

import com.github.wxpay.sdk.WXPayConstants;
import com.leyou.auth.entity.UserInfo;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.dto.CartDTO;
import com.leyou.item.pojo.Sku;
import com.leyou.order.client.AddressClient;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.dto.AddressDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enuma.OrderStatusEnum;
import com.leyou.order.enuma.PayState;
import com.leyou.order.enuma.PayStatusEnum;
import com.leyou.order.filter.LoginInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.mapper.PayLogMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.pojo.PayLog;
import com.leyou.order.utils.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private OrderStatusMapper orderStatusMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PayHelper payHelper;

    @Autowired
    private PayLogMapper payLogMapper;

    //设置商品存入redis时的key的前缀，用于区别其他服务中存入redis的用户id
    private static String PRE_KEY = "ly:cart:uid";

    /**
     * 创建订单，添加到数据库
     * <p>
     * 此处由于有多次对数据库的增删，需要添加事务，
     * 其中减库存使用了乐观锁的方式，并将其放在方法最后，
     * 一旦其出现异常，则整个方法全部回滚
     *
     * @param orderDTO
     * @return
     */
    @Transactional
    public Long addOrder(OrderDTO orderDTO) {
        //获取用户登陆信息
        UserInfo userInfo = LoginInterceptor.getUserInfoFromThread();

        //1.组织order数据
        Order order = new Order();
        //1.1用户登陆id。userId
        Long userId = userInfo.getId();
        //1.2创建订单编号
        long orderId = idWorker.nextId();
        //1.3跨服务查询商品信息
        //1.3.1获取订单中的商品id集合
        List<Long> skuids = orderDTO.getCarts().stream().map(c -> c.getSkuId()).collect(Collectors.toList());
        List<Sku> skus = goodsClient.querySkusByIds(skuids);
        if (CollectionUtils.isEmpty(skus)) {
            throw new RuntimeException("订单不存在");
        }
        //组织成map类型
        Map<Long, Sku> skuMap = skus.stream().collect(Collectors.toMap(s -> s.getId(), s -> s));
        //1.3.2获取商品的总金额,等于商品价格成商品数量
        Long totalPay = 0l;
        //查询orderDetail的数据,并封装
        List<OrderDetail> detailList = new ArrayList<>();
        for (CartDTO cartDTO : orderDTO.getCarts()) {
            Sku sku = skuMap.get(cartDTO.getSkuId());
            totalPay = totalPay + sku.getPrice() * cartDTO.getNum();
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setId(null);
            orderDetail.setOrderId(orderId);
            orderDetail.setSkuId(sku.getId());
            orderDetail.setNum(cartDTO.getNum());
            orderDetail.setTitle(sku.getTitle());
            orderDetail.setPrice(sku.getPrice());
            orderDetail.setOwnSpec(sku.getOwnSpec());
            orderDetail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            //封装orderDetail集合
            detailList.add(orderDetail);

        }
        //收货人信息
        AddressDTO addressDTO = AddressClient.findById(orderDTO.getAddressId());
        if (addressDTO == null) {
            throw new RuntimeException("收货人信息不存在");
        }
        //1.4封装数据
        order.setOrderId(orderId);
        order.setTotalPay(totalPay);//总金额
        order.setActualPay(totalPay);//实付金额 = 总金额 + 邮费 - 优惠金额，暂时使用总金额的价钱
        order.setPaymentType(orderDTO.getPaymentType());
        order.setCreateTime(new Date());
        //1.4.2.封装物流数据,暂时不封装
        //1.4.3.封装用户数据,其中买家评价等暂时不封装
        order.setUserId(userId);
        //1.4.4.收货人信息
        order.setReceiver(addressDTO.getName());
        order.setReceiverMobile(addressDTO.getPhone());
        order.setReceiverState(addressDTO.getState());
        order.setReceiverCity(addressDTO.getCity());
        order.setReceiverDistrict(addressDTO.getDistrict());
        order.setReceiverAddress(addressDTO.getAddress());
        order.setReceiverZip(addressDTO.getZipCode());
        order.setBuyerNick(userInfo.getUsername());
        //1.5.将order存入数据库
        orderMapper.insertSelective(order);

        //2.组织orderDetial数据

        //2.1 将orderDetail批量存入数据库
        orderDetailMapper.insertList(detailList);

        //3.组织orderStatus数据,先暂存部分数据
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(orderId);
        orderStatus.setStatus(OrderStatusEnum.INIT.value());
        orderStatus.setCreateTime(new Date());
        //3.1 存入数据库
        orderStatusMapper.insert(orderStatus);

        //4.删除购物车中已生成订单的商品
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(PRE_KEY + userId.toString());
        for (CartDTO cartDTO : orderDTO.getCarts()) {
            Long delete = ops.delete(cartDTO.getSkuId().toString());
        }

        //5.减少库存中相应商品中的数量
        goodsClient.decreaseStock(orderDTO.getCarts());

        //6.成功日志
        log.info("生成订单，订单编号为：{}，用户id：{}", orderId, userId);
        return orderId;
    }

    public String generateUrl(Long id) {

        Order order = orderMapper.selectByPrimaryKey(id);
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(id);
        OrderStatus status = orderStatusMapper.selectOne(orderStatus);
        //对订单状态进行判断，
        if (status.getStatus() != OrderStatusEnum.INIT.value()) {
            //如果订单不为未支付则退出
            throw new LyException(HttpStatus.BAD_REQUEST, "订单状态不对哦");
        }

        String payUrl = payHelper.createPayUrl("乐友商城", id, 1l);
        if (StringUtils.isBlank(payUrl)) {
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "二维码链接获取失败");
        }
        //在获取到微信二维码后，生成支付日志，用于记录用户支付状态

        try {
            //先删除日志，在创建新的日志,防止日志插入重复
            payLogMapper.deleteByPrimaryKey(id);

            PayLog payLog = new PayLog();
            payLog.setOrderId(id);
            payLog.setTotalFee(order.getActualPay());
            payLog.setUserId(order.getUserId());
            payLog.setStatus(PayStatusEnum.NOT_PAY.value());
            payLog.setPayType(1);
            payLog.setCreateTime(new Date());

            int i = payLogMapper.insertSelective(payLog);
            if(i != 1){
                log.error("支付订单插入失败");
            }
        } catch (Exception e) {
            log.error("支付订单插入失败");
        }


        return payUrl;
    }

    public Order queryOrderById(Long id) {

        Order order = orderMapper.selectByPrimaryKey(id);
        if(order == null){
            throw new LyException(HttpStatus.NOT_FOUND,"订单查询失败，订单不存在");
        }
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(id);
        OrderStatus status = orderStatusMapper.selectOne(orderStatus);
        if(status == null){
            throw new LyException(HttpStatus.NOT_FOUND,"订单状态查询失败");
        }
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(id);
        List<OrderDetail> orderDetails = orderDetailMapper.select(orderDetail);
        if(CollectionUtils.isEmpty(orderDetails)){
            throw new LyException(HttpStatus.NOT_FOUND,"订单详情查询失败");
        }

        //封装order
        order.setOrderStatus(status);
        order.setOrderDetails(orderDetails);
        return order;
    }

    public Integer queryOrderStatusById(Long orderId) {

        // 优先去支付日志表中查询状态。
        PayLog payLog = payLogMapper.selectByPrimaryKey(orderId);

        if (payLog == null || PayStatusEnum.NOT_PAY.value() == payLog.getStatus()) {
            // 如果是未支付，则去调用微信查询支付状态接口
            return payHelper.queryPayState(orderId).getValue();
        }

        if (PayStatusEnum.SUCCESS.value() == payLog.getStatus()) {
            // 如果已经成功，返回1，代表支付成功
            return PayState.SUCCESS.getValue();
        }

        // 如果是其它状态，则认为支付失败，返回2
        return PayState.FAIL.getValue();
    }

    /**
     * 处理用户支付后的微信的回调结果，目的是将数据加密后与sign签名的对比，
     * @param map
     */
    @Transactional
    public void payNotify(Map<String, String> map) {

        //判断支付结果是否成功
        if(WXPayConstants.FAIL.equals(map.get("return_code"))){
            log.error("【微信下单】，用户支付失败，失败原因：{}",map.get("return_msg"));
            return;
        }

        //在payHelper中校验数据
        payHelper.payNotify(map);


    }
}
