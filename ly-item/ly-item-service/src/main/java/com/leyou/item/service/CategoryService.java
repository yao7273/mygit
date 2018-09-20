package com.leyou.item.service;

import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> queryListByParentId(Long pid){

        Category category = new Category();
        category.setParentId(pid);

        List<Category> categoryList = categoryMapper.select(category);

        return categoryList;

    }


    public List<Category> queryCateByBid(Long bid) {

       List<Category>  categories = categoryMapper.queryCateByBid(bid);

       return categories;

    }

    /**
     * 根据id集合查询商品分类集合
     * @param longs
     * @return
     */
    public List<Category> selectByIdList(List<Long> longs) {

        List<Category> categories = categoryMapper.selectByIdList(longs);
        if (CollectionUtils.isEmpty(categories)) {
            // 没找到，返回404
            throw new LyException(HttpStatus.NOT_FOUND, "该分类不存在");
        }

        return categories;
    }


    /**
     * 根据cid3查询三级分类
     * @param id
     * @return
     */
    public List<Category> queryAllByCid3(Long id) {
        Category c3 = categoryMapper.selectByPrimaryKey(id);
        Category c2 = categoryMapper.selectByPrimaryKey(c3.getParentId());
        Category c1 = categoryMapper.selectByPrimaryKey(c2.getParentId());
            return Arrays.asList(c1,c2,c3);
    }
}
