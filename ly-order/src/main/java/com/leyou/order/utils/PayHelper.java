package com.leyou.order.utils;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import com.leyou.common.exception.LyException;
import com.leyou.order.config.WXPayConfigImpl;
import com.leyou.order.enuma.OrderStatusEnum;
import com.leyou.order.enuma.PayState;
import com.leyou.order.enuma.PayStatusEnum;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.mapper.PayLogMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.pojo.PayLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.wxpay.sdk.WXPayConstants.FAIL;
import static com.github.wxpay.sdk.WXPayConstants.SignType;

//import static com.github.wxpay.sdk.WXPayConstants.SignType;

/**
 * 用于封装与微信api交互的基本数据，
 */
@Slf4j
public class PayHelper {

    private WXPay wxPay;

    private WXPayConfigImpl payConfigImpl;

    //使用redis将微信返回的支付链接存入redis，并设置30分钟失效
    //同时判断数据库中订单的状态，为1表示未付款才会查询redis中该订单的二维码链接。没有则从微信获取
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private PayLogMapper payLogMapper;

    //存入redis中的订单前缀
    private static final String PRE_KEY = "order:pay:url:";

    public PayHelper(WXPayConfigImpl wxPayConfig) {
        wxPay = new WXPay(wxPayConfig, SignType.HMACSHA256);
        payConfigImpl = wxPayConfig;
    }

    //封装与微信交互的数据，返回值为codeUrl，生成二维码的地址数据
    public String createPayUrl(String body, Long orderId, Long totalFee) {
        //存入redis的二维码链接
        String key = PRE_KEY + orderId.toString();
        //订单状态为1未付款时才会去redis中查询,返回订单地址
        System.out.println("key = " + key);
        try {
            String url = redisTemplate.opsForValue().get(key);
            if(StringUtils.isNotBlank(url)){
                return url;
            }
        } catch (Exception e) {
            log.error("查询redis中的url链接异常，订单号：{}",orderId);
        }

/*            //如果订单状态不为未支付状态，如果redis中有支付链接，则直接移除
            if(redisTemplate.hasKey(key))
                redisTemplate.delete(key);
        */

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("body", body);  //商品描述
        data.put("out_trade_no", orderId.toString());
        data.put("total_fee", totalFee.toString());
        data.put("spbill_create_ip", payConfigImpl.getSpbillCreateIp());
        data.put("notify_url", payConfigImpl.getNotifyUrl());
        data.put("trade_type", payConfigImpl.getTradeType());
        // data.put("time_expire", "20170112104120");

        try {
            //会自动调用wxPay中的方法，将上述传入wxPay中的数据配置信息进行封装
            //以上为对于微信交互的数据的封装，在unifiedOrder方法中会自动调用方法将数据发送到微信端的指定地址
            //  并且会将返回的string类型数据转换成map结构
            Map<String, String> r = wxPay.unifiedOrder(data);

            //其中以codeUrl在return_code 和result_code都为SUCCESS的时候才会返回
            //而result_code字段在return_code为SUCCESS的时候才会返回
            //所以先判断return_code是否SUCCESS,FAIL则抛异常
            if (FAIL.equals(r.get("return_code"))) {
                log.error("【微信下单】，于微信通信失败，订单号：{}，失败原因：{}", orderId,
                        r.get("return_msg"));
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "下单失败");
            }
            //判断result_code是否SUCCESS,FAIL则抛异常
            if (FAIL.equals(r.get("result_code"))) {
                log.error("【微信下单】，创建预交易账单失败，订单号：{}，错误代码：{}，错误代码描述:{}", orderId,
                        r.get("err_code"), r.get("err_code_des"));
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "下单失败");
            }
            //校验签名是否有效
            isSignatureValid(r);

            //获取map中的codeUrl数据
            String codeUrl = r.get("code_url");

            try {
                //将微信返回的二维码链接存入redis，30分中有效
                redisTemplate.opsForValue().set(key, codeUrl, 30, TimeUnit.MINUTES);
            } catch (Exception e) {
                //redis缓存异常值记录日志
                log.error("【微信下单】，redis缓存二维码链接失败，订单号：{}，错误代码：{}，错误代码描述:{}", orderId,
                        r.get("err_code"), r.get("err_code_des"));
            }

