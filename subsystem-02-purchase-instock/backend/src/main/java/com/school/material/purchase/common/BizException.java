package com.school.material.purchase.common;

/** 业务异常：code 必须落在子系统②区间 2000-2999 */
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
