package com.leyou.test;

import com.leyou.LySearchApplication;
import com.leyou.item.pojo.Brand;
import com.leyou.search.client.BrandClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LySearchApplication.class)
public class BrandClientTest {

    @Autowired
    private BrandClient brandClient;

    @Test
    public void test(){

        List<Brand> brands = brandClient.queryBrandsByIds(Arrays.asList(8557l, 18374l, 12669l, 25591l, 15127l));
        for (Brand brand : brands) {
            System.out.println("brand = " + brand);
        }
    }
    @Test
    public void test2(){

        Brand brand = brandClient.queryBrandById(8557l);
        System.out.println("brand = " + brand);

    }

}
