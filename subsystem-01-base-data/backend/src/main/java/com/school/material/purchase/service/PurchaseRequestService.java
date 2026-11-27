package com.school.material.purchase.service;

import com.school.material.purchase.dto.PurchaseAuditReq;
import com.school.material.purchase.dto.PurchaseRecordVO;
import com.school.material.purchase.dto.PurchaseRegisterReq;
import com.school.material.purchase.dto.PurchaseRegisterResp;

import java.util.List;

public interface PurchaseRequestService {

    PurchaseRegisterResp register(PurchaseRegisterReq req);

    List<PurchaseRecordVO> listRecords(String registrarUserId, Integer auditStatus);

    List<PurchaseRecordVO> listPending();

    PurchaseRecordVO audit(PurchaseAuditReq req);
}
