package com.school.material.borrow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.borrow.entity.Material;
import com.school.material.borrow.entity.Stock;
import com.school.material.borrow.entity.Warehouse;
import com.school.material.borrow.mapper.MaterialMapper;
import com.school.material.borrow.mapper.StockMapper;
import com.school.material.borrow.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** 基础数据只读服务：物资目录、仓库、库存与出库候选批次 */
@Service
@RequiredArgsConstructor
public class MetaService {

    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final StockMapper stockMapper;

    /** 借用只允许选低值耐用品（material_type=2）；低值易耗品请走领用 */
    public List<Material> materials(String keyword) {
        return materialMapper.selectMaterials(keyword, 2);
    }

    /** 仅实体仓库参与公共借用出库（虚拟个人保管仓 warehouse_type=2 不参与） */
    public List<Warehouse> warehouses() {
        return warehouseMapper.selectList(Wrappers.<Warehouse>lambdaQuery()
                .eq(Warehouse::getWarehouseType, 1)
                .orderByAsc(Warehouse::getWarehouseId));
    }

    public List<Stock> stocks(String keyword) {
        return stockMapper.selectStockOverview(keyword);
    }

    public BigDecimal available(Long materialId) {
        BigDecimal v = stockMapper.sumAvailable(materialId);
        return v == null ? BigDecimal.ZERO : v;
    }

    public List<Stock> batches(Long materialId, Long warehouseId) {
        return stockMapper.selectFifoBatches(materialId, warehouseId);
    }
}
