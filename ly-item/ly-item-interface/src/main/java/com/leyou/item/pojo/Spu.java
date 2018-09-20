package com.leyou.item.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

@Table(name = "tb_spu")
@Data
public class Spu {
    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;
    private Long brandId;
    private Long cid1;// 1级类目
    private Long cid2;// 2级类目
    private Long cid3;// 3级类目
    private String title;// 标题
    private String subTitle;// 子标题
    private Boolean saleable;// 是否上架
    @JsonIgnore//从数据库中获取过该字段数据后，不双环成json数据传向前端
    private Boolean valid;// 是否有效，逻辑删除用
    @JsonIgnore
    private Date createTime;// 创建时间
    @JsonIgnore
    private Date lastUpdateTime;// 最后修改时间

    @Transient //在数据库中忽略该字段
    private String cname;  //商品分类，三级分类都显示
    @Transient
    private String bname;  //品牌名称

    @Transient
    private SpuDetail spuDetail; //商品详情

    @Transient
    private List<Sku> skus; //sku列表
}