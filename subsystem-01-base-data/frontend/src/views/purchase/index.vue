<template>
  <div class="module-page">
    <el-card shadow="always">
      <div class="placeholder">
        <el-tag effect="plain" size="small">待接入</el-tag>
        <h2 class="title">采购验收与入库</h2>
        <p class="desc">该模块由采购管理子系统提供。</p>
        <ul class="feature-list">
          <li>采购计划与采购申请：按学期/部门汇总需求，生成集体采购计划并提交审批</li>
          <li>到货验收与入库：登记供应商、到货数量与验收结论，确认后自动写入库存与库存流水</li>
          <li>采购单据归档：关联发票、验收单与入库单，支持按供应商、时间查询</li>
        </ul>
        <p class="hint">
          依赖物资档案与仓库基础数据，入库结果实时同步到「库存查询」。
          本模块处理集体采购与批量验收；教师个人科研经费的散单登记仍在「个人发票登记」办理。
        </p>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.module-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 360px;
  text-align: center;
}

.title {
  margin: 8px 0 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-1);
}

.desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-2);
}

.hint {
  max-width: 620px;
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-3);
  text-align: left;
}

.feature-list {
  max-width: 620px;
  margin: 4px 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-2);
  text-align: left;
}

.feature-list li::marker {
  color: var(--primary);
}
</style>
