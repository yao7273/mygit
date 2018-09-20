package com.leyou.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**

 * @create: 2018-08-16 14:59
 **/
@Getter
public class LyException extends RuntimeException{

    /*响应状态对象*/
    private HttpStatus status;

    /* 响应状态码*/
    private int statusCode;


    public LyException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public LyException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public LyException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
    public LyException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}