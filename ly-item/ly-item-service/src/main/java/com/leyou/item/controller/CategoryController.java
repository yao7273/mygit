package com.leyou.item.controller;

import com.leyou.item.pojo.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("category") //网关接口为item
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 根据pid查询商品分类
     * @param pid
     * @return
     */
    @GetMapping("list")
    public ResponseEntity<List<Category>> queryByParentId(@RequestParam(value = "pid",defaultValue = "0") Long pid ){

        List<Category> categories = categoryService.queryListByParentId(pid);

        //返回数据前先判断查询的结果中是否数据
        if(categories == null || categories.size()<1){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(categories);
    }

    /**
     * 根据品牌id查询分类数据
     */
    @GetMapping("bid/{bid}")
    public ResponseEntity<List<Category>> queryCateByBid(@PathVariable("bid")Long bid){

        List<Category> categories = categoryService.queryCateByBid(bid);

        if(categories == null || categories.size()<0){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(categories);
    }

    /**
     * 根据分类ids集合查询分类
     * @param ids
     * @return
     */
    @GetMapping("list/ids")
    public ResponseEntity<List<Category>> queryByCids(@RequestParam("ids")List<Long>  ids){

       List<Category> categories =  categoryService.selectByIdList(ids);

        return ResponseEntity.ok(categories);
    }

    ///item/category/all/level?id="+data.categories.id

    /**
     * 根据cid3查询三级分类
     * @param id
     * @return
     */
    @GetMapping("all/level")
    public ResponseEntity<List<Category>> queryAllByCid3(@RequestParam("id")Long id){

        List<Category> categories = categoryService.queryAllByCid3(id);

        if(CollectionUtils.isEmpty(categories)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(categories);

    }



}
