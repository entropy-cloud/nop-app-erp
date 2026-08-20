package app.erp.ct.service.job;

import app.erp.ct.biz.IErpCtDocumentBiz;
import app.erp.ct.service.ErpCtConfigs;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 合同文档保留策略扫描 Job Bean（RC-R1.80 Phase 4，UC-CT-10 D；owner doc §文档保留策略）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code erp-ct-doc-retention.job.yaml} 的 cronExpr 决定（默认每日 02:00）。
 *
 * <p>双层门控（R1.37 范式）：① job.yaml {@code enabled} 默认 false（部署启用决策）；② 执行门控
 * cron 单键模式（{@code erp-ct.doc-retention-cron} 空值 = 「不调度」语义，bean 跳过）。行为门控：
 * 归档 {@code erp-ct.doc-auto-archive}（默认 true）；销毁 {@code erp-ct.doc-auto-purge}
 * （默认 false——需人工确认语义保持，人工通道 = {@code ErpCtDocument__purge} mutation）。
 *
 * <p>扫描经 {@link IErpCtDocumentBiz#archiveOverdueDocuments} /
 * {@code purgeOverdueDocuments} 批量入口（BizModel 内单条失败隔离 + legalHold/ACTIVE/
 * purgeDate-due 守卫短路；销毁前审计记录经 BizModel purge 内 remark + 通知）。
 * 镜像 {@code ErpCtContractExpiryJob} 范式（cron 空值跳过 + runInSession 包裹 IBiz 调用）。
 */
public class ErpCtDocRetentionJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCtDocRetentionJob.class);

    @Inject
    IErpCtDocumentBiz documentBiz;
    @Inject
    IOrmTemplate ormTemplate;

    public void setDocumentBiz(IErpCtDocumentBiz documentBiz) {
        this.documentBiz = documentBiz;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时按 config 门控执行到期归档 + 到期销毁。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-ct-doc-retention-skipped: cron config empty (erp-ct.doc-retention-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int archived = 0;
            int purged = 0;
            if (autoArchiveEnabled()) {
                archived = runArchiveScan(ctx);
            } else {
                LOG.info("erp-ct-doc-retention: doc-auto-archive=false，到期归档扫描跳过");
            }
            if (autoPurgeEnabled()) {
                purged = runPurgeScan(ctx);
            } else {
                LOG.info("erp-ct-doc-retention: doc-auto-purge=false（默认，需人工确认），到期销毁扫描跳过");
            }
            LOG.info("erp-ct-doc-retention-done: archived={}, purged={}", archived, purged);
        } catch (Exception e) {
            LOG.error("erp-ct-doc-retention-failed", e);
        }
    }

    /**
     * 批量到期归档。runInSession 原因：job 非 GraphQL 上下文，经 IBiz 代理调用时 @SingleSession
     * 不经代理生效（对齐 ErpCtContractExpiryJob.runExpirations 同型包裹）。
     */
    protected int runArchiveScan(IServiceContext ctx) {
        return ormTemplate.runInSession(session -> documentBiz.archiveOverdueDocuments(ctx));
    }

    /**
     * 批量到期销毁（仅 doc-auto-purge=true 时触达；D4 逻辑删除 + 审计）。
     */
    protected int runPurgeScan(IServiceContext ctx) {
        return ormTemplate.runInSession(session -> documentBiz.purgeOverdueDocuments(ctx));
    }

    protected boolean autoArchiveEnabled() {
        Boolean v = AppConfig.var(ErpCtConfigs.CFG_DOC_AUTO_ARCHIVE, ErpCtConfigs.DEFAULT_DOC_AUTO_ARCHIVE);
        return v == null || v;
    }

    protected boolean autoPurgeEnabled() {
        Boolean v = AppConfig.var(ErpCtConfigs.CFG_DOC_AUTO_PURGE, ErpCtConfigs.DEFAULT_DOC_AUTO_PURGE);
        return v != null && v;
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCtConfigs.CFG_DOC_RETENTION_CRON, "");
    }
}
