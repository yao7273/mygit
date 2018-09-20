package com.leyou.item.controller;


import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Brand;
import com.leyou.item.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("brand")
public class BrandController {

    @Autowired
    private BrandService brandService;

    /**
     *h后台品牌的查询
     * @param page 当前页码
     * @param rows 示条数
     * @param desc 是否降序
     * @param sortBy 按照什么排序
     * @param key  搜索的关键字
     * @return
     */
    //根据分页组件中的pagination属性，确定参数，返回值则为查询到的数据，数据总条数
    @GetMapping("page")
    public ResponseEntity<PageResult<Brand>>  queryBrandByPage(
            @RequestParam(value = "page",defaultValue = "1") Integer page,
            @RequestParam(value = "rows",defaultValue = "5") Integer rows,
            @RequestParam(value = "desc",defaultValue = "false") boolean desc,
            @RequestParam(value = "sortBy",required = false) String sortBy,
            @RequestParam(value = "key",required = false) String key){

       PageResult<Brand> pageResult =  brandService.queryBrandByPage(page,rows,desc,sortBy,key);

       //判断查询到的结果是否为空
       if(pageResult.getItems() == null || pageResult.getItems().size()==0){
           return new ResponseEntity<>(HttpStatus.NOT_FOUND);
       }
       return ResponseEntity.ok(pageResult);
    }

    /**
     * 新增品牌,
     *
     * 需要在品牌表，品牌与商品分类的中间表中也插入数据
     *
     */
    @PostMapping
    //Void表示返回值为空，不需要返回值...
    // 前台传递的参数为集合时即（1，2，3，4），后端直接用list接收即可
    public ResponseEntity<Void> saveBrand(@RequestParam(value = "cids")List<Long> cids,
                                          Brand brand){
        brandService.saveBrand(cids,brand);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * 根据cid查询品牌
     * @return
     */
    @GetMapping("cid/{cid}")
    public ResponseEntity<List<Brand>> queryBrandBycid(@PathVariable("cid")Long cid){
        List<Brand> brands = brandService.queryBrandBycid(cid);
        return ResponseEntity.ok(brands);
    }

    /**
     * 根据品牌id查询品牌
     * @param id
     * @return
     */
    @GetMapping("{id}")
    public ResponseEntity<Brand> queryBrandById(@PathVariable("id")Long id){
       Brand brand =  brandService.queryBrandById(id);
       return ResponseEntity.ok(brand);
    }

    /**
     * 根据bids集合查询品牌集合
     * @param ids
     * @return
     */
    @GetMapping("list")
    public ResponseEntity<List<Brand>> queryBrandsByIds(@RequestParam("ids") List<Long> ids){

        return ResponseEntity.ok(brandService.queryBrandsByIds(ids));

    }


}
