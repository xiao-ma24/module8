package com.school.material.common;

/**
 * 平台字典类型编码，对应 sys_dict_type.dict_type 与 sys_dict_item.dict_type。
 * 业务代码统一引用此处常量，避免散落的字典类型字面量。
 */
public final class DictTypes {

    /** 物资分类字典：物资表 category_code 存其 item_value，item_label 为分类名称 */
    public static final String MATERIAL_CATEGORY = "material_category";

    private DictTypes() {
    }
}
