package com.leyou.item.pojo;

import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.List;

//规格参数组实体类
@Table(name = "tb_spec_group")
@Data
public class SpecGroup {

    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;
    //商品分类id
    private Long cid;
    //规格组名称
    private String name;

    //该组下的所有规格参数集合，用于在商品详情页面渲染使用
    @Transient
    private List<SpecParam> params;
}