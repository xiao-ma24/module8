<template>
  <div class="module-page">
    <el-card shadow="always">
      <div class="placeholder">
        <el-tag effect="plain" size="small">待接入</el-tag>
        <h2 class="title">盘点预警与补货</h2>
        <p class="desc">该模块由盘点预警子系统提供。</p>
        <ul class="feature-list">
          <li>盘点任务：按仓库或物资范围创建盘点单，记录账面数量与实盘数量</li>
          <li>盘盈盘亏调整：审核后生成调整流水，修正库存并保留盘点差异明细</li>
          <li>安全库存预警：低于下限或高于上限时自动预警，并给出补货建议</li>
        </ul>
        <p class="hint">
          依赖物资档案中的安全库存区间与实时库存数据，预警结果将在「库存查询」中高亮展示。
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
