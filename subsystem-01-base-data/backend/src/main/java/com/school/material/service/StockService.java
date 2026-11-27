package com.school.material.service;

import com.school.material.common.PageResult;
import com.school.material.dto.StockChangeReq;
import com.school.material.dto.StockChangeResp;
import com.school.material.dto.StockVO;

import java.math.BigDecimal;

public interface StockService {

    /** 库存分页查询（关联物资、仓库） */
    PageResult<StockVO> page(Integer pageNum, Integer pageSize, Long materialId, Long warehouseId);

    /** 手工库存变更（IN/OUT），以 sourceNo 作为幂等键 */
    StockChangeResp change(StockChangeReq req);

    /**
     * 内部库存变更：写 stock_flow 并原子更新 stock。
     *
     * @param num        变动量：入库同时增加 qty_on_hand/qty_available，出库同时扣减
     * @param bizType    业务类型：1入库 2领用
     * @param bizId      来源单据 id
     * @param bizItemId  来源单据明细 id，无明细填 0
     */
    StockChangeResp applyChange(Long materialId, Long warehouseId, String batchNo, String direction,
                                BigDecimal num, Integer bizType, Long bizId, Long bizItemId,
                                Long operatorId);

    /**
     * 个人保管入库（个人经费自购）：只增加在库量，可用量保持 0，不进入公共领用口径。
     * 按保管人拆分批次，保证同一仓库下不同教师的个人物资互不混合。
     */
    StockChangeResp personalInbound(Long materialId, Long warehouseId, Long holderId, BigDecimal num,
                                    Integer bizType, Long bizId, Long bizItemId, Long operatorId);

    /** 某物资在某仓库的在库总量（跨批次合计 qty_on_hand） */
    BigDecimal currentQtyOnHand(Long materialId, Long warehouseId);
}
