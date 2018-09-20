package com.leyou.item.test;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ItemService {

    public Item saveItem(Item item){
        int id = new Random().nextInt();
        item.setId(id);

        return item;

    }

}