            return codeUrl;
        } catch (Exception e) {
            log.error("【微信下单】，下单失败，订单号：{}", orderId, e);
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "下单失败");
        }
    }

    private void isSignatureValid(Map<String, String> r)  {
        try {
            //校验微信返回的数据的正确性,（将微信返回的数据，使用密钥与加密方式，与自己的相比）由于微信为返回签名类型
            boolean b1 = WXPayUtil.isSignatureValid(r, payConfigImpl.getKey(), SignType.MD5);
            boolean b2 = WXPayUtil.isSignatureValid(r, payConfigImpl.getKey(), SignType.HMACSHA256);
            if (!b1 && !b2) {
                //如果两种机密方式得到的结构都错误，则抛异常
                log.error("【微信下单】，签名校验失败，");
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "签名校验失败");
            }
        } catch (Exception e) {
            log.error("【微信下单】，签名校验失败");
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "签名校验失败");
        }
    }


    /**
     //校验签名
     //校验金额
     //判断订单状态
     //修改日志
     //修改订单状态
     * @param msg
     */
    public void payNotify(Map<String, String> msg) {
        //1.校验签名
        isSignatureValid(msg);
        //2.校验金额,将数据库中订单的金额与微信返回的数据中的金额对比
        //2.1,解析数据
        String totalFee = msg.get("total_fee"); //订单总金额
        String outTradeNo = msg.get("out_trade_no"); //商品的订单号
        String transactionId = msg.get("transaction_id"); //微信支付订单号
        String bankType = msg.get("bank_type");  //银行支付类型
        if (StringUtils.isBlank(outTradeNo) || StringUtils.isBlank(totalFee)
                || StringUtils.isBlank(transactionId) || StringUtils.isBlank(bankType)) {
            log.error("【微信支付回调】支付回调返回数据不正确");
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "数据不正确");
        }
        // 2.2.查询订单,根据微信返回的订单id查询数剧
        System.out.println("outTradeNo:"+outTradeNo);
        System.out.println("Long.valueOf(outTradeNo):"+Long.valueOf(outTradeNo));

        Order order = orderMapper.selectByPrimaryKey(Long.valueOf(outTradeNo));
        System.out.println("order = " + order.toString());
        // 2.3.校验金额，此处因为我们支付的都是1，所以写死了，应该与订单中的对比
        if (1L != Long.valueOf(totalFee)) {
            log.error("【微信支付回调】支付回调返回数据不正确");
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "数据不正确");
        }
        // 判断支付状态
        OrderStatus status = statusMapper.selectByPrimaryKey(order.getOrderId());
        if (status.getStatus() != OrderStatusEnum.INIT.value()) {
            // 如果不是未支付状态，则都认为支付成功！
            return;
        }
        // 3、修改支付日志状态
        PayLog payLog = payLogMapper.selectByPrimaryKey(order.getOrderId());
        // 只有未支付订单才需要修改
        if (payLog.getStatus() == PayStatusEnum.NOT_PAY.value()) {
            payLog.setOrderId(order.getOrderId());
            payLog.setStatus(PayStatusEnum.SUCCESS.value());
            payLog.setTransactionId(transactionId);
            payLog.setBankType(bankType);
            payLog.setPayTime(new Date());
            payLogMapper.updateByPrimaryKeySelective(payLog);
        }

        // 4、修改订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(order.getOrderId());
        orderStatus.setPaymentTime(new Date());
        orderStatus.setStatus(OrderStatusEnum.PAY_UP.value());
        statusMapper.updateByPrimaryKeySelective(orderStatus);
    }

    /**
     * 商户主动向微信查询订单状态
     * @param orderId
     * @return
     */
    public PayState queryPayState(Long orderId) {
        Map<String, String> data = new HashMap<>();
        // 订单号
        data.put("out_trade_no", orderId.toString());
        try {
            Map<String, String> result = this.wxPay.orderQuery(data);
            // 链接失败
            if (result == null || WXPayConstants.FAIL.equals(result.get("return_code"))) {
                // 未查询到结果或链接失败，认为是未付款
                log.info("【支付状态查询】链接微信服务失败，订单编号：{}", orderId);
                return PayState.NOT_PAY;
            }
            // 查询失败
            if (WXPayConstants.FAIL.equals(result.get("result_code"))) {
                log.error("【支付状态查询】查询微信订单支付状态失败，错误码：{}，错误信息：{}",
                        result.get("err_code"), result.get("err_code_des"));
                return PayState.NOT_PAY;
            }
            // 校验签名
            isSignatureValid(result);
            String state = result.get("trade_state");
            if ("SUCCESS".equals(state)) {
                // 修改支付状态等信息
                payNotify(result);

                // success，则认为付款成功
                return PayState.SUCCESS;
            } else if (StringUtils.equals("USERPAYING", state) || StringUtils.equals("NOTPAY", state)) {
                // 未付款或正在付款，都认为是未付款
                return PayState.NOT_PAY;
            } else {
                // 其它状态认为是付款失败
                return PayState.FAIL;
            }
        } catch (Exception e) {
            log.error("查询订单状态异常", e);
            return PayState.NOT_PAY;
        }
    }
}
