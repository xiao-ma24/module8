package com.school.material.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.common.BusinessException;
import com.school.material.common.DictTypes;
import com.school.material.common.ErrorCode;
import com.school.material.common.PageResult;
import com.school.material.dto.MaterialReq;
import com.school.material.dto.MaterialVO;
import com.school.material.entity.Material;
import com.school.material.mapper.MaterialMapper;
import com.school.material.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialMapper materialMapper;

    @Override
    public PageResult<MaterialVO> page(Integer pageNum, Integer pageSize, String materialName,
                                       String categoryCode, Integer status) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        String name = StringUtils.hasText(materialName) ? materialName.trim() : null;
        String code = StringUtils.hasText(categoryCode) ? categoryCode.trim() : null;

        long total = materialMapper.countPage(name, code);
        List<MaterialVO> records = total == 0
                ? List.of()
                : materialMapper.selectPageList(name, code, DictTypes.MATERIAL_CATEGORY, (long) (num - 1) * size, size);
        return PageResult.of(total, records);
    }

    @Override
    public MaterialVO getById(Long id) {
        Material material = getEntity(id);
        MaterialVO vo = new MaterialVO();
        vo.setId(material.getId());
        vo.setMaterialNo(material.getMaterialNo());
        vo.setMaterialName(material.getMaterialName());
        vo.setCategoryCode(material.getCategoryCode());
        vo.setCategoryName(materialMapper.selectCategoryName(DictTypes.MATERIAL_CATEGORY, material.getCategoryCode()));
        vo.setMaterialType(material.getMaterialType());
        vo.setUnit(material.getUnit());
        vo.setReferencePrice(material.getReferencePrice());
        vo.setIsControlled(material.getIsControlled());
        vo.setServiceLifeYears(material.getServiceLifeYears());
        vo.setShelfLifeDays(material.getShelfLifeDays());
        vo.setSafeStock(material.getSafeStock());
        vo.setCreatedAt(material.getCreatedAt());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MaterialReq req) {
        checkCategoryExists(req.getCategoryCode());
        checkMaterialNoUnique(req.getMaterialNo(), null);

        Material entity = new Material();
        fill(entity, req);
        materialMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MaterialReq req) {
        Material entity = getEntity(id);
        checkCategoryExists(req.getCategoryCode());
        checkMaterialNoUnique(req.getMaterialNo(), id);

        fill(entity, req);
        materialMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getEntity(id);
        materialMapper.deleteById(id);
    }

    private Material getEntity(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, "物资不存在");
        }
        return material;
    }

    private void fill(Material entity, MaterialReq req) {
        int materialType = req.getMaterialType() == null ? 1 : req.getMaterialType();
        if (materialType == 2
                && (req.getServiceLifeYears() == null || req.getServiceLifeYears() < 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "低值耐用品必须填写限用年限（≥1 年）");
        }
        if (materialType == 1 && req.getShelfLifeDays() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "低值易耗品必须填写保质期天数");
        }

        entity.setMaterialNo(req.getMaterialNo().trim());
        entity.setMaterialName(req.getMaterialName().trim());
        entity.setCategoryCode(req.getCategoryCode().trim());
        entity.setMaterialType(materialType);
        entity.setUnit(req.getUnit().trim());
        entity.setReferencePrice(req.getReferencePrice());
        entity.setIsControlled(req.getIsControlled() == null ? 0 : req.getIsControlled());
        entity.setServiceLifeYears(req.getServiceLifeYears());
        entity.setShelfLifeDays(req.getShelfLifeDays());
        entity.setSafeStock(req.getSafeStock() == null ? BigDecimal.ZERO : req.getSafeStock());
    }

    private void checkCategoryExists(String categoryCode) {
        if (materialMapper.selectCategoryName(DictTypes.MATERIAL_CATEGORY, categoryCode.trim()) == null) {
            throw new BusinessException(ErrorCode.MATERIAL_CATEGORY_NOT_FOUND, "物资所属分类不存在");
        }
    }

    private void checkMaterialNoUnique(String materialNo, Long selfId) {
        Long count = materialMapper.selectCount(Wrappers.<Material>lambdaQuery()
                .eq(Material::getMaterialNo, materialNo.trim())
                .ne(selfId != null, Material::getId, selfId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.MATERIAL_NO_DUPLICATE, "物资编码已存在");
        }
    }
}
