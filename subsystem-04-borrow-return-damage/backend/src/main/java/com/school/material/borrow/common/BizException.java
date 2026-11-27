package com.school.material.borrow.common;

/** 业务异常：code 必须落在子系统③区间 4000-3999 */
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
