package com.school.material.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一分页响应体：{ "total": 100, "list": [] }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private List<T> list;

    public static <T> PageResult<T> of(long total, List<T> list) {
        return new PageResult<>(total, list == null ? new ArrayList<>() : list);
    }
}
