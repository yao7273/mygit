package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;


    /**
     * @param page
     * @param rows
     * @param desc
     * @param sortBy
     * @param key
     * @return
     */
    public PageResult<Brand> queryBrandByPage(Integer page, Integer rows, boolean desc, String sortBy, String key) {

        //分页查询Brand品牌数据
        //开启分页助手，
        PageHelper.startPage(page, rows);

        //使用通用mapper进行多条件查询
        Example example = new Example(Brand.class);
        Example.Criteria criteria = example.createCriteria();  //------------------此处不同

        //拼接条件，判断搜索条件是否为空,同时选择是按name模糊查找，还是按照字母精确查找，--------测试判断是否无视大小写，无视
        if (StringUtils.isNotBlank(key)) {
            criteria.orLike("name", "%" + key + "%")
                    .orEqualTo("letter", key);  //key.toUpperCase()
        }
        //排序，判断排序的条件，以及是否降序或是升序
        if (StringUtils.isNotBlank(sortBy)) {
            //此处sql语句相当于SELECT * FROM `tb_brand` order by id desc;需要有个空格
            String orderByCaluse = sortBy + (desc ? " desc" : " asc");
            example.setOrderByClause(orderByCaluse);

        }

        List<Brand> brands = brandMapper.selectByExample(example);

        //记住，每次查询完结果后，都要判断结果是否为空，如果为空，则通过抛自定义异常的方式返回信息给页面
        if (brands == null) {
            throw new LyException(HttpStatus.NOT_FOUND, "没有查询到相应的品牌");
        }

        //
        PageInfo<Brand> pageInfo = new PageInfo<>(brands);
        //返回封装好的结果对象
        return new PageResult<>(pageInfo.getTotal(), brands);
    }

    /**
     * 新增品牌
     *
     * @param cids
     * @param brand
     */
    public void saveBrand(List<Long> cids, Brand brand) {

        //新增品牌
        //将brand中的id设为null，数据库自己添加主键值
        int i = brandMapper.insertSelective(brand);
        //判断是否插入成功
        if (i == 0) {
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "新增品牌失败");
        }
        //在中间表中新增(两种方式，一：新建中间表的实体类，使用通用mapper
        //                          二：使用mapper自定义sql语句)
        for (Long cid : cids) {
            i = brandMapper.saveBrand(cid, brand.getId());
        }
    }

    /**
     * 查询品牌，通过bid
     * @param brandId
     * @return
     */
    public Brand queryByid(Long brandId) {

        Brand brand = brandMapper.selectByPrimaryKey(brandId);

        if (brand == null) {
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "商品所属品牌不存在");
        }

        return brand;
    }

    /**
     * 通过cid查询品牌
     * @param cid
     * @return
     */
    public List<Brand> queryBrandBycid(Long cid) {

        //需要夺标连个查询
        List<Brand> brands = brandMapper.queryBrandBycid(cid);

        if(CollectionUtils.isEmpty(brands)){
            throw new LyException(HttpStatus.NOT_FOUND,"没有找到分类");
        }

        return brands;
    }

    public Brand queryBrandById(Long id) {
        Brand brand = brandMapper.selectByPrimaryKey(id);
        return brand;
    }

    public List<Brand> queryBrandsByIds(List<Long> ids) {

        List<Brand> brands = brandMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(brands)){
            throw new LyException(HttpStatus.NOT_FOUND,"品牌查询失败");
        }
        return brands;
    }
}
