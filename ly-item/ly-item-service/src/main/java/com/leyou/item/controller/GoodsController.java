package com.leyou.item.controller;

import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.CartDTO;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    /**
     * 分页获取商品列表数据
     * @param key  搜索得关键字
     * @param saleable  是否下架
     * @param page   当前页码
     * @param rows  每页得条数
     * @return
     */
    @GetMapping("spu/page")
    public ResponseEntity<PageResult<Spu>> querySpuByPage(
            @RequestParam(value = "key",required = false)String key,
            @RequestParam(value = "saleable",required = false)Boolean saleable,
            @RequestParam(value = "page",defaultValue = "1")Integer page,
            @RequestParam(value = "rows",defaultValue = "5")Integer rows){

        PageResult<Spu> pageResult = goodsService.querySpuByPage(key,saleable,page,rows);

        return ResponseEntity.ok(pageResult);
    }

    /**
     * 向数据库新增sku商品信息
     * @param spu
     * @return
     */
    @PostMapping("goods")
    public ResponseEntity<Void> saveGoods(@RequestBody Spu spu){
        goodsService.saveGoods(spu);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * 更新商品信息
     * @param spu
     * @return
     */
    @PutMapping("goods")
    public ResponseEntity<Void> updateGoods(@RequestBody Spu spu){
        goodsService.updateGoods(spu);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 根据spu的id查询商品详情信息
     * @param pid
     * @return
     */
    @GetMapping("spu/detail/{pid}")
    public ResponseEntity<SpuDetail> querySpuDetailByPid(@PathVariable("pid")Long pid){

        SpuDetail spuDetail = goodsService.querySpuDetailByPid(pid);

        return ResponseEntity.ok(spuDetail);

    }

    /**
     * 根据spu_id查询skus（商品信息）
     * @param id
     * @return
     */
    @GetMapping("/sku/list")
    public ResponseEntity<List<Sku>> querySkusByPid(@RequestParam("id")Long id){
        List<Sku> skus = goodsService.querySkusByPid(id);
        return ResponseEntity.ok(skus);
    }


    /**
     * 删除商品（逻辑删除）
     * @param spuid
     * @return
     */
    @GetMapping("deleteGood/{spuid}")
    public ResponseEntity<Void> deleteGood(@PathVariable("spuid")Long spuid){
        goodsService.deleteGood(spuid);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 下架商品
     * @param spuid
     * @return
     */
    @GetMapping("/soldOut/{spuid}")
    public ResponseEntity<Void> soldOutGood(@PathVariable("spuid")Long spuid){
        goodsService.soldOutGood(spuid);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 上架商品
     * @param spuid
     * @return
     */
    @GetMapping("/soldUp/{spuid}")
    public ResponseEntity<Void> soldUpGood(@PathVariable("spuid")Long spuid){
        goodsService.soldUpGood(spuid);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 根据spuId查询spu数据
     * @param id
     * @return
     */
    @GetMapping("spu/{id}")
    public ResponseEntity<Spu> querySpuByPid(@PathVariable("id") Long id){
        return ResponseEntity.ok(goodsService.querySpuByPid(id));
    }

    /**
     * 根据sku_ids集合查询skus集合
     * @param ids
     * @return
     */
    @GetMapping("sku/list/ids")
    public ResponseEntity<List<Sku>> querySkusByIds(@RequestParam("ids")List<Long> ids){

        return ResponseEntity.ok(goodsService.querySkusByIds(ids));

    }

    /**
     * 减库存，根据订单中商品集合
     * @param cartDTOS
     */
    @PostMapping("stock/decrease")
    public ResponseEntity<Void> decreaseStock(@RequestBody List<CartDTO> cartDTOS){

        goodsService.decreaseStock(cartDTOS);

        return ResponseEntity.ok().build();
    }

    /**
     * 根据spuids查询spus
     * @param ids
     * @return
     */
    @GetMapping("spus/spuids/{ids}")
    public ResponseEntity<List<Spu>> querySpusBySpuids(@PathVariable("ids")String ids){
        return ResponseEntity.ok(goodsService.querySpusBySpuids(ids));
    }

}
