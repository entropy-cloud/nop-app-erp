package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinBudgetCarryForwardLog;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.budget.ErpFinBudgetScenarioProcessor;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinAccountingPeriod、ErpFinAccountingPeriodStatus、ErpFinBudgetCarryForwardLog、ErpFinBudgetLine、ErpFinVoucher、ErpFinVoucherLine）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpFinBudgetScenario carryForward per-mutation Processor（R6.9，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>自包含预算结转编排（budget.md §结转规则引擎）。共享 protected helper 单一真相源留
 * {@link ErpFinBudgetScenarioProcessor} facade（requireScenario / save / loadBudgetLines / resolveUserId），
 * 本类按方案 A（facade-as-helper-holder）{@code @Inject} facade 调用共享 helper。下游可经 Delta beans.xml
 * 同名 bean id 覆盖本类。
 */
public class ErpFinBudgetScenarioCarryForwardProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinBudgetScenarioCarryForwardProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpFinBudgetScenarioProcessor facade;

    public ErpFinBudgetScenario carryForward(String id, String targetScenarioId, String rule, IServiceContext context) {
        validateEnabled(id);
        ErpFinBudgetScenario source = facade.requireScenario(id);
        ErpFinBudgetScenario target = facade.requireScenario(targetScenarioId);
        validateCarryForwardPreconditions(source, target);

        String actualRule = resolveRule(rule);
        Map<String, BigDecimal> aggregation = aggregateSourceAmounts(source);
        BigDecimal sourceBudget = aggregation.getOrDefault("budget", BigDecimal.ZERO);
        BigDecimal sourceActual = aggregation.getOrDefault("actual", BigDecimal.ZERO);
        // F2.3（P1-CK-fin3-002）：三量口径对齐 owner doc 与控制引擎（budget − actual − commitment）
        BigDecimal sourceCommitment = aggregateCommitment(source);
        BigDecimal sourceRemaining = sourceBudget.subtract(sourceActual).subtract(sourceCommitment);

        BigDecimal carriedAmount = computeCarriedAmount(actualRule, sourceBudget, sourceActual, sourceRemaining);

        if (carriedAmount.signum() > 0) {
            appendCarryForwardLines(source, target, actualRule, sourceRemaining, sourceActual, carriedAmount);
            writeCarryForwardVoucher(source, target, carriedAmount);
        }

        closeSourceScenario(source);
        writeCarryForwardLog(source, target, actualRule, sourceRemaining, sourceActual, carriedAmount, context);
        LOG.info("budget carry-forward: {} → {} (rule={}, sourceRemaining={}, carried={})",
                source.getCode(), target.getCode(), actualRule, sourceRemaining, carriedAmount);
        return source;
    }

    protected void validateEnabled(String id) {
        if (!isCarryForwardEnabled()) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_NOT_APPROVED)
                    .param(ErpFinErrors.ARG_SCENARIO_ID, id)
                    .param("reason", "erp-fin.budget-carry-forward-enabled=false");
        }
    }

    protected void closeSourceScenario(ErpFinBudgetScenario source) {
        source.setDocStatus(ErpFinConstants.BUDGET_STATUS_CLOSED);
        source.setClosedAt(CoreMetrics.currentTimestamp());
        facade.save(source);
    }

    protected boolean isCarryForwardEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpFinConstants.CONFIG_BUDGET_CARRY_FORWARD_ENABLED, Boolean.FALSE));
    }

    protected String resolveRule(String rule) {
        if (rule != null && !rule.isEmpty()) {
            return rule;
        }
        return AppConfig.var(ErpFinConstants.CONFIG_BUDGET_CARRY_FORWARD_DEFAULT_RULE,
                ErpFinConstants.BUDGET_CARRY_FORWARD_REMAINING_FULL);
    }

    protected void validateCarryForwardPreconditions(ErpFinBudgetScenario source, ErpFinBudgetScenario target) {
        if (!Objects.equals(source.getDocStatus(), ErpFinConstants.BUDGET_STATUS_APPROVED)) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_NOT_APPROVED)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, source.getCode())
                    .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, source.getDocStatus());
        }
        if (!Objects.equals(target.getDocStatus(), ErpFinConstants.BUDGET_STATUS_DRAFT)) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_CARRY_FORWARD_RULE_INVALID)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, source.getCode())
                    .param("targetScenarioCode", target.getCode())
                    .param("rule", "target must be DRAFT");
        }
        if (!Objects.equals(source.getOrgId(), target.getOrgId())
                || !Objects.equals(source.getAcctSchemaId(), target.getAcctSchemaId())
                || !Objects.equals(source.getCurrencyId(), target.getCurrencyId())) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_CARRY_FORWARD_RULE_INVALID)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, source.getCode())
                    .param("targetScenarioCode", target.getCode())
                    .param("rule", "cross orgId/acctSchemaId/currencyId");
        }
        // 硬前置（P1-MA2-034，budget.md §结转算法 / period-close.md §预算结转与期间状态机）：
        // 源 Scenario 所在年度的所有会计期间必须 CLOSED（glStatus=CLOSED）。
        // daoProvider 直访同模块 ErpFinAccountingPeriod/ErpFinAccountingPeriodStatus（只读聚合校验，
        // 无对应 IBiz 覆盖此跨期间查询语义）。
        if (!isSourceFiscalYearFullyClosed(source)) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_CARRY_FORWARD_RULE_INVALID)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, source.getCode())
                    .param("targetScenarioCode", target.getCode())
                    .param("rule", "source fiscalYear periods not all CLOSED")
                    .param(ErpFinErrors.ARG_YEAR, source.getFiscalYear());
        }
    }

    /**
     * 校验源 Scenario 所在年度的全部会计期间 glStatus=CLOSED（年度已结账硬前置，P1-MA2-034）。
     *
     * <p>判定口径：按 {@code source.fiscalYear} 查询全部 {@link ErpFinAccountingPeriod}，对每个期间
     * 查 {@link ErpFinAccountingPeriodStatus}（1:1，periodId 关联），要求 {@code glStatus == CLOSED}。
     * 期间无 status 行（未结账过）或 glStatus 非 CLOSED 即视为未结账。
     */
    protected boolean isSourceFiscalYearFullyClosed(ErpFinBudgetScenario source) {
        Integer fiscalYear = source.getFiscalYear();
        if (fiscalYear == null) {
            return false;
        }
        IEntityDao<ErpFinAccountingPeriod> periodDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean periodQ = new QueryBean();
        periodQ.addFilter(eq("year", fiscalYear));
        List<ErpFinAccountingPeriod> periods = periodDao.findAllByQuery(periodQ);
        if (periods.isEmpty()) {
            return false;
        }
        IEntityDao<ErpFinAccountingPeriodStatus> statusDao =
                daoProvider.daoFor(ErpFinAccountingPeriodStatus.class);
        for (ErpFinAccountingPeriod p : periods) {
            QueryBean sq = new QueryBean();
            sq.addFilter(eq("periodId", p.getId()));
            List<ErpFinAccountingPeriodStatus> statuses = statusDao.findAllByQuery(sq);
            if (statuses.isEmpty()) {
                return false;
            }
            for (ErpFinAccountingPeriodStatus s : statuses) {
                if (!Objects.equals(s.getGlStatus(), ErpFinConstants.MODULE_CLOSE_CLOSED)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 聚合源方案的预算/实际净额（按 subjectId × costCenterId 维度），用于结转计算。 */
    protected Map<String, BigDecimal> aggregateSourceAmounts(ErpFinBudgetScenario source) {
        List<ErpFinBudgetLine> lines = facade.loadBudgetLines(source.getId());
        BigDecimal budget = BigDecimal.ZERO;
        for (ErpFinBudgetLine l : lines) {
            budget = budget.add(l.getBudgetAmountFunctional() != null
                    ? l.getBudgetAmountFunctional() : BigDecimal.ZERO);
        }
        BigDecimal actual = aggregateActualForScenario(source);
        Map<String, BigDecimal> map = new HashMap<>();
        map.put("budget", budget);
        map.put("actual", actual);
        return map;
    }

    protected BigDecimal aggregateActualForScenario(ErpFinBudgetScenario source) {
        List<ErpFinBudgetLine> lines = facade.loadBudgetLines(source.getId());
        BigDecimal actual = BigDecimal.ZERO;
        for (ErpFinBudgetLine l : lines) {
            actual = actual.add(aggregateActualForLine(l));
        }
        return actual;
    }

    /** 从 ErpFinVoucherLine 聚合该 BudgetLine 维度的实际数（postingType != BUDGET/COMMITMENT）。 */
    protected BigDecimal aggregateActualForLine(ErpFinBudgetLine line) {
        if (line.getPeriodId() == null || line.getSubjectId() == null) {
            return BigDecimal.ZERO;
        }
        ErpMdSubject subject = line.getSubject();
        if (subject == null) {
            return BigDecimal.ZERO;
        }
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", line.getPeriodId()));
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        vq.addFilter(eq("isReversed", Boolean.FALSE));
        List<ErpFinVoucher> vouchers = daoProvider.daoFor(ErpFinVoucher.class).findAllByQuery(vq);
        if (vouchers.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<String> voucherIds = new ArrayList<>(vouchers.size());
        for (ErpFinVoucher v : vouchers) {
            if (ErpFinConstants.POSTING_TYPE_BUDGET.equals(v.getPostingType())
                    || ErpFinConstants.POSTING_TYPE_COMMITMENT.equals(v.getPostingType())) {
                continue;
            }
            voucherIds.add(v.getId());
        }
        if (voucherIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("subjectId", line.getSubjectId()));
        if (line.getCostCenterId() != null) {
            lq.addFilter(eq("costCenterId", line.getCostCenterId()));
        }
        // P2-CK-fin3-007（B1 修订）：voucherId 集合下推消除 O(n×m) contains
        //（期间过滤经 voucherId 集合传递——VoucherLine 无 periodId 列；分批防 in 参数上限）
        BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
        int batch = 500;
        for (int i = 0; i < voucherIds.size(); i += batch) {
            List<String> batchIds = voucherIds.subList(i, Math.min(i + batch, voucherIds.size()));
            for (ErpFinVoucherLine vl : findVoucherLinesByVoucherIds(line, batchIds)) {
                debit = debit.add(vl.getDebitAmount() != null ? vl.getDebitAmount() : BigDecimal.ZERO);
                credit = credit.add(vl.getCreditAmount() != null ? vl.getCreditAmount() : BigDecimal.ZERO);
            }
        }
        return ErpFinConstants.DC_CREDIT.equals(subject.getDirection())
                ? credit.subtract(debit) : debit.subtract(credit);
    }

    /** P2-CK-fin3-007：按 voucherId 批次查询同维度凭证行（行级条件下推）。 */
    protected List<ErpFinVoucherLine> findVoucherLinesByVoucherIds(ErpFinBudgetLine line, List<String> voucherIds) {
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("subjectId", line.getSubjectId()));
        lq.addFilter(in("voucherId", voucherIds));
        if (line.getCostCenterId() != null) {
            lq.addFilter(eq("costCenterId", line.getCostCenterId()));
        }
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lq);
    }

    protected BigDecimal computeCarriedAmount(String rule, BigDecimal budget, BigDecimal actual, BigDecimal remaining) {
        switch (rule) {
            case ErpFinConstants.BUDGET_CARRY_FORWARD_REMAINING_FULL:
                return remaining.max(BigDecimal.ZERO);
            case ErpFinConstants.BUDGET_CARRY_FORWARD_REMAINING_RATIO:
                BigDecimal ratio = AppConfig.var(ErpFinConstants.CONFIG_BUDGET_CARRY_FORWARD_RATIO,
                        ErpFinConstants.DEFAULT_BUDGET_CARRY_FORWARD_RATIO);
                return remaining.max(BigDecimal.ZERO).multiply(ratio).setScale(4, RoundingMode.HALF_UP);
            case ErpFinConstants.BUDGET_CARRY_FORWARD_USED_FULL:
                return actual.max(BigDecimal.ZERO);
            case ErpFinConstants.BUDGET_CARRY_FORWARD_NONE:
            default:
                return BigDecimal.ZERO;
        }
    }

    /** 在目标方案增补结转 BudgetLine（按源方案 subjectId × costCenterId 维度合并；简化：单行总额写入）。 */
    protected void appendCarryForwardLines(ErpFinBudgetScenario source, ErpFinBudgetScenario target,
                                           String rule, BigDecimal remaining, BigDecimal actual, BigDecimal carried) {
        IEntityDao<ErpFinBudgetLine> lineDao = daoProvider.daoFor(ErpFinBudgetLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("scenarioId", target.getId()));
        List<ErpFinBudgetLine> existing = lineDao.findAllByQuery(q);
        int maxLineNo = 0;
        for (ErpFinBudgetLine l : existing) {
            if (l.getLineNo() != null && l.getLineNo() > maxLineNo) {
                maxLineNo = l.getLineNo();
            }
        }
        // F2.3（P1-CK-fin3-001）：结转行按源方案明细行科目维度写入——修复前单行汇总且
        // subjectId 误写 source.getId()（方案实体 id 而非科目 id），结转额度永不参与预算控制/报表
        List<ErpFinBudgetLine> sourceLines = facade.loadBudgetLines(source.getId());
        int lineNo = maxLineNo;
        for (ErpFinBudgetLine sl : sourceLines) {
            if (sl.getSubjectId() == null) {
                continue;
            }
            BigDecimal lineShare = computeLineShare(sl, sourceLines, carried);
            if (lineShare.signum() <= 0) {
                continue;
            }
            ErpFinBudgetLine cl = lineDao.newEntity();
            cl.setScenarioId(target.getId());
            cl.setLineNo(++lineNo);
            cl.setOrgId(target.getOrgId());
            cl.setAcctSchemaId(target.getAcctSchemaId());
            cl.setSubjectId(sl.getSubjectId());
            cl.setSubjectCode(sl.getSubjectCode());
            cl.setPeriodId(sl.getPeriodId());
            cl.setCostCenterId(sl.getCostCenterId());
            cl.setBudgetAmountSource(lineShare);
            cl.setBudgetAmountFunctional(lineShare);
            cl.setCurrencyId(target.getCurrencyId());
            cl.setExchangeRate(BigDecimal.ONE);
            cl.setRemark("Carry-forward from " + source.getCode() + " (rule=" + rule + ")");
            lineDao.saveEntity(cl);
        }
    }

    /**
     * F2.3（P1-CK-fin3-001）：结转凭证段移除——原实现写单张 Dr=Cr 同科目凭证（净额恒 0，无信息量），
     * 且凭证行 subjectId 误用方案实体 id。结转额度经 appendCarryForwardLines 按科目维度写入
     * budget lines（POSTING_TYPE_BUDGET 通道由 getBudgetVsActual/aggregateAmount 聚合），
     * 预算控制/报表面已由行修复完整覆盖。凭证载体归 owner doc 后续裁决（若需审计轨迹再设计真实 Dr/Cr 对）。
     */
    protected void writeCarryForwardVoucher(ErpFinBudgetScenario source, ErpFinBudgetScenario target,
                                            BigDecimal carriedAmount) {
        // F2.3: 移除（见 javadoc）——保留方法签名供下游覆盖
    }

    /** 取源方案第一个 BudgetLine 的 periodId（结转凭证期间归属）。 */
    protected String resolveFirstPeriodId(ErpFinBudgetScenario source) {
        List<ErpFinBudgetLine> lines = facade.loadBudgetLines(source.getId());
        for (ErpFinBudgetLine l : lines) {
            if (l.getPeriodId() != null) {
                return l.getPeriodId();
            }
        }
        return null;
    }

    protected void writeCarryForwardLog(ErpFinBudgetScenario source, ErpFinBudgetScenario target,
                                        String rule, BigDecimal remaining, BigDecimal actual,
                                        BigDecimal carried, IServiceContext context) {
        IEntityDao<ErpFinBudgetCarryForwardLog> dao = daoProvider.daoFor(ErpFinBudgetCarryForwardLog.class);
        ErpFinBudgetCarryForwardLog log = dao.newEntity();
        log.setOrgId(source.getOrgId());
        log.setScenarioId(source.getId());
        log.setSourceScenarioId(source.getId());
        log.setTargetScenarioId(target.getId());
        log.setRule(rule);
        log.setSourceRemaining(remaining);
        log.setSourceUsed(actual);
        log.setCarriedAmount(carried);
        log.setCarriedAt(CoreMetrics.currentTimestamp());
        log.setCarriedBy(facade.resolveUserId(context));
        dao.saveEntity(log);
    }

    /** F2.3（P1-CK-fin3-002）：聚合源方案的承付款（COMMITMENT 通道凭证行，对齐 getBudgetVsActual 三通道口径）。 */
    private BigDecimal aggregateCommitment(ErpFinBudgetScenario source) {
        List<ErpFinBudgetLine> lines = facade.loadBudgetLines(source.getId());
        BigDecimal commitment = BigDecimal.ZERO;
        for (ErpFinBudgetLine l : lines) {
            if (l.getPeriodId() == null || l.getSubjectId() == null) {
                continue;
            }
            QueryBean vq = new QueryBean();
            vq.addFilter(eq("periodId", l.getPeriodId()));
            vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
            vq.addFilter(eq("isReversed", Boolean.FALSE));
            vq.addFilter(eq("postingType", ErpFinConstants.POSTING_TYPE_COMMITMENT));
            List<ErpFinVoucher> vouchers = daoProvider.daoFor(ErpFinVoucher.class).findAllByQuery(vq);
            if (vouchers.isEmpty()) {
                continue;
            }
            List<String> voucherIds = new ArrayList<>();
            for (ErpFinVoucher v : vouchers) {
                voucherIds.add(v.getId());
            }
            QueryBean lq = new QueryBean();
            lq.addFilter(io.nop.api.core.beans.FilterBeans.in("voucherId", voucherIds));
            lq.addFilter(eq("subjectId", l.getSubjectId()));
            if (l.getCostCenterId() != null) {
                lq.addFilter(eq("costCenterId", l.getCostCenterId()));
            }
            for (ErpFinVoucherLine vl : daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lq)) {
                commitment = commitment.add(vl.getAmountFunctional() != null
                        ? vl.getAmountFunctional() : BigDecimal.ZERO);
            }
        }
        return commitment;
    }

    /** F2.3（P1-CK-fin3-001）：按源行 budgetAmount 占比分摊结转额度。 */
    private BigDecimal computeLineShare(ErpFinBudgetLine sl, List<ErpFinBudgetLine> sourceLines, BigDecimal carried) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpFinBudgetLine l : sourceLines) {
            total = total.add(l.getBudgetAmountFunctional() != null ? l.getBudgetAmountFunctional() : BigDecimal.ZERO);
        }
        if (total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal own = sl.getBudgetAmountFunctional() != null ? sl.getBudgetAmountFunctional() : BigDecimal.ZERO;
        return own.multiply(carried).divide(total, 4, RoundingMode.HALF_UP);
    }
}