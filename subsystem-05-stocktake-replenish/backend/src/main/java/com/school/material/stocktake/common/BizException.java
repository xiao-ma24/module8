package com.school.material.stocktake.common;

/** 业务异常：code 必须落在子系统⑤区间 5000-5999 */
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
