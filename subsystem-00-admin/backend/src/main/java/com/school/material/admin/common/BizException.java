package com.school.material.admin.common;

import lombok.Getter;

/** 业务异常：code 见 Result 注释 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(String msg) {
        this(1000, msg);
    }

    public static BizException of(String msg) {
        return new BizException(msg);
    }
}
