package com.leyou.test;

import com.leyou.LySearchApplication;
import com.leyou.item.pojo.Category;
import com.leyou.search.client.CategoryClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LySearchApplication.class)
public class CateTest {

    @Autowired
    private CategoryClient categoryClient;

    @Test
    public void test1(){

        List<Category> cateList = categoryClient.queryByCids(Arrays.asList(1l, 2l, 3l));
        for (Category category : cateList) {
            System.out.println("category = " + category);
        }
        //文件测试，自己先预期
        //Assert.assertEquals(3,cateList.size());

    }


}
