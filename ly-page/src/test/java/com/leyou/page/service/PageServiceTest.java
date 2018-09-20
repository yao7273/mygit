package com.leyou.page.service;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileNotFoundException;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PageServiceTest {

    @Autowired
    private PageService pageService;

    @Test
    public void toGoodsPage() {

        Map<String, Object> map = pageService.toGoodsPage(144l);
        Object specs = map.get("specs");
        System.out.println("specs = " + specs.toString());
        System.out.println("specs = " + map.get("spu").toString());
        System.out.println("specs = " + map.get("skus").toString());

    }

    @Test
    public void createHtml() throws FileNotFoundException {
        pageService.createHtml(144l);

    }
}