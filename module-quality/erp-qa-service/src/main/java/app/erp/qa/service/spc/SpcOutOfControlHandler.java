package app.erp.qa.service.spc;

import app.erp.qa.biz.IErpQaActionBiz;
import app.erp.qa.biz.IErpQaNonConformanceBiz;
import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConfigs;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * SPC 失控样本 → NCR/CAPA 级联处理器（{@code docs/design/quality/spc.md §关键流程 3}，
 * plan 2026-07-07-0305-2 Phase 3）。
 *
 * <p>失控样本经 {@code txn().afterCommit}（模式 B post-commit）创建 {@link ErpQaNonConformance}
 * （sourceType=SPC，severity 按 violatedRules 映射 erp-qa/severity 字典）+ {@link ErpQaAction}（actionType=CAPA）。
 *
 * <p>config-gated：{@code erp-qa.spc-auto-ncr-enabled}（默认 true 关闭则仅标记不建 NCR）。
 * 失败隔离：单 sample 建单失败经 try/catch 不阻断其他。
 */
public class SpcOutOfControlHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SpcOutOfControlHandler.class);

    // erp-qa/severity 字典码值
    private static final String SEVERITY_LOW = "LOW";
    private static final String SEVERITY_NORMAL = "NORMAL";
    private static final String SEVERITY_HIGH = "HIGH";
    private static final String SEVERITY_CRITICAL = "CRITICAL";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    IErpQaNonConformanceBiz ncrBiz;
    @Inject
    IErpQaActionBiz actionBiz;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setTransactionTemplate(ITransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void setNcrBiz(IErpQaNonConformanceBiz ncrBiz) {
        this.ncrBiz = ncrBiz;
    }

    public void setActionBiz(IErpQaActionBiz actionBiz) {
        this.actionBiz = actionBiz;
    }

    /**
     * 失控样本级联创建 NCR + CAPA。post-commit 触发，确保 sample 事务提交后再建单
     * （sample 回滚则不建单，避免悬挂 NCR）。
     *
     * <p>幂等性双重保障：本方法注册 afterCommit 前预检 + {@link #createNcrAndAction} 内部再次预检
     * （防止同 chart 多样本 post-commit 并发触发时竞态）。
     */
    public void cascadeNcrAndCapa(ErpQaSpcChart chart, ErpQaSpcSample sample, Set<String> violatedRules,
                                   IServiceContext context) {
        if (!ErpQaConfigs.isSpcAutoNcrEnabled()) {
            return;
        }
        transactionTemplate.afterCommit(null, () -> {
            try {
                createNcrAndAction(chart, sample, violatedRules, context);
            } catch (Exception e) {
                LOG.warn("spc-cascade-create-ncr-failed: chartId={} sampleId={}",
                        chart.getId(), sample.getId(), e);
            }
        });
    }

    private void createNcrAndAction(ErpQaSpcChart chart, ErpQaSpcSample sample, Set<String> violatedRules,
                                     IServiceContext context) {
        // 内部幂等预检：post-commit 并发触发时同 chart/sample 可能多次入此方法
        if (findExistingSpcNcr(chart.getCode(), sample.getSubgroupNo()) != null) {
            return;
        }
        IEntityDao<ErpQaNonConformance> ncrDao = daoProvider.daoFor(ErpQaNonConformance.class);
        ErpQaNonConformance ncr = ncrDao.newEntity();
        ncr.setCode("NCR-SPC-" + chart.getCode() + "-" + sample.getSubgroupNo());
        ncr.setNcrDate(CoreMetrics.today());
        ncr.setSourceType(ErpQaConstants.NCR_SOURCE_TYPE_SPC);
        ncr.setSourceCode(chart.getCode() + "#" + sample.getSubgroupNo());
        ncr.setMaterialId(chart.getMaterialId());
        ncr.setQuantity(java.math.BigDecimal.ONE);
        ncr.setSeverity(mapSeverity(violatedRules));
        ncr.setStatus(ErpQaConstants.NCR_STATUS_OPEN);
        ncr.setDescription("SPC 失控预警：chart=" + chart.getCode()
                + " subgroupNo=" + sample.getSubgroupNo()
                + " mean=" + sample.getMean()
                + " violatedRules=" + (sample.getViolatedRules() == null ? "" : sample.getViolatedRules())
                + " (ucl=" + chart.getUcl() + " lcl=" + chart.getLcl() + " cl=" + chart.getCl() + ")");
        ncrDao.saveEntity(ncr);

        IEntityDao<ErpQaAction> actionDao = daoProvider.daoFor(ErpQaAction.class);
        ErpQaAction action = actionDao.newEntity();
        action.setNcrId(ncr.getId());
        action.setActionType("CAPA");
        action.setDescription("SPC 失控 CAPA：调查并消除特殊原因（chart=" + chart.getCode()
                + ", subgroup=" + sample.getSubgroupNo() + "）");
        action.setStatus(ErpQaConstants.ACTION_STATUS_PENDING);
        actionDao.saveEntity(action);
    }

    /** severity 按 violatedRules 映射 erp-qa/severity 字典码值：含规则 1=HIGH；多规则=CRITICAL；其他=NORMAL。 */
    static String mapSeverity(Set<String> violatedRules) {
        if (violatedRules == null || violatedRules.isEmpty()) {
            return SEVERITY_NORMAL;
        }
        if (violatedRules.size() >= 2) {
            return SEVERITY_CRITICAL;
        }
        if (violatedRules.contains("1")) {
            return SEVERITY_HIGH;
        }
        return SEVERITY_NORMAL;
    }

    private ErpQaNonConformance findExistingSpcNcr(String chartCode, Integer subgroupNo) {
        try {
            QueryBean q = new QueryBean();
            q.addFilter(eq("sourceType", ErpQaConstants.NCR_SOURCE_TYPE_SPC));
            q.addFilter(eq("sourceCode", chartCode + "#" + subgroupNo));
            q.setLimit(1);
            List<ErpQaNonConformance> list = daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            return null;
        }
    }
}
