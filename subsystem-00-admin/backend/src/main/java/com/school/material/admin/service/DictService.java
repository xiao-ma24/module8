package com.school.material.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.admin.common.BizException;
import com.school.material.admin.entity.SysDictItem;
import com.school.material.admin.entity.SysDictType;
import com.school.material.admin.mapper.SysDictItemMapper;
import com.school.material.admin.mapper.SysDictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 数据字典（全平台下拉框、状态标签的唯一来源） */
@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictTypeMapper typeMapper;
    private final SysDictItemMapper itemMapper;
    private final AuditService audit;

    public List<SysDictType> types(String keyword) {
        List<SysDictType> types = typeMapper.selectList(Wrappers.<SysDictType>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysDictType::getDictName, keyword).or().like(SysDictType::getDictType, keyword))
                .orderByAsc(SysDictType::getDictType));
        Map<String, Long> counts = itemMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(SysDictItem::getDictType, Collectors.counting()));
        types.forEach(t -> t.setItemCount(counts.getOrDefault(t.getDictType(), 0L).intValue()));
        return types;
    }

    public List<SysDictItem> items(String dictType, String keyword, Integer status) {
        return itemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(dictType != null && !dictType.isBlank(), SysDictItem::getDictType, dictType)
                .eq(status != null, SysDictItem::getItemStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysDictItem::getItemLabel, keyword).or().like(SysDictItem::getItemValue, keyword))
                .orderByAsc(SysDictItem::getDictType).orderByAsc(SysDictItem::getSortNo));
    }

    /** 一次性返回全部字典，前端可缓存为 { dictType: [{value,label,color}] } */
    public Map<String, List<Map<String, Object>>> all() {
        List<SysDictItem> items = itemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getItemStatus, 1).orderByAsc(SysDictItem::getSortNo));
        Map<String, List<Map<String, Object>>> res = new LinkedHashMap<>();
        for (SysDictItem i : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", i.getItemValue());
            m.put("label", i.getItemLabel());
            m.put("color", i.getItemColor());
            res.computeIfAbsent(i.getDictType(), k -> new ArrayList<>()).add(m);
        }
        return res;
    }

    public SysDictType saveType(SysDictType in) {
        if (in.getDictType() == null || in.getDictType().isBlank()) throw new BizException("字典编码不能为空");
        SysDictType exist = typeMapper.selectById(in.getDictType());
        if (exist == null) {
            in.setDictStatus(in.getDictStatus() == null ? 1 : in.getDictStatus());
            in.setCreatedAt(LocalDateTime.now());
            typeMapper.insert(in);
            audit.write("数据字典", "新增类型", in.getDictType(), in.getDictName());
        } else {
            typeMapper.updateById(in);
            audit.write("数据字典", "修改类型", in.getDictType(), in.getDictName(),
                    "名称=" + exist.getDictName() + "，状态=" + exist.getDictStatus(),
                    "名称=" + in.getDictName() + "，状态=" + in.getDictStatus());
        }
        return in;
    }

    @Transactional
    public void removeType(String dictType) {
        SysDictType t = typeMapper.selectById(dictType);
        if (t == null) throw new BizException("字典类型不存在");
        itemMapper.delete(Wrappers.<SysDictItem>lambdaQuery().eq(SysDictItem::getDictType, dictType));
        typeMapper.deleteById(dictType);
        audit.write("数据字典", "删除类型", dictType, "同时删除字典项");
    }

    public SysDictItem saveItem(SysDictItem in) {
        if (in.getDictType() == null || in.getDictType().isBlank()) throw new BizException("请选择字典类型");
        if (in.getItemValue() == null || in.getItemValue().isBlank()) throw new BizException("字典值不能为空");
        if (in.getItemLabel() == null || in.getItemLabel().isBlank()) throw new BizException("显示名称不能为空");
        if (in.getItemId() == null) {
            in.setItemStatus(in.getItemStatus() == null ? 1 : in.getItemStatus());
            in.setSortNo(in.getSortNo() == null ? 0 : in.getSortNo());
            itemMapper.insert(in);
            audit.write("数据字典", "新增字典项", in.getDictType(), in.getItemValue() + "=" + in.getItemLabel());
        } else {
            SysDictItem old = itemMapper.selectById(in.getItemId());
            itemMapper.updateById(in);
            audit.write("数据字典", "修改字典项", in.getDictType(), in.getItemValue() + "=" + in.getItemLabel(),
                    old == null ? null : old.getItemValue() + "=" + old.getItemLabel() + "，状态=" + old.getItemStatus(),
                    in.getItemValue() + "=" + in.getItemLabel() + "，状态=" + in.getItemStatus());
        }
        return in;
    }

    public void removeItem(Long itemId) {
        SysDictItem i = itemMapper.selectById(itemId);
        if (i == null) throw new BizException("字典项不存在");
        itemMapper.deleteById(itemId);
        audit.write("数据字典", "删除字典项", i.getDictType(), i.getItemValue());
    }
}
