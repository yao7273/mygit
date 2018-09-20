package com.leyou.item.service;

import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    /**
     * 根据分类cid查询规格组信息
     * @param cid
     * @return
     */
    public List<SpecGroup> querySpecGroup(Long cid) {

        SpecGroup s = new SpecGroup();
        s.setCid(cid);

        List<SpecGroup> groups = specGroupMapper.select(s);

        if (CollectionUtils.isEmpty(groups)) {
            throw new LyException(HttpStatus.NOT_FOUND, "没找到相应规格组");
        }


        return groups;
    }

    /**
     * 查询规格组中参数信息(多条件都可使用)
     *
     * @param gid       根据规格组id
     * @param cid       根据商品分类id
     * @param generic   根据是否是sku通用属性
     * @param searching 是否用于搜索过滤
     * @return
     */
    public List<SpecParam> querySpecParam(Long gid, Long cid, Boolean generic, Boolean searching) {

        SpecParam sp = new SpecParam();
        sp.setGroupId(gid);
        sp.setCid(cid);
        sp.setGeneric(generic);
        sp.setSearching(searching);

        List<SpecParam> specParams = specParamMapper.select(sp);

        if (CollectionUtils.isEmpty(specParams)) {
            throw new LyException(HttpStatus.NOT_FOUND, "没找到相应规格参数");
        }

        return specParams;

    }

    /**
     * 规格组得修改
     *
     * @param specGroup
     */

    public void updateGroup(SpecGroup specGroup) {

        specGroupMapper.updateByPrimaryKeySelective(specGroup);
    }

    /**
     * 删除规格组（需要把规格参数组也一起删除）
     *
     * @param gid
     */
    public void deleteGroupById(Long gid) {

        //先删除外键约束表中数据
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParamMapper.delete(specParam);

        specGroupMapper.deleteByPrimaryKey(gid);

    }

    @Transactional
    public void updateOrSaveParam(SpecParam specParam) {

        //先查询数据库中书否已有该数据，没有则新增，有则删除
        SpecParam specParam1 = specParamMapper.selectByPrimaryKey(specParam.getId());
        if (specParam1 == null) {
            specParamMapper.insertSelective(specParam);
        } else {
            //没有则修改
            specParamMapper.updateByPrimaryKeySelective(specParam);
        }
    }

    public void deleteParamById(Long pid) {

        specParamMapper.deleteByPrimaryKey(pid);

    }

    /**
     * 根据商品分类id查询规格组集合
     * @param cid
     * @return
     */
    public List<SpecGroup> querySpecsByCid(Long cid) {

        //先查询规格组
        List<SpecGroup> specGroups = this.querySpecGroup(cid);
        //查询当前分类下的所有规格参数
        List<SpecParam> specParams = this.querySpecParam(null, cid, null, null);

        //整理规格组与规格参数，
        Map<Long,List<SpecParam>> map = new HashMap<>();
        for (SpecParam specParam : specParams) {
            //遍历规格参数，封装到map集合中
            if(!map.containsKey(specParam.getGroupId())){
                //如果不存在则新建，
                map.put(specParam.getGroupId(),new ArrayList<>());
            }
            //再添加
            map.get(specParam.getGroupId()).add(specParam);
        }
        //循环遍历规格组，添加specParam数据
        for (SpecGroup specGroup : specGroups) {
            specGroup.setParams(map.get(specGroup.getId()));
        }
        return specGroups;
    }
}
