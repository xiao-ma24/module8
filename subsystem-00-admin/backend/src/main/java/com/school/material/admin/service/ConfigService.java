package com.school.material.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.admin.common.BizException;
import com.school.material.admin.entity.SysConfig;
import com.school.material.admin.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 系统参数（平台级开关，各子系统读取同一份配置） */
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SysConfigMapper configMapper;
    private final AuditService audit;

    public List<SysConfig> list(String group, String keyword) {
        return configMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                .eq(group != null && !group.isBlank(), SysConfig::getConfigGroup, group)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysConfig::getConfigKey, keyword).or().like(SysConfig::getConfigName, keyword))
                .orderByAsc(SysConfig::getConfigGroup).orderByAsc(SysConfig::getConfigKey));
    }

    public void updateValue(Long configId, String value) {
        SysConfig old = configMapper.selectById(configId);
        if (old == null) throw new BizException("参数不存在");
        validate(old.getValueType(), value);
        SysConfig upd = new SysConfig();
        upd.setConfigId(configId);
        upd.setConfigValue(value);
        configMapper.updateById(upd);
        audit.write("系统参数", "修改", old.getConfigKey(), old.getConfigValue() + " → " + value,
                old.getConfigValue(), value);
    }

    public SysConfig save(SysConfig in) {
        if (in.getConfigKey() == null || in.getConfigKey().isBlank()) throw new BizException("参数键不能为空");
        Long dup = configMapper.selectCount(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, in.getConfigKey())
                .ne(in.getConfigId() != null, SysConfig::getConfigId, in.getConfigId()));
        if (dup != null && dup > 0) throw new BizException("参数键已存在：" + in.getConfigKey());
        validate(in.getValueType(), in.getConfigValue());
        if (in.getConfigId() == null) {
            in.setBuiltin(0);
            in.setConfigGroup(in.getConfigGroup() == null ? "自定义" : in.getConfigGroup());
            configMapper.insert(in);
            audit.write("系统参数", "新增", in.getConfigKey(), in.getConfigValue());
        } else {
            configMapper.updateById(in);
            audit.write("系统参数", "修改", in.getConfigKey(), in.getConfigValue());
        }
        return in;
    }

    public void remove(Long configId) {
        SysConfig c = configMapper.selectById(configId);
        if (c == null) throw new BizException("参数不存在");
        if (c.getBuiltin() != null && c.getBuiltin() == 1) throw new BizException("内置参数不可删除");
        configMapper.deleteById(configId);
        audit.write("系统参数", "删除", c.getConfigKey(), c.getConfigValue());
    }

    private void validate(String type, String value) {
        if (type == null) return;
        switch (type) {
            case "number" -> {
                try {
                    new java.math.BigDecimal(value);
                } catch (Exception e) {
                    throw new BizException("参数值必须是数字：" + value);
                }
            }
            case "boolean" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new BizException("参数值必须是 true / false");
                }
            }
            default -> {
            }
        }
    }
}
