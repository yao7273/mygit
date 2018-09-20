package com.leyou.item.test;

import com.leyou.common.exception.LyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ItemController {

    @Autowired
    private ItemService itemService;

    @PostMapping("item")
    public ResponseEntity<Item> saveItem(Item item){
        if(item.getPrice() == null){
            //return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

            //统一的异常处理
            //throw new RuntimeException("价格不能为空");
            //使用自定义的异常类传递
            throw  new LyException(HttpStatus.NOT_FOUND,"价格不能为空7777");
        }
        Item item1 = itemService.saveItem(item);

        return ResponseEntity.ok(item1);


    }
}
