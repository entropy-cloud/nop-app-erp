package app.erp.crm.service.job;

import app.erp.crm.biz.IErpCrmLeadScoreBiz;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CRM 线索评分批量重算帮助类（plan 2026-08-14-1815-2 Phase 2，P1-RC-035 SCHEDULED 触发器接线）。
 *
 * <p>供 nop-batch {@code lead-scoring-recalc.batch.xml} 的 processor 按记录调用：
 * 迭代 active 线索逐条重算评分（triggerEvent=SCHEDULED），经 {@link IErpCrmLeadScoreBiz#recalculateScore}
 * 走既有评分引擎（LOOKUP/FORMULA/BOOLEAN + 归一化 + append-only + auto-qualify）。
 *
 * <p>双层门控：job 层 {@code nop.job.erp-crm-lead-scoring-recalc.enabled}（默认 false，部署 opt-in，
 * 见 job.yaml @cfg 引用）；本类消费业务 config {@code erp-crm.lead-scoring.schedule-cron}
 * （默认 {@code 0 2 * * *}，空值=跳过——「空值=跳过」语义与 L2 配置表行对齐，bank-recon helper
 * 机制开关同型范式）。
 *
 * <p>单条失败隔离：每条独立 REQUIRES_NEW 事务，失败（如评分期间配置/线索状态异常）
 * 记录 WARN 日志并返回 false，不阻断批次继续处理其余候选（镜像
 * {@code ErpFinBankReconAutoReverseHelper} 范式——batch chunk 事务本身不提供 per-item 隔离，
 * 见 {@code BatchTaskBuilder.buildChunkProcessor} + {@code InvokerBatchConsumer}）。
 *
 * <p>{@code batchChunkCtx.serviceContext} 在 nop-batch 执行路径可能为 null（job 触发经
 * {@code BatchTaskRunner.executeAsync} → {@code newBatchTaskContext()} 无绑定上下文），
 * 而 {@code IErpCrmLeadScoreBiz} 代理调用需非 null ctx（{@code EvalServiceAction.invoke} →
 * {@code context.getEvalScope()}）——本类空值兜底 {@code new ServiceContextImpl()}，
 * 对齐 R1.4/R1.5 Job bean {@code execute()} 自建 ctx 范式。
 */
public class ErpCrmLeadScoringRecalcHelper {

    static final Logger LOG = LoggerFactory.getLogger(ErpCrmLeadScoringRecalcHelper.class);

    @Inject
    IErpCrmLeadScoreBiz leadScoreBiz;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    IOrmTemplate ormTemplate;

    /**
     * 业务 config 机制开关：{@code erp-crm.lead-scoring.schedule-cron} 为空时跳过（空值=跳过语义）。
     */
    public boolean isScheduleCronConfigured() {
        return StringHelper.isNotEmpty(AppConfig.var(ErpCrmConstants.CONFIG_LEAD_SCORING_SCHEDULE_CRON, "0 2 * * *"));
    }

    /**
     * 重算单条线索评分（triggerEvent=SCHEDULED）。schedule-cron 空值=跳过（INFO 日志）；
     * 单条独立 REQUIRES_NEW 事务，失败记录 WARN 日志并返回 false（单条失败隔离，批次不中断）。
     *
     * @return true=评分成功（或 config 关闭跳过）；false=评分失败（候选下次重试）
     */
    public boolean recalculateOne(Long leadId, IServiceContext ctx) {
        if (!isScheduleCronConfigured()) {
            LOG.info("erp-crm-lead-scoring-recalc-skipped-by-config: leadId={}, configKey={}",
                    leadId, ErpCrmConstants.CONFIG_LEAD_SCORING_SCHEDULE_CRON);
            return true;
        }
        IServiceContext svcCtx = ctx != null ? ctx : new ServiceContextImpl();
        try {
            return transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        leadScoreBiz.recalculateScore(leadId, ErpCrmConstants.TRIGGER_EVENT_SCHEDULED, svcCtx);
                        session.flush();
                        return true;
                    }));
        } catch (Exception e) {
            LOG.warn("erp-crm-lead-scoring-recalc-failed: leadId={}, reason={}", leadId, e.getMessage());
            return false;
        }
    }
}
