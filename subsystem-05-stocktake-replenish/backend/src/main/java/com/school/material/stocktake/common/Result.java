package com.school.material.stocktake.common;

import lombok.Data;

/** 统一响应体：{ code, msg, data }，code=0 成功（与 docs/接口规范与合并指南.md §2 一致） */
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    /** 子系统⑤错误码区间 5000-5999 */
    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
