package com.leyou.item.api;

import com.leyou.item.pojo.Brand;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequestMapping("brand")
public interface BrandApi {

    /**
     * 根据品牌id查询品牌
     * @param id
     * @return
     */
    @GetMapping("{id}")
    Brand queryBrandById(@PathVariable("id")Long id);

    /**
     * 根据bids集合查询品牌集合
     * @param ids
     * @return
     */
    @GetMapping("list")
    List<Brand> queryBrandsByIds(@RequestParam("ids") List<Long> ids);

    }
