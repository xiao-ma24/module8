<template>
  <div class="module-page">
    <el-card shadow="always">
      <div class="placeholder">
        <el-tag effect="plain" size="small">待接入</el-tag>
        <h2 class="title">借用归还与报损</h2>
        <p class="desc">该模块由借用报损子系统提供。</p>
        <ul class="feature-list">
          <li>借用登记：记录借用人、借用数量、借用期限与用途，生成借用单</li>
          <li>归还与逾期提醒：到期前提醒归还，归还时核销数量并更新借用状态</li>
          <li>报损处理：登记损坏或丢失情况，经审批后扣减库存并保留处理记录</li>
        </ul>
        <p class="hint">
          依赖物资档案与库存数据；借用、归还、报损都会形成库存变动流水，可追溯到具体单据。
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
