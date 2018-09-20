package com.leyou.item.controller;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("spec")
public class SpecificationController {

    @Autowired
    private SpecificationService service;

    /**
     * 查询规格组数据
     * @param cid
     * @return
     */
    @GetMapping("groups/{cid}")
    public ResponseEntity<List<SpecGroup>> querySpecGroup(@PathVariable("cid")Long cid){

        List<SpecGroup> specGroups = service.querySpecGroup(cid);
        return ResponseEntity.ok(specGroups);
    }

    /**
     * 查询规格组中参数信息(多条件都可使用)
     * @param gid  根据规格组id
     * @param cid   根据商品分类id
     * @param generic  根据是否是sku通用属性
     * @param searching  是否用于搜索过滤
     * @return
     */
    @GetMapping("/params")
    public ResponseEntity<List<SpecParam>> querySpecParam(
            @RequestParam(value = "gid",required = false)Long gid,
            @RequestParam(value = "cid",required = false)Long cid,
            @RequestParam(value = "generic",required = false)Boolean generic,
            @RequestParam(value = "searching",required = false)Boolean searching){

        List<SpecParam> specParams = service.querySpecParam(gid,cid,generic,searching);

        return ResponseEntity.ok(specParams);
    }

    /**
     * 规格组得修改
     * @param specGroup
     * @return
     */
    @PutMapping("/group")
    public ResponseEntity<Void> updateGroup(@RequestBody SpecGroup specGroup){

        service.updateGroup(specGroup);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 删除规格组
     * @param gid
     * @return
     */
    @DeleteMapping("/group/{gid}")
    public ResponseEntity<Void> deleteGroupById(@PathVariable("gid")Long gid){

        service.deleteGroupById(gid);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 修改||新增规格参数信息
     * @param specParam
     * @return
     */
    @RequestMapping("/param")
    public ResponseEntity<Void> updateOrSaveParam(@RequestBody SpecParam specParam){

        service.updateOrSaveParam(specParam);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 删除规格参数信息
     * @param pid
     * @return
     */
    @DeleteMapping("/param/{pid}")
    public ResponseEntity<Void> deleteParamById(@PathVariable("pid")Long pid){

        service.deleteParamById(pid);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 根据商品分类id查询规格组集合
     * @param cid
     * @return
     */
    @GetMapping("{cid}")
    public ResponseEntity<List<SpecGroup>> querySpecsByCid(@PathVariable("cid")Long cid){

       return ResponseEntity.ok( service.querySpecsByCid(cid));

    }


}
