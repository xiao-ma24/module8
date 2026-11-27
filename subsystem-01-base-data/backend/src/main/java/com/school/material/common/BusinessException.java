package com.school.material.common;

import lombok.Getter;

/**
 * 业务异常：由全局异常处理器转换为统一响应体
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
