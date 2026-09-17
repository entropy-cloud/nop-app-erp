package app.erp.fin.service.processor;

import app.erp.fin.dao.dto.AutoReconResult;
import app.erp.fin.dao.dto.AutoReconUnmatched;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.reconciliation.AutoReconciliationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.beans.query.QueryBean;
import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import io.nop.dao.api.IEntityDao;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * ErpFinReconciliation runAutoReconciliation per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含自动核销编排：按 partner 维度 matchAndBuild 候选行，复用 {@link ErpFinReconciliationCreateProcessor} +
 * {@link ErpFinReconciliationPostProcessor} 落核销单。共享 helper 单一真相源在
 * {@link AbstractErpFinReconciliationProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinReconciliationRunAutoReconciliationProcessor extends AbstractErpFinReconciliationProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinReconciliationRunAutoReconciliationProcessor.class);

    @Inject
    AutoReconciliationEngine autoReconciliationEngine;
    @Inject
    ErpFinReconciliationCreateProcessor createProcessor;
    @Inject
    ErpFinReconciliationPostProcessor postProcessor;

    public AutoReconResult runAutoReconciliation(String direction, String partnerId, String strategy,
                                                 IServiceContext context) {
        return runAutoReconciliation(direction, partnerId, strategy, false, context);
    }

    /**
     * P2-CK-fin2-008：skipDisabled 变体——批处理入口传 true（业务开关关闭时 skip+warn 返回空结果，
     * 不再整批抛错）；BizModel 手动路径（view.xml 自动核销按钮）走单参重载保留
     * ERR_AUTO_RECON_DISABLED 错误反馈（约束⑥：手动路径契约不回归）。
     */
    public AutoReconResult runAutoReconciliation(String direction, String partnerId, String strategy,
                                                 boolean skipDisabled, IServiceContext context) {
        if (!isAutoReconcileEnabled()) {
            if (skipDisabled) {
                LOG.warn("erp-fin-auto-recon-skipped-disabled: business switch erp-fin.auto-reconcile off");
                return new AutoReconResult();
            }
            throw new NopException(ErpFinErrors.ERR_AUTO_RECON_DISABLED);
        }
        IServiceContext ctx = context != null ? context : serviceContext();
        String effectiveStrategy = resolveStrategy(strategy);
        LocalDate businessDate = CoreMetrics.today();

        AutoReconResult result = new AutoReconResult();
        List<String> partnerIds = partnerId != null
                ? Collections.singletonList(partnerId)
                : autoReconciliationEngine.findPartnersWithOpenItems(direction, ctx);

        for (String pid : partnerIds) {
            // P2-CK-fin2-008（记录级重试）：单 partner 失败不阻断整批。
            // 会话卫生（B3/R1 修订）：create 校验失败发生在首写前（无残留）直接继续；
            // post 失败发生在核销单已 flush 落库后——补偿性删除头+行（同事务 INSERT+DELETE
            // 相抵，净效果无残留；evict 是纯 detach 无法撤回已 flush INSERT，弃）。
            try {
                AutoReconciliationEngine.MatchResult match =
                        autoReconciliationEngine.matchAndBuild(direction, pid, effectiveStrategy, ctx);
                result.getUnmatched().addAll(match.getUnmatched());
                if (match.getLines().isEmpty()) {
                    continue;
                }
                List<ReconciliationLineInput> matchLines = match.getLines();
                ErpFinReconciliation head = createProcessor.create(direction, pid, businessDate, matchLines, ctx);
                orm().flushSession();
                try {
                    postProcessor.post(head.getId(), ctx);
                    result.getReconciliationIds().add(head.getId());
                } catch (RuntimeException postErr) {
                    compensationDeleteReconciliation(head);
                    recordPartnerFailure(result, pid, direction, postErr);
                }
            } catch (RuntimeException partnerErr) {
                LOG.warn("erp-fin-auto-recon-partner-failed (skip to next): partnerId={}, direction={}, reason={}",
                        pid, direction, partnerErr.getMessage());
                recordPartnerFailure(result, pid, direction, partnerErr);
            }
        }
        return result;
    }

    /**
     * P2-CK-fin2-008：post 失败的补偿性删除——同事务删除已 flush 的核销单行与头
     * （INSERT+DELETE 相抵，净效果无残留；对齐约束⑥ deleteEntity+flushSession 配套）。
     */
    private void compensationDeleteReconciliation(ErpFinReconciliation head) {
        try {
            IEntityDao<ErpFinReconciliationLine> lineDao =
                    daoProvider.daoFor(ErpFinReconciliationLine.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("reconciliationId", head.getId()));
            for (ErpFinReconciliationLine line : lineDao.findAllByQuery(q)) {
                lineDao.deleteEntity(line);
            }
            daoProvider.daoFor(ErpFinReconciliation.class).deleteEntity(head);
            orm().flushSession();
            LOG.warn("erp-fin-auto-recon-compensation-delete-done: reconciliationId={}", head.getId());
        } catch (Exception delErr) {
            // 补偿失败：日志告警（该 partner 残留半程核销单，人工介入），不阻断整批
            LOG.error("erp-fin-auto-recon-compensation-delete-failed: reconciliationId={}, reason={}",
                    head.getId(), delErr.getMessage(), delErr);
        }
    }

    private void recordPartnerFailure(AutoReconResult result, String partnerId, String direction, RuntimeException err) {
        AutoReconUnmatched u = new AutoReconUnmatched();
        u.setPartnerId(partnerId);
        u.setDirection(direction);
        u.setUnmatchedReason("RECON_FAILED: " + err.getMessage());
        result.getUnmatched().add(u);
    }

    protected boolean isAutoReconcileEnabled() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_AUTO_RECONCILE, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    protected String resolveStrategy(String strategy) {
        if (!StringHelper.isBlank(strategy)) {
            return strategy.toUpperCase();
        }
        String s = AppConfig.var(ErpFinConstants.CONFIG_AUTO_RECON_STRATEGY,
                ErpFinConstants.AUTO_RECON_STRATEGY_FIFO);
        return !StringHelper.isBlank(s) ? s.toUpperCase() : ErpFinConstants.AUTO_RECON_STRATEGY_FIFO;
    }

    /** 当前服务上下文；无绑定（job 入口/直接 Java 调用）时兜底新建——M2.8 分片③ common-015-r3 族回填，
     * 镜像 ExpenseCostAggregator 兜底范式：优先继承调用方绑定上下文（身份/数据权限），仅无绑定时构造新上下文。 */
    private static IServiceContext serviceContext() {
        IServiceContext context = IServiceContext.getCtx();
        return context != null ? context : new ServiceContextImpl();
    }
}
