package com.school.material.stocktake.config;

import com.school.material.stocktake.service.WarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预警扫描定时任务。
 *
 * <p>按《开发约束与规范》§5 与合并评审建议：定时任务必须可开关，
 * 合并到 material-boot 单进程后由统一调度器按配置决定是否执行，避免与其他子系统的任务抢执行。</p>
 *
 * <p>开关：{@code stocktake.scan-enabled}，缺省 true（本子系统独立运行时默认开启）；
 * 合并后建议在 material-boot 的 application.yml 中显式设为 false，由统一调度触发
 * {@code WarningService#scan()} 或直接调用 {@code POST /api/v1/stocktake/warnings/scan}。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "stocktake", name = "scan-enabled", havingValue = "true", matchIfMissing = true)
public class StocktakeScanJob {

    private final WarningService warningService;

    /** cron 由 stocktake.scan-cron 配置，默认每日 02:00 */
    @Scheduled(cron = "${stocktake.scan-cron}")
    public void scan() {
        warningService.scan();
    }
}
