package app.erp.prj.service.job;

import app.erp.prj.biz.IErpPrjProjectPnlBiz;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 项目损益汇总定时批量计算帮助类（plan 2026-08-14-2304-3 Phase 2，P1-RC-053 nop-job 调度接线）。
 *
 * <p>供 nop-batch {@code pnl-calc.batch.xml} 的 processor 按记录调用：迭代活跃
 * （DRAFT/OPEN/ON_HOLD）项目逐条刷新损益汇总（聚合 Billing 收入 + CostCollection 四类成本），
 * 经 {@link IErpPrjProjectPnlBiz#refreshPnl} 走既有 {@code ProjectPnlCalculator} 引擎。
 *
 * <p>双层门控：job 层 {@code nop.job.erp-prj-pnl-calc.enabled}（默认 false，部署 opt-in，
 * 见 job.yaml @cfg 引用）；本类消费业务 config 双键——{@code erp-prj.pnl-auto-calc-enabled}
 * （默认 false，总开关）+ {@code erp-prj.pnl-calc-cron}（默认 {@code 0 0 1 * * ?}，
 * 空值=跳过——「空值=跳过」语义与 job.yaml cronExpr @cfg 默认对齐，bank-recon helper
 * 机制开关同型范式）。
 *
 * <p>单条失败隔离：每条独立 REQUIRES_NEW 事务，失败（如项目不存在
 * {@code ERR_PROJECT_NOT_REFERENCEABLE}）记录 WARN 日志并返回 false，不阻断批次继续
 * 处理其余候选（镜像 R1.23 {@code ErpCrmLeadScoringRecalcHelper} 范式——batch chunk
 * 事务本身不提供 per-item 隔离，见 {@code BatchTaskBuilder.buildChunkProcessor} +
 * {@code InvokerBatchConsumer}）。
 *
 * <p>{@code batchChunkCtx.serviceContext} 在 nop-batch 执行路径可能为 null（job 触发经
 * {@code BatchTaskRunner.executeAsync} → {@code newBatchTaskContext()} 无绑定上下文），
 * 而 {@code IErpPrjProjectPnlBiz} 代理调用需非 null ctx（{@code EvalServiceAction.invoke} →
 * {@code context.getEvalScope()}）——本类空值兜底 {@code new ServiceContextImpl()}，
 * 对齐 R1.23 同型修复（R1.23 Follow-up 注记收口）。
 */
public class ErpPrjProjectPnlCalcHelper {

    static final Logger LOG = LoggerFactory.getLogger(ErpPrjProjectPnlCalcHelper.class);

    @Inject
    IErpPrjProjectPnlBiz pnlBiz;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    IOrmTemplate ormTemplate;

    /**
     * 业务 config 门控：{@code erp-prj.pnl-auto-calc-enabled}（默认 false）+ {@code erp-prj.pnl-calc-cron}
     * 非空（空值=禁用）同时满足才执行。
     */
    public boolean isScheduleConfigured() {
        return ErpPrjConfigs.pnlAutoCalcEnabled()
                && StringHelper.isNotEmpty(ErpPrjConfigs.pnlCalcCron());
    }

    /**
     * 刷新单项目损益汇总。config 门控不满足=跳过（INFO 日志）；
     * 单条独立 REQUIRES_NEW 事务，失败记录 WARN 日志并返回 false（单条失败隔离，批次不中断）。
     *
     * @return true=汇总成功（或 config 关闭跳过）；false=汇总失败（候选下次重试）
     */
    public boolean recalculateOne(Long projectId, IServiceContext ctx) {
        if (!isScheduleConfigured()) {
            LOG.info("erp-prj-pnl-calc-skipped-by-config: projectId={}, configKey={}",
                    projectId, ErpPrjConstants.CONFIG_PNL_AUTO_CALC_ENABLED);
            return true;
        }
        IServiceContext svcCtx = ctx != null ? ctx : new ServiceContextImpl();
        try {
            return transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        pnlBiz.refreshPnl(projectId, null, null, svcCtx);
                        session.flush();
                        return true;
                    }));
        } catch (Exception e) {
            LOG.warn("erp-prj-pnl-calc-failed: projectId={}, reason={}", projectId, e.getMessage());
            return false;
        }
    }
}
