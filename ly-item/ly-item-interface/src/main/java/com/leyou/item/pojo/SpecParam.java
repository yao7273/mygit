package com.leyou.item.pojo;

import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "tb_spec_param")
@Data
public class SpecParam {
    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;
    private Long cid;
    private Long groupId;
    private String name;
    @Column(name = "`numeric`")
    private Boolean numeric;
    private String unit;
    private Boolean generic;
    private Boolean searching;
    private String segments;


    /* `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
      `cid` bigint(20) NOT NULL COMMENT '商品分类id',
      `group_id` bigint(20) NOT NULL,
      `name` varchar(256) NOT NULL COMMENT '参数名',
      `numeric` tinyint(1) NOT NULL COMMENT '是否是数字类型参数，true或false',
      `unit` varchar(256) DEFAULT '' COMMENT '数字类型参数的单位，非数字类型可以为空',
      `generic` tinyint(1) NOT NULL COMMENT '是否是sku通用属性，true或false',
      `searching` tinyint(1) NOT NULL COMMENT '是否用于搜索过滤，true或false',
      `segments` varchar(1024) DEFAULT '' COMMENT '数值类型参数，如果需要搜索，则添加分段间隔值，如CPU频率间隔：0.5-1.0',
*/


}