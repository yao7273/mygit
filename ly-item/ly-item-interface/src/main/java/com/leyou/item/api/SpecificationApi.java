package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequestMapping("spec")
public interface SpecificationApi {


    @GetMapping("/params")
    List<SpecParam> querySpecParam(
            @RequestParam(value = "gid",required = false)Long gid,
            @RequestParam(value = "cid",required = false)Long cid,
            @RequestParam(value = "generic",required = false)Boolean generic,
            @RequestParam(value = "searching",required = false)Boolean searching);

    /**
     * 根据商品分类id查询规格参数
     * @param cid
     * @return
     */
    @GetMapping("{cid}")
    List<SpecGroup> querySpecsByCid(@PathVariable("cid")Long cid);



    }
