package com.leyou.item.mapper;

import com.leyou.common.mapper.BaseMapper;
import com.leyou.item.pojo.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface SkuMapper extends BaseMapper<Sku> {


    /**
     * 此处会有线程安全问题
     * 这里减库存并没有采用先查询库存，判断充足才减库存的方案，
     * 那样会有线程安全问题，当然可以通过加锁解决。
     * 不过我们此处为了效率，并没有使用悲观锁，而是对库存采用了乐观锁方案
     *
     * 即在数据库减少库存时，如果库存少于要减少的数量，则修改失败，返回0
     */
    @Update("UPDATE tb_stock SET stock = stock - #{num} WHERE sku_id = #{id} AND stock >= #{num}")
    int decreaseStock(@Param("id")Long id,@Param("num")Integer num);

}
