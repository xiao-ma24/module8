package com.school.material.requisition.service;

import com.school.material.requisition.dto.ReqOrderAuditReq;
import com.school.material.requisition.dto.ReqOrderSubmitReq;
import com.school.material.requisition.dto.ReqOrderVO;

import java.util.List;

public interface ReqOrderService {

    ReqOrderVO submit(ReqOrderSubmitReq req);

    ReqOrderVO audit(ReqOrderAuditReq req);

    List<ReqOrderVO> list(Integer auditStatus);
}
