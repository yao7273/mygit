package com.leyou.item.mapper;

import com.leyou.item.pojo.Category;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;


public interface CategoryMapper extends Mapper<Category>,IdListMapper<Category,Long> {


    @Select("SELECT * FROM `tb_category` where id in (SELECT category_id FROM tb_category_brand where brand_id = #{bid})")
    List<Category> queryCateByBid(@Param("bid") Long bid);
}
