<template>
  <div class="module-page">
    <el-card shadow="always">
      <div class="placeholder">
        <el-tag effect="plain" size="small">待接入</el-tag>
        <h2 class="title">领用申请与出库</h2>
        <p class="desc">该模块由领用出库子系统提供。</p>
        <ul class="feature-list">
          <li>领用申请：教师或部门按物资、数量提交领用单，填写用途与预计使用时间</li>
          <li>审批与出库：管理员审批通过后办理出库，库存不足时拦截并提示可用数量</li>
          <li>领用台账：按部门、物资、时间统计领用记录，支持导出与追溯</li>
        </ul>
        <p class="hint">
          依赖物资档案与实时库存数据，出库成功后会生成库存变动流水并同步到「库存查询」。
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
