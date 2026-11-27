package com.school.material.purchase.controller;

import com.school.material.purchase.common.Result;
import com.school.material.purchase.entity.Stock;
import com.school.material.purchase.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 跨子系统库存变更接口（公共契约）。
 *
 * <p>按《开发约束与规范》§5：`stock` 的唯一写入口是子系统② 的 {@link InventoryService}，
 * 子系统③（领用出库）、④（借用归还报损）、⑤（盘点）一律通过本控制器调用，禁止直连改表。</p>
 *
 * <p>所有接口均为<b>幂等</b>：`bizType + bizId + bizItemId` 唯一确定一次业务操作，
 * 重复提交会因 `stock_flow` 唯一键冲突整体回滚并返回 2339。</p>
 *
 * <p>调用方：③ 领用（reserve/release/issue）、④ 借用（borrow-out/return-good/write-off/scrap）、⑤ 盘点（profit/loss）。</p>
 */
@RestController
@RequestMapping("/api/v1/purchase/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventory;

    /** 库存变动请求体：bizId 为业务单据主键，bizItemId 为单据内明细主键（无明细则传 0）。 */
    public record MoveReq(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                          Long bizId, Long bizItemId, Long operatorId) {

        InventoryService.MoveCmd toCmd() {
            return new InventoryService.MoveCmd(materialId, warehouseId, batchNo, qty,
                    bizId, bizItemId, operatorId);
        }
    }

    /** 盘盈入库请求体 */
    public record ProfitReq(Long materialId, Long warehouseId, String batchNo,
                            BigDecimal qty, Long bizId, Long bizItemId, Long operatorId) {
    }

    /** 审批通过并预占：可用 -q、预占 +q */
    @PostMapping("/reserve")
    public Result<Void> reserve(@RequestBody MoveReq req) {
        inventory.reserve(req.toCmd());
        return Result.ok();
    }

    /** 取消/关闭释放预占：可用 +q、预占 -q */
    @PostMapping("/release")
    public Result<Void> release(@RequestBody MoveReq req) {
        inventory.releaseReserve(req.toCmd());
        return Result.ok();
    }

    /** 领用出库（消耗预占）：在库 -q、预占 -q，写 2 号领用流水 */
    @PostMapping("/issue")
    public Result<Void> issue(@RequestBody MoveReq req) {
        inventory.issueReserved(req.toCmd());
        return Result.ok();
    }

    /** 领用出库（无预占，一次性扣在库与可用）：写 2 号领用流水 */
    @PostMapping("/issue-direct")
    public Result<Void> issueDirect(@RequestBody MoveReq req) {
        inventory.issueDirect(req.toCmd());
        return Result.ok();
    }

    /** 借出：reserved=true 扣预占，false 扣可用；在库总量不变，写 3 号借用流水 */
    @PostMapping("/borrow-out")
    public Result<Void> borrowOut(@RequestBody MoveReq req,
                                  @RequestParam(defaultValue = "true") boolean reserved) {
        inventory.borrowOut(req.toCmd(), reserved);
        return Result.ok();
    }

    /** 完好归还：可用 +q，写 4 号归还流水 */
    @PostMapping("/return-good")
    public Result<Void> returnGood(@RequestBody MoveReq req) {
        inventory.returnGood(req.toCmd());
        return Result.ok();
    }

    /** 已借出物资报损核销：在库 -q，写 7 号报废核销流水 */
    @PostMapping("/write-off")
    public Result<Void> writeOff(@RequestBody MoveReq req) {
        inventory.writeOffBorrowed(req.toCmd());
        return Result.ok();
    }

    /** 库内可用物资报废核销：在库 -q、可用 -q，写 7 号报废核销流水 */
    @PostMapping("/scrap")
    public Result<Void> scrap(@RequestBody MoveReq req) {
        inventory.scrapAvailable(req.toCmd());
        return Result.ok();
    }

    /** 盘盈入库（子系统⑤） */
    @PostMapping("/profit")
    public Result<Void> profit(@RequestBody ProfitReq req) {
        inventory.stocktakeProfit(new InventoryService.InboundCmd(
                req.materialId(), req.warehouseId(), null, req.batchNo(), null, null,
                req.qty(), false, req.bizId(), req.bizItemId(), req.operatorId()));
        return Result.ok();
    }

    /** 盘亏核销（子系统⑤）：scope=AVAILABLE/LOCKED/BORROWED */
    @PostMapping("/loss")
    public Result<Void> loss(@RequestBody MoveReq req,
                             @RequestParam(defaultValue = "AVAILABLE") String scope) {
        inventory.stocktakeLoss(req.toCmd(), InventoryService.LossScope.valueOf(scope.toUpperCase()));
        return Result.ok();
    }

    /** 出库候选批次：按先到期先出返回，已排除个人保管仓与过期批次。 */
    @GetMapping("/batches")
    public Result<List<Stock>> batches(@RequestParam Long materialId,
                                       @RequestParam(required = false) Long warehouseId) {
        return Result.ok(inventory.fifoBatchesForOutbound(materialId, warehouseId));
    }

    /** 库存流水（物资目录导出用）：keyword 匹配物资编码/名称，from/to 为 ISO 时间 */
    @GetMapping("/flows")
    public Result<List<java.util.Map<String, Object>>> flows(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to) {
        return Result.ok(inventory.queryFlows(keyword, from, to));
    }
}
