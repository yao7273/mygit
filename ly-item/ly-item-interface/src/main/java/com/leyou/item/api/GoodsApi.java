package com.leyou.item.api;

import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.CartDTO;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping
public interface GoodsApi {

    /**
     * 分页获取商品列表数据
     * @param key  搜索得关键字
     * @param saleable  是否下架
     * @param page   当前页码
     * @param rows  每页得条数
     * @return
     */
    @GetMapping("spu/page")
    PageResult<Spu> querySpuByPage(
            @RequestParam(value = "key",required = false)String key,
            @RequestParam(value = "saleable",required = false)Boolean saleable,
            @RequestParam(value = "page",defaultValue = "1")Integer page,
            @RequestParam(value = "rows",defaultValue = "5")Integer rows);

    /**
     * 根据spu的id查询商品详情信息
     * @param pid
     * @return
     */
    @GetMapping("spu/detail/{pid}")
    SpuDetail querySpuDetailByPid(@PathVariable("pid")Long pid);


    /**
     * 根据spu_id查询skus（商品信息）
     * @param id
     * @return
     */
    @GetMapping("/sku/list")
    List<Sku> querySkusByPid(@RequestParam("id")Long id);

    /**
     * 根据spuId查询spu数据
     * @param id
     * @return
     */
    @GetMapping("spu/{id}")
    Spu querySpuByPid(@PathVariable("id") Long id);

    /**
     * 根据sku_ids集合查询skus集合
     * @param ids
     * @return
     */
    @GetMapping("sku/list/ids")
    List<Sku> querySkusByIds(@RequestParam("ids")List<Long> ids);

    /**
     * 减库存，根据订单中商品集合
     * @param cartDTOS
     */
    @PostMapping("stock/decrease")
    void decreaseStock(@RequestBody List<CartDTO> cartDTOS);

    /**
     * 根据spuids查询spus
     * @param ids
     * @return
     */
    @GetMapping("spus/spuids/{ids}")
    List<Spu> querySpusBySpuids(@PathVariable("ids")String ids);
    }
