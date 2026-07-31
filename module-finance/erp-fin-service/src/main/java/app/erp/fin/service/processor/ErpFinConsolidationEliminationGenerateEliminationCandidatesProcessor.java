package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinConsolidationElimination;
import app.erp.fin.dao.entity.ErpFinIntercompanyMatch;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpFinConsolidationElimination generateEliminationCandidates per-mutation Processor（R6.1，
 * {@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含合并抵消候选识别编排（{@code multi-company.md §合并抵消范围}）：按 3 类（AR_AP/REVENUE_COST/INVENTORY_PROFIT）
 * 扫描配对候选。config-gated。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(
            ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    public int generateEliminationCandidates(Long periodId, IServiceContext context) {
        if (!isEliminationEnabled()) {
            return 0;
        }
        if (periodId == null) {
            return 0;
        }

        // 扫描已配对记录（MATCHED）作为 AR_AP 抵消候选
        QueryBean matchQ = new QueryBean();
        matchQ.addFilter(eq("periodId", periodId));
        matchQ.addFilter(eq("status", ErpFinConstants.INTERCOMPANY_MATCH_MATCHED));
        List<ErpFinIntercompanyMatch> matches =
                daoProvider.daoFor(ErpFinIntercompanyMatch.class).findAllByQuery(matchQ);

        int count = 0;
        IEntityDao<ErpFinConsolidationElimination> elimDao =
                daoProvider.daoFor(ErpFinConsolidationElimination.class);

        for (ErpFinIntercompanyMatch m : matches) {
            ErpFinConsolidationElimination candidate = elimDao.newEntity();
            candidate.setCode("ELIM-" + periodId + "-" + StringHelper.generateUUID().substring(0, 8));
            // 抵消候选归属 + 跨法人双方（P1-MA2-097）：移除 hardcoded orgId=1L，按配对审计列设置 from/toOrg
            candidate.setOrgId(m.getArOrgId() != null ? m.getArOrgId() : m.getApOrgId());
            candidate.setFromOrgId(m.getArOrgId());
            candidate.setToOrgId(m.getApOrgId());
            candidate.setEliminationType(ErpFinConstants.ELIMINATION_TYPE_AR_AP);
            candidate.setPeriodId(periodId);
            candidate.setPairKey(m.getPairKey());
            candidate.setMatchId(m.getId());
            candidate.setEliminationAmount(m.getMatchedAmount());
            candidate.setStatus(ErpFinConstants.ELIMINATION_STATUS_CANDIDATE);
            elimDao.saveEntity(candidate);
            count++;
        }

        // REVENUE_COST 抵消候选（简化：复用 MATCHED 记录金额作为收入/成本抵消额）
        for (ErpFinIntercompanyMatch m : matches) {
            ErpFinConsolidationElimination candidate = elimDao.newEntity();
            candidate.setCode("ELIM-RC-" + periodId + "-" + StringHelper.generateUUID().substring(0, 8));
            candidate.setOrgId(m.getArOrgId() != null ? m.getArOrgId() : m.getApOrgId());
            candidate.setFromOrgId(m.getArOrgId());
            candidate.setToOrgId(m.getApOrgId());
            candidate.setEliminationType(ErpFinConstants.ELIMINATION_TYPE_REVENUE_COST);
            candidate.setPeriodId(periodId);
            candidate.setPairKey(m.getPairKey());
            candidate.setMatchId(m.getId());
            candidate.setEliminationAmount(m.getMatchedAmount());
            candidate.setStatus(ErpFinConstants.ELIMINATION_STATUS_CANDIDATE);
            elimDao.saveEntity(candidate);
            count++;
        }

        // INVENTORY_PROFIT 试点（config-gated）
        if (isInventoryProfitEliminationEnabled()) {
            for (ErpFinIntercompanyMatch m : matches) {
                ErpFinConsolidationElimination candidate = elimDao.newEntity();
                candidate.setCode("ELIM-IP-" + periodId + "-" + StringHelper.generateUUID().substring(0, 8));
                candidate.setOrgId(m.getArOrgId() != null ? m.getArOrgId() : m.getApOrgId());
                candidate.setFromOrgId(m.getArOrgId());
                candidate.setToOrgId(m.getApOrgId());
                candidate.setEliminationType(ErpFinConstants.ELIMINATION_TYPE_INVENTORY_PROFIT);
                candidate.setPeriodId(periodId);
                candidate.setPairKey(m.getPairKey());
                candidate.setMatchId(m.getId());
                candidate.setEliminationAmount(m.getMatchedAmount());
                candidate.setStatus(ErpFinConstants.ELIMINATION_STATUS_CANDIDATE);
                elimDao.saveEntity(candidate);
                count++;
            }
        }

        if (count == 0) {
            throw new NopException(ErpFinErrors.ERR_ELIMINATION_NO_CANDIDATES)
                    .param(ErpFinErrors.ARG_PERIOD_ID, periodId);
        }

        LOG.info("抵消候选识别完成：期间 {} 识别 {} 条候选", periodId, count);
        return count;
    }

    protected boolean isEliminationEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpFinConstants.CONFIG_CONSOLIDATION_ELIMINATION_ENABLED, Boolean.FALSE));
    }

    protected boolean isInventoryProfitEliminationEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpFinConstants.CONFIG_ELIMINATION_INVENTORY_PROFIT_ENABLED, Boolean.FALSE));
    }
}
