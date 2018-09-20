package com.leyou.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor //无参构造函数
@AllArgsConstructor  //带所有参数的构造函数
public class UserInfo {

    private Long id;

    private String username;
}