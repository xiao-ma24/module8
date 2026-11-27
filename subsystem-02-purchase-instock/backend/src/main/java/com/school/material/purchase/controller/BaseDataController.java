package com.school.material.purchase.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.purchase.common.Result;
import com.school.material.purchase.entity.*;
import com.school.material.purchase.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 基础数据：物资目录 / 供应商 / 仓库 / 用户 / 部门 / 预算科目 / 库存查询（只读） */
@RestController
@RequestMapping("/api/v1/base")
@RequiredArgsConstructor
public class BaseDataController {

    private final MaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final WarehouseMapper warehouseMapper;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final BudgetMapper budgetMapper;
    private final StockMapper stockMapper;

    // ---- 物资目录 ----
    @GetMapping("/materials")
    public Result<List<Material>> materials(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String categoryCode) {
        return Result.ok(materialMapper.selectList(Wrappers.<Material>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(Material::getMaterialName, keyword).or().like(Material::getMaterialCode, keyword))
                .eq(categoryCode != null, Material::getCategoryCode, categoryCode)
                .orderByAsc(Material::getMaterialCode)));
    }

    @PostMapping("/materials")
    public Result<Material> addMaterial(@RequestBody Material m) {
        if (m.getSafeStock() == null) m.setSafeStock(java.math.BigDecimal.ZERO);
        if (m.getIsControlled() == null) m.setIsControlled(0);
        if (m.getMaterialType() == null) m.setMaterialType(1);
        checkLife(m);
        materialMapper.insert(m);
        return Result.ok(m);
    }

    @PutMapping("/materials/{id}")
    public Result<Void> updMaterial(@PathVariable Long id, @RequestBody Material m) {
        m.setMaterialId(id);
        if (m.getMaterialType() != null) checkLife(m);
        materialMapper.updateById(m);
        // updateById 会忽略 null，类型切换时显式清空另一侧的期限字段
        if (m.getMaterialType() != null && m.getMaterialType() == 1)
            materialMapper.update(null, Wrappers.<Material>lambdaUpdate()
                    .eq(Material::getMaterialId, id).set(Material::getServiceLifeYears, null));
        if (m.getMaterialType() != null && m.getMaterialType() == 2)
            materialMapper.update(null, Wrappers.<Material>lambdaUpdate()
                    .eq(Material::getMaterialId, id).set(Material::getShelfLifeDays, null));
        return Result.ok();
    }

    /** 数据字典表2-6：类型 2 低值耐用品时限用年限必填且 ≥1 年 */
    private void checkLife(Material m) {
        if (m.getMaterialType() != null && m.getMaterialType() == 2) {
            if (m.getServiceLifeYears() == null || m.getServiceLifeYears() < 1)
                throw new com.school.material.purchase.common.BizException(2402,
                        "低值耐用品必须填写限用年限（≥1 年）");
            m.setShelfLifeDays(null);
        } else {
            m.setServiceLifeYears(null);
        }
    }

    @DeleteMapping("/materials/{id}")
    public Result<Void> delMaterial(@PathVariable Long id) {
        materialMapper.deleteById(id);
        return Result.ok();
    }

    // ---- 供应商 ----
    @GetMapping("/suppliers")
    public Result<List<Supplier>> suppliers() {
        return Result.ok(supplierMapper.selectList(Wrappers.<Supplier>lambdaQuery()
                .orderByDesc(Supplier::getStarLevel)));
    }

    @PostMapping("/suppliers")
    public Result<Supplier> addSupplier(@RequestBody Supplier s) {
        if (s.getStarLevel() == null) s.setStarLevel(3);
        if (s.getSupplierStatus() == null) s.setSupplierStatus(1);
        supplierMapper.insert(s);
        return Result.ok(s);
    }

    @PutMapping("/suppliers/{id}")
    public Result<Void> updSupplier(@PathVariable Long id, @RequestBody Supplier s) {
        s.setSupplierId(id);
        supplierMapper.updateById(s);
        return Result.ok();
    }

    @DeleteMapping("/suppliers/{id}")
    public Result<Void> delSupplier(@PathVariable Long id) {
        supplierMapper.deleteById(id);
        return Result.ok();
    }

    // ---- 仓库 ----
    @GetMapping("/warehouses")
    public Result<List<Warehouse>> warehouses() {
        return Result.ok(warehouseMapper.selectListWithKeeper());
    }

    @PostMapping("/warehouses")
    public Result<Warehouse> addWarehouse(@RequestBody Warehouse w) {
        // 文档表2-8：默认库管员须被授予仓库管理员（STORE_KEEPER）角色
        if (w.getKeeperId() != null && userMapper.countStoreKeeper(w.getKeeperId()) == 0)
            throw new com.school.material.purchase.common.BizException(2401,
                    "所选默认库管员不具备「仓库管理员」角色，请先在平台管理端授权");
        warehouseMapper.insert(w);
        return Result.ok(w);
    }

    // ---- 用户 / 部门 / 预算（跨组引用，只读） ----
    @GetMapping("/users")
    public Result<List<User>> users(@RequestParam(required = false) String roleCode) {
        return Result.ok(userMapper.selectListWithDept().stream()
                .filter(u -> roleCode == null || roleCode.equals(u.getRoleCode()))
                .toList());
    }

    @GetMapping("/depts")
    public Result<List<Dept>> depts() {
        return Result.ok(deptMapper.selectList(null));
    }

    @GetMapping("/budgets")
    public Result<List<Budget>> budgets() {
        return Result.ok(budgetMapper.selectList(null));
    }

    // ---- 库存（只读，供前端展示与 AI 使用；写入一律走入库流程） ----
    @GetMapping("/stocks")
    public Result<List<Stock>> stocks(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Long warehouseId,
                                       @RequestParam(required = false) Boolean lowOnly) {
        return Result.ok(stockMapper.selectStockList(keyword, warehouseId,
                Boolean.TRUE.equals(lowOnly)));
    }
}
