package com.leyou.common.advice;

import com.leyou.common.exception.LyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-16 14:48
 **/
@ControllerAdvice
@Slf4j
public class BasicExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e){
        // 记录日志
        log.error(e.getMessage(), e);
        // 判断是否是自定义异常
        if(e instanceof LyException){
            LyException e2 = (LyException) e;
            // 获取状态码
            int code = e2.getStatus() == null ? e2.getStatusCode() : e2.getStatus().value();
            return ResponseEntity.status(code).body(e.getMessage());
        }
        // 其它异常处理
        return ResponseEntity.status(400).body("未知错误！");
    }

}
