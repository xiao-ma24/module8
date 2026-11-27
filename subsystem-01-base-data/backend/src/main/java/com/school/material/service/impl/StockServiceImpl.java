package com.school.material.service.impl;

import com.school.material.common.BusinessException;
import com.school.material.common.ErrorCode;
import com.school.material.common.PageResult;
import com.school.material.dto.StockChangeReq;
import com.school.material.dto.StockChangeResp;
import com.school.material.dto.StockVO;
import com.school.material.entity.Stock;
import com.school.material.entity.StockFlow;
import com.school.material.mapper.MaterialMapper;
import com.school.material.mapper.StockFlowMapper;
import com.school.material.mapper.StockMapper;
import com.school.material.mapper.WarehouseMapper;
import com.school.material.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private static final String DIRECTION_IN = "IN";
    private static final String DIRECTION_OUT = "OUT";

    private static final int BIZ_TYPE_IN = 1;
    private static final int BIZ_TYPE_OUT = 2;

    /** stock 联合主键中的批次号：发票登记入库不暴露批次，统一落到 DEFAULT */
    private static final String DEFAULT_BATCH = "DEFAULT";

    private final StockMapper stockMapper;
    private final StockFlowMapper stockFlowMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;

    @Override
    public PageResult<StockVO> page(Integer pageNum, Integer pageSize, Long materialId, Long warehouseId) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        long total = stockMapper.countPage(materialId, warehouseId);
        List<StockVO> records = total == 0
                ? List.of()
                : stockMapper.selectPageList(materialId, warehouseId, (long) (num - 1) * size, size);
        return PageResult.of(total, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockChangeResp change(StockChangeReq req) {
        String direction = normalizeDirection(req.getDirection());
        BigDecimal num = checkQty(req.getNum());
        String sourceNo = req.getSourceNo().trim();

        int bizType = DIRECTION_IN.equals(direction) ? BIZ_TYPE_IN : BIZ_TYPE_OUT;
        long bizId = crc32(sourceNo);

        StockChangeResp resp = applyChange(req.getMaterialId(), req.getWarehouseId(), req.getBatchNo(),
                direction, num, bizType, bizId, 0L, 0L);
        resp.setSourceNo(sourceNo);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockChangeResp applyChange(Long materialId, Long warehouseId, String batchNo, String direction,
                                       BigDecimal num, Integer bizType, Long bizId, Long bizItemId,
                                       Long operatorId) {
        String dir = normalizeDirection(direction);
        BigDecimal qty = checkQty(num);
        if (bizType == null || (bizType != BIZ_TYPE_IN && bizType != BIZ_TYPE_OUT)) {
            throw new BusinessException(ErrorCode.STOCK_DIRECTION_INVALID, "业务类型只能是 1（入库）或 2（出库）");
        }
        if (materialMapper.selectById(materialId) == null) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, "物资不存在");
        }
        if (warehouseMapper.selectById(warehouseId) == null) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND, "仓库不存在");
        }

        String batch = resolveBatch(materialId, warehouseId, batchNo, dir, qty);

        StockFlow flow = new StockFlow();
        flow.setMaterialId(materialId);
        flow.setWarehouseId(warehouseId);
        flow.setBatchNo(batch);
        flow.setBizType(bizType);
        flow.setBizId(bizId);
        flow.setBizItemId(bizItemId == null ? 0L : bizItemId);
        flow.setChangeQty(DIRECTION_IN.equals(dir) ? qty : qty.negate());
        flow.setOperatorId(operatorId == null ? 0L : operatorId);
        flow.setFlowTime(LocalDateTime.now());
        try {
            stockFlowMapper.insert(flow);
        } catch (DuplicateKeyException e) {
            Stock latest = stockMapper.selectBatch(materialId, warehouseId, batch);
            return buildResp(materialId, warehouseId, dir, qty, latest, true);
        }

        if (DIRECTION_IN.equals(dir)) {
            doIncrease(materialId, warehouseId, batch, qty);
        } else {
            int rows = stockMapper.decreaseBatch(materialId, warehouseId, batch, qty);
            if (rows == 0) {
                Stock latest = stockMapper.selectBatch(materialId, warehouseId, batch);
                BigDecimal available = latest == null ? BigDecimal.ZERO : latest.getQtyAvailable();
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH,
                        "库存不足，当前可用量 " + available.toPlainString());
            }
        }

        Stock latest = stockMapper.selectBatch(materialId, warehouseId, batch);
        return buildResp(materialId, warehouseId, dir, qty, latest, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockChangeResp personalInbound(Long materialId, Long warehouseId, Long holderId, BigDecimal num,
                                           Integer bizType, Long bizId, Long bizItemId, Long operatorId) {
        if (holderId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "个人保管入库必须指定保管人");
        }
        BigDecimal qty = checkQty(num);
        if (bizType == null || bizType != BIZ_TYPE_IN) {
            throw new BusinessException(ErrorCode.STOCK_DIRECTION_INVALID, "个人保管入库业务类型只能为 1（入库）");
        }
        if (materialMapper.selectById(materialId) == null) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, "物资不存在");
        }
        if (warehouseMapper.selectById(warehouseId) == null) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND, "仓库不存在");
        }

        // 按保管人拆批次：同一仓库下不同教师的个人物资互不混合，也不会与公共批次串量
        String batch = "PER-" + holderId;

        StockFlow flow = new StockFlow();
        flow.setMaterialId(materialId);
        flow.setWarehouseId(warehouseId);
        flow.setBatchNo(batch);
        flow.setBizType(bizType);
        flow.setBizId(bizId);
        flow.setBizItemId(bizItemId == null ? 0L : bizItemId);
        flow.setChangeQty(qty);
        flow.setOperatorId(operatorId == null ? 0L : operatorId);
        flow.setFlowTime(LocalDateTime.now());
        try {
            stockFlowMapper.insert(flow);
        } catch (DuplicateKeyException e) {
            Stock latest = stockMapper.selectBatch(materialId, warehouseId, batch);
            return buildResp(materialId, warehouseId, DIRECTION_IN, qty, latest, true);
        }

        int rows = stockMapper.increasePersonalBatch(materialId, warehouseId, batch, holderId, qty);
        if (rows == 0) {
            Stock stock = new Stock();
            stock.setMaterialId(materialId);
            stock.setWarehouseId(warehouseId);
            stock.setHolderId(holderId);
            stock.setBatchNo(batch);
            stock.setInboundDate(LocalDate.now());
            stock.setQtyOnHand(qty);
            stock.setQtyAvailable(BigDecimal.ZERO);
            stock.setQtyLocked(BigDecimal.ZERO);
            try {
                stockMapper.insertPersonalBatch(stock);
            } catch (DuplicateKeyException e) {
                if (stockMapper.increasePersonalBatch(materialId, warehouseId, batch, holderId, qty) == 0) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "个人保管库存更新失败，请重试");
                }
            }
        }

        Stock latest = stockMapper.selectBatch(materialId, warehouseId, batch);
        return buildResp(materialId, warehouseId, DIRECTION_IN, qty, latest, false);
    }

    @Override
    public BigDecimal currentQtyOnHand(Long materialId, Long warehouseId) {
        BigDecimal total = stockMapper.sumQtyOnHand(materialId, warehouseId);
        return total == null ? BigDecimal.ZERO : total;
    }

    private void doIncrease(Long materialId, Long warehouseId, String batch, BigDecimal qty) {
        int rows = stockMapper.increaseBatch(materialId, warehouseId, batch, qty);
        if (rows > 0) {
            return;
        }
        Stock stock = new Stock();
        stock.setMaterialId(materialId);
        stock.setWarehouseId(warehouseId);
        stock.setBatchNo(batch);
        stock.setInboundDate(LocalDate.now());
        stock.setQtyOnHand(qty);
        stock.setQtyAvailable(qty);
        stock.setQtyLocked(BigDecimal.ZERO);
        try {
            stockMapper.insertBatch(stock);
        } catch (DuplicateKeyException e) {
            if (stockMapper.increaseBatch(materialId, warehouseId, batch, qty) == 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "库存更新失败，请重试");
            }
        }
    }

    private String resolveBatch(Long materialId, Long warehouseId, String batchNo,
                                String direction, BigDecimal num) {
        String requested = StringUtils.hasText(batchNo) ? batchNo.trim() : null;
        if (DIRECTION_IN.equals(direction)) {
            // 入库：前端不暴露批次，缺省固定 DEFAULT
            return requested == null ? DEFAULT_BATCH : requested;
        }
        // 出库：显式批次优先；未指定或 DEFAULT 时按先进先出挑选有可用量的批次
        if (requested != null && !DEFAULT_BATCH.equalsIgnoreCase(requested)) {
            return requested;
        }
        String available = stockMapper.selectAvailableBatch(materialId, warehouseId, num);
        return available == null ? DEFAULT_BATCH : available;
    }

    private String normalizeDirection(String direction) {
        String dir = direction == null ? "" : direction.trim().toUpperCase(Locale.ROOT);
        if (!DIRECTION_IN.equals(dir) && !DIRECTION_OUT.equals(dir)) {
            throw new BusinessException(ErrorCode.STOCK_DIRECTION_INVALID, "库存变更方向只能是 IN 或 OUT");
        }
        return dir;
    }

    private BigDecimal checkQty(BigDecimal num) {
        if (num == null || num.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.STOCK_QUANTITY_INVALID, "变更数量必须大于 0");
        }
        return num;
    }

    private long crc32(String value) {
        CRC32 crc = new CRC32();
        crc.update(value.getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }

    private StockChangeResp buildResp(Long materialId, Long warehouseId, String direction,
                                      BigDecimal changeQty, Stock latest, boolean duplicated) {
        StockChangeResp resp = new StockChangeResp();
        resp.setMaterialId(materialId);
        resp.setWarehouseId(warehouseId);
        resp.setDirection(direction);
        resp.setChangeQty(changeQty);
        resp.setQtyOnHand(latest == null ? BigDecimal.ZERO : latest.getQtyOnHand());
        resp.setQtyAvailable(latest == null ? BigDecimal.ZERO : latest.getQtyAvailable());
        resp.setDuplicated(duplicated);
        return resp;
    }
}
