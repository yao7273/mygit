package com.leyou.test;

import com.leyou.item.LyItemApplication;
import com.leyou.item.controller.BrandController;
import com.leyou.item.controller.CategoryController;
import com.leyou.item.pojo.Brand;
import com.leyou.item.pojo.Category;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LyItemApplication.class)
public class test {

    @Autowired
    private CategoryController categoryController;

    @Autowired
    private BrandController brandController;

    @Test
    public void test1(){

        ResponseEntity<List<Category>> cateList = categoryController.queryByCids(Arrays.asList(1l, 2l, 3l));
        for (Category category : cateList.getBody()) {
            System.out.println("category = " + category);
        }
        //文件测试，自己先预期
        Assert.assertEquals(3,cateList.getBody().size());
    }

    @Test
    public void test2(){

        ResponseEntity<List<Brand>> listResponseEntity = brandController.queryBrandsByIds(Arrays.asList(8557l, 18374l, 12669l, 25591l, 15127l));
        List<Brand> brands = listResponseEntity.getBody();
        for (Brand brand : brands) {
            System.out.println("brand = " + brand);
        }
    }

    @Test
    public void test22(){
        List<Long> spuids = new ArrayList<>();
        spuids.add(1l);
        spuids.add(1l);
        spuids.add(1l);
        StringBuffer sb = new StringBuffer();
        for (Long spuid : spuids) {
            sb.append(spuid);

        }
        String join = StringUtils.join(spuids, ",");
        System.out.println("join = " + join);
        System.out.println("sb = " + sb);
        String[] split = StringUtils.split(join, ",");
        System.out.println("split = " + split.toString());

    }


}
