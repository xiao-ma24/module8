package com.school.material.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.common.BusinessException;
import com.school.material.common.DictTypes;
import com.school.material.common.ErrorCode;
import com.school.material.dto.CategoryTreeResp;
import com.school.material.dto.MaterialCategoryReq;
import com.school.material.entity.Material;
import com.school.material.entity.SysDictItem;
import com.school.material.mapper.MaterialMapper;
import com.school.material.mapper.SysDictItemMapper;
import com.school.material.service.MaterialCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 物资分类服务：分类不再单独建表，统一读写平台字典 sys_dict_item
 */
@Service
@RequiredArgsConstructor
public class MaterialCategoryServiceImpl implements MaterialCategoryService {

    /** 物资分类对应的字典类型 */
    private static final String DICT_TYPE = DictTypes.MATERIAL_CATEGORY;

    private final SysDictItemMapper dictItemMapper;
    private final MaterialMapper materialMapper;

    @Override
    public List<CategoryTreeResp> tree(Integer status) {
        List<SysDictItem> items = dictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, DICT_TYPE)
                .eq(status != null, SysDictItem::getItemStatus, status)
                .orderByAsc(SysDictItem::getSortNo)
                .orderByAsc(SysDictItem::getItemId));

        List<CategoryTreeResp> nodes = new ArrayList<>();
        for (SysDictItem item : items) {
            nodes.add(CategoryTreeResp.from(item));
        }
        return nodes;
    }

    @Override
    public CategoryTreeResp getById(Long id) {
        return CategoryTreeResp.from(mustGet(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MaterialCategoryReq req) {
        checkCategoryNoUnique(req.getCategoryNo(), null);

        SysDictItem entity = new SysDictItem();
        entity.setDictType(DICT_TYPE);
        entity.setItemValue(req.getCategoryNo().trim());
        entity.setItemLabel(req.getCategoryName().trim());
        entity.setSortNo(req.getSort() == null ? 0 : req.getSort());
        entity.setItemStatus(req.getStatus() == null ? 1 : req.getStatus());
        dictItemMapper.insert(entity);
        return entity.getItemId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MaterialCategoryReq req) {
        SysDictItem entity = mustGet(id);
        checkCategoryNoUnique(req.getCategoryNo(), id);

        entity.setItemValue(req.getCategoryNo().trim());
        entity.setItemLabel(req.getCategoryName().trim());
        entity.setSortNo(req.getSort() == null ? 0 : req.getSort());
        entity.setItemStatus(req.getStatus() == null ? 1 : req.getStatus());
        dictItemMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDictItem entity = mustGet(id);

        Long materials = materialMapper.selectCount(Wrappers.<Material>lambdaQuery()
                .eq(Material::getCategoryCode, entity.getItemValue()));
        if (materials != null && materials > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_MATERIAL, "该分类下存在物资，不能删除");
        }

        dictItemMapper.deleteById(id);
    }

    private SysDictItem mustGet(Long id) {
        SysDictItem entity = dictItemMapper.selectById(id);
        if (entity == null || !DICT_TYPE.equals(entity.getDictType())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类不存在");
        }
        return entity;
    }

    private void checkCategoryNoUnique(String categoryNo, Long selfId) {
        Long count = dictItemMapper.selectCount(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, DICT_TYPE)
                .eq(SysDictItem::getItemValue, categoryNo.trim())
                .ne(selfId != null, SysDictItem::getItemId, selfId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_NO_DUPLICATE, "分类编号已存在");
        }
    }
}
