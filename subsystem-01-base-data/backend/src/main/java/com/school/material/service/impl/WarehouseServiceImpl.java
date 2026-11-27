package com.school.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.material.common.BusinessException;
import com.school.material.common.ErrorCode;
import com.school.material.common.PageResult;
import com.school.material.dto.WarehouseReq;
import com.school.material.entity.Warehouse;
import com.school.material.mapper.WarehouseMapper;
import com.school.material.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;

    @Override
    public PageResult<Warehouse> page(Integer pageNum, Integer pageSize, String warehouseName, Integer status) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        // warehouse 表无 status 列，status 参数仅为兼容旧前端保留
        LambdaQueryWrapper<Warehouse> wrapper = Wrappers.<Warehouse>lambdaQuery()
                .like(StringUtils.hasText(warehouseName), Warehouse::getWarehouseName,
                        warehouseName == null ? null : warehouseName.trim())
                .orderByDesc(Warehouse::getId);

        Page<Warehouse> page = warehouseMapper.selectPage(new Page<>(num, size), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public Warehouse getById(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND, "仓库不存在");
        }
        return warehouse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WarehouseReq req) {
        checkWarehouseNameUnique(req.getWarehouseName(), null);

        Warehouse entity = new Warehouse();
        fill(entity, req);
        warehouseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, WarehouseReq req) {
        Warehouse entity = getById(id);
        checkWarehouseNameUnique(req.getWarehouseName(), id);

        fill(entity, req);
        warehouseMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        warehouseMapper.deleteById(id);
    }

    private void fill(Warehouse entity, WarehouseReq req) {
        entity.setWarehouseName(req.getWarehouseName().trim());
        entity.setWarehouseType(req.getWarehouseType() == null ? 1 : req.getWarehouseType());
        entity.setLocation(req.getLocation());
        entity.setKeeperId(req.getKeeperId());
    }

    private void checkWarehouseNameUnique(String warehouseName, Long selfId) {
        Long count = warehouseMapper.selectCount(Wrappers.<Warehouse>lambdaQuery()
                .eq(Warehouse::getWarehouseName, warehouseName.trim())
                .ne(selfId != null, Warehouse::getId, selfId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NO_DUPLICATE, "仓库名称已存在");
        }
    }
}
