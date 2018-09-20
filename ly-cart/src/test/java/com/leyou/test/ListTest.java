package com.leyou.test;

import org.junit.Test;

import java.util.*;

public class ListTest {

    @Test
    public void test() {

        HashMap<Integer, User> map = new HashMap<>();
        map.put(1, new User("张三", 25));
        map.put(2, new User("李四", 22));
        map.put(3, new User("王五", 28));

        HashMap<Integer, User> map1 = sortHashMap(map);


    }

    private HashMap<Integer, User> sortHashMap(HashMap<Integer, User> map) {

        Set<Map.Entry<Integer, User>> entrySet = map.entrySet();
        List<Map.Entry<Integer, User>> entryList = new ArrayList<>(entrySet);
        entryList.sort((i1, i2) -> {
            return i1.getValue().getAge() - i2.getValue().getAge();
        });
        System.out.println("entryList = " + entryList);
        LinkedHashMap<Integer, User> linkedHashMap = new LinkedHashMap<>();
        entryList.forEach(e -> linkedHashMap.put(e.getKey(), e.getValue()));
        System.out.println("linkedHashMap = " + linkedHashMap);
        return linkedHashMap;
    }

    @Test
    public void test2(){

        /**
         * 2出现了1
         7出现了1
         8出现了1
         88出现了1
         77出现了1
         1出现了3
         4出现了4
         5出现了4
         */
        int[] arr = {1,4,1,4,2,5,4,5,8,7,77,88,5,4,1,5};
        HashMap<Integer,Integer> map = new HashMap<>();
        for (int i = 0; i < arr.length; i++) {
            if(map.containsKey(arr[i])){
                map.put(arr[i],map.get(arr[i])+1);
            }else {
                map.put(arr[i], 1);
            }
        }
        Set<Map.Entry<Integer, Integer>> entries = map.entrySet();
        LinkedList<Map.Entry<Integer, Integer>> entries1 = new LinkedList<>(entries);
        entries1.sort((i1,i2) -> {return i1.getValue() - i2.getValue();});

        System.out.println("entries1 = " + entries1);
        for (Map.Entry<Integer, Integer> entry : entries1) {
            System.out.println(  entry.getKey()+"出现了"+entry.getValue());

        }

    }

}
