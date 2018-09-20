package com.leyou.item.controller;

import com.leyou.item.pojo.Category;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
@RunWith(SpringRunner.class)
@SpringBootTest
public class CategoryControllerTest {

    @Autowired
    private CategoryController categoryController;

    @Test
    public void queryAllByCid3() {

        ResponseEntity<List<Category>> allByCid3 = categoryController.queryAllByCid3(991l);
        List<Category> categories = allByCid3.getBody();
        for (Category category : categories) {
            System.out.println(category.getName());
        }


    }
}