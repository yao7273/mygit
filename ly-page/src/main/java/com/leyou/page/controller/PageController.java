package com.leyou.page.controller;

import com.leyou.page.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class PageController {

    @Autowired
    private PageService pageService;

    /**
     * 查询商品详情页
     * @param id
     * @param model
     * @return
     */
    @GetMapping("item/{id}.html")
    public String toGoodsPage(@PathVariable("id")Long id, Model model){
        //通过model模型传递数据
        Map<String,Object> data = pageService.toGoodsPage(id);
        model.addAllAttributes(data);

        //创建html
        pageService.asyncCreateHtml(id);

        return "item";
    }


}
