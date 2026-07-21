package app.erp.fin.service.budget;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinBudgetCarryForwardLog;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetRollforwardLog;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 预算方案编排 Processor（{@code processor-extension-pattern.md} Facade + Processor）。
 * Facade {@code ErpFinBudgetScenarioBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>状态机（{@code budget.md §ErpFinBudgetScenario}）：
 * <pre>
 *   DRAFT → SUBMITTED → APPROVED（生成 postingType=BUDGET 影子凭证）
 *   REJECTED → DRAFT（修改重提）
 *   SUBMITTED → REJECTED
 *   APPROVED → CANCELLED（红冲原 BUDGET 凭证）
 * </pre>
 *
 * <p>配置余地：状态机迁移（{@link #validateTransition}）、凭证生成（{@link #generateBudgetVoucher}）、
 * 凭证红冲（{@link #reverseBudgetVoucher}）均为 {@code protected} 方法、以 {@link IServiceContext} 为末参，
 * 下游可逐 step 覆盖。
 */
public class ErpFinBudgetScenarioProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinBudgetScenarioProcessor.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    BudgetVoucherGenerator budgetVoucherGenerator;

    public ErpFinBudgetScenario submit(Long id, IServiceContext context) {
        ErpFinBudgetScenario scenario = requireScenario(id);
        validateTransition(scenario, ErpFinConstants.BUDGET_STATUS_SUBMITTED,
                ErpFinConstants.BUDGET_STATUS_DRAFT, ErpFinConstants.BUDGET_STATUS_REJECTED);
        scenario.setDocStatus(ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        scenario.setApproveStatus(ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        save(scenario);
        return scenario;
    }

    public ErpFinBudgetScenario approve(Long id, IServiceContext context) {
        ErpFinBudgetScenario scenario = requireScenario(id);
        validateTransition(scenario, ErpFinConstants.BUDGET_STATUS_APPROVED,
                ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        generateBudgetVoucher(scenario, context);
        scenario.setDocStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        scenario.setApproveStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        save(scenario);
        return scenario;
    }

    public ErpFinBudgetScenario reject(Long id, IServiceContext context) {
        ErpFinBudgetScenario scenario = requireScenario(id);
        validateTransition(scenario, ErpFinConstants.BUDGET_STATUS_REJECTED,
                ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        scenario.setDocStatus(ErpFinConstants.BUDGET_STATUS_REJECTED);
        scenario.setApproveStatus(ErpFinConstants.BUDGET_STATUS_REJECTED);
        save(scenario);
        return scenario;
    }

    public ErpFinBudgetScenario cancel(Long id, IServiceContext context) {
        ErpFinBudgetScenario scenario = requireScenario(id);
        validateTransition(scenario, ErpFinConstants.BUDGET_STATUS_CANCELLED,
                ErpFinConstants.BUDGET_STATUS_APPROVED);
        reverseBudgetVoucher(scenario, context);
        scenario.setDocStatus(ErpFinConstants.BUDGET_STATUS_CANCELLED);
        save(scenario);
        return scenario;
    }

    // ==================== A2 滚动预算自动复制 + 结转规则引擎（plan 2026-07-21-1206-2） ====================

    /**
     * 滚动预算自动复制（A2，budget.md §滚动预算自动复制引擎）。按 strategy 复制源方案 BudgetLine 至 newFiscalYear 新方案。
     * periodId 按 (newFiscalYear - source.fiscalYear) 偏移重映射。写 RollforwardLog。
     */
    public ErpFinBudgetScenario rollForward(Long id, Integer newFiscalYear, String strategy, IServiceContext context) {
        if (!isRollForwardEnabled()) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_NOT_APPROVED)
                    .param(ErpFinErrors.ARG_SCENARIO_ID, id)
                    .param("reason", "erp-fin.budget-roll-forward-enabled=false");
        }
        ErpFinBudgetScenario source = requireScenario(id);
        if (!Objects.equals(source.getDocStatus(), ErpFinConstants.BUDGET_STATUS_APPROVED)) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_NOT_APPROVED)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, source.getCode())
                    .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, source.getDocStatus());
        }
        if (newFiscalYear == null || newFiscalYear <= source.getFiscalYear()) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_PERIOD_MISMATCH)
                    .param(ErpFinErrors.ARG_PERIOD_ID, newFiscalYear)
                    .param(ErpFinErrors.ARG_YEAR, source.getFiscalYear());
        }
        String actualStrategy = resolveStrategy(strategy);

        ErpFinBudgetScenario target = createRollForwardScenario(source, newFiscalYear, actualStrategy);
        BigDecimal sourceAmount = copyBudgetLinesForRollForward(source, target, actualStrategy);

        writeRollforwardLog(source, target, actualStrategy, newFiscalYear, sourceAmount, context);
        LOG.info("预算滚动复制：{} → {}（strategy={}, newFiscalYear={}, sourceAmt={})",
                source.getCode(), target.getCode(), actualStrategy, newFiscalYear, sourceAmount);
        return target;
    }

    /**
     * 预算结转（A2，budget.md §结转规则引擎）。按 rule 将源方案预算结转至目标方案。
     * 结转生成 BUDGET 凭证写入目标方案；源方案状态置 CLOSED；写 CarryForwardLog。
     */
    public ErpFinBudgetScenario carryForward(Long id, Long targetScenarioId, String rule, IServiceContext context) {
        if (!isCarryForwardEnabled()) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_NOT_APPROVED)
                    .param(ErpFinErrors.ARG_SCENARIO_ID, id)
                    .param("reason", "erp-fin.budget-carry-forward-enabled=false");
        }
        ErpFinBudgetScenario source = requireScenario(id);
        ErpFinBudgetScenario target = requireScenario(targetScenarioId);
        validateCarryForwardPreconditions(source, target);

        String actualRule = resolveRule(rule);
        Map<String, BigDecimal> aggregation = aggregateSourceAmounts(source);
        BigDecimal sourceBudget = aggregation.getOrDefault("budget", BigDecimal.ZERO);
        BigDecimal sourceActual = aggregation.getOrDefault("actual", BigDecimal.ZERO);
        BigDecimal sourceRemaining = sourceBudget.subtract(sourceActual);

        BigDecimal carriedAmount = computeCarriedAmount(actualRule, sourceBudget, sourceActual, sourceRemaining);

        if (carriedAmount.signum() > 0) {
            appendCarryForwardLines(source, target, actualRule, sourceRemaining, sourceActual, carriedAmount);
            writeCarryForwardVoucher(source, target, carriedAmount);
        }

        source.setDocStatus(ErpFinConstants.BUDGET_STATUS_CLOSED);
        source.setClosedAt(CoreMetrics.currentTimestamp());
        save(source);

        writeCarryForwardLog(source, target, actualRule, sourceRemaining, sourceActual, carriedAmount, context);
        LOG.info("预算结转：{} → {}（rule={}, sourceRemaining={}, carried={})",
                source.getCode(), target.getCode(), actualRule, sourceRemaining, carriedAmount);
        return source;
    }

    // ==================== A2 辅助方法 ====================

    private boolean isRollForwardEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpFinConstants.CONFIG_BUDGET_ROLL_FORWARD_ENABLED, Boolean.FALSE));
    }

    private boolean isCarryForwardEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpFinConstants.CONFIG_BUDGET_CARRY_FORWARD_ENABLED, Boolean.FALSE));
    }

    private String resolveStrategy(String strategy) {
        if (strategy != null && !strategy.isEmpty()) {
            return strategy;
        }
        return AppConfig.var(ErpFinConstants.CONFIG_BUDGET_ROLLFORWARD_DEFAULT_STRATEGY,
                ErpFinConstants.BUDGET_ROLLFORWARD_FIXED_PERCENTAGE);
    }

    private String resolveRule(String rule) {
        if (rule != null && !rule.isEmpty()) {
            return rule;
        }
        return AppConfig.var(ErpFinConstants.CONFIG_BUDGET_CARRY_FORWARD_DEFAULT_RULE,
                ErpFinConstants.BUDGET_CARRY_FORWARD_REMAINING_FULL);
    }

    private ErpFinBudgetScenario createRollForwardScenario(ErpFinBudgetScenario source, int newFiscalYear,
                                                           String strategy) {
        IEntityDao<ErpFinBudgetScenario> dao = daoProvider.daoFor(ErpFinBudgetScenario.class);
        ErpFinBudgetScenario target = dao.newEntity();
        target.setCode(source.getCode() + "-" + newFiscalYear);
        target.setName(source.getName() + " (Roll-Forward " + newFiscalYear + ")");
        target.setOrgId(source.getOrgId());
        target.setAcctSchemaId(source.getAcctSchemaId());
        target.setFiscalYear(newFiscalYear);
        target.setScenarioType(source.getScenarioType());
        target.setParentScenarioId(source.getId());
        target.setBudgetGroupCode(source.getBudgetGroupCode());
        target.setValidFrom(source.getValidFrom() != null
                ? source.getValidFrom().plusYears(newFiscalYear - source.getFiscalYear()) : null);
        target.setValidTo(source.getValidTo() != null
                ? source.getValidTo().plusYears(newFiscalYear - source.getFiscalYear()) : null);
        target.setCurrencyId(source.getCurrencyId());
        target.setExchangeRate(source.getExchangeRate());
        target.setControlLevel(source.getControlLevel());
        target.setDocStatus(ErpFinConstants.BUDGET_STATUS_DRAFT);
        target.setApproveStatus(ErpFinConstants.BUDGET_STATUS_DRAFT);
        target.setRollForwardStrategy(strategy);
        dao.saveEntity(target);
        return target;
    }

    private BigDecimal copyBudgetLinesForRollForward(ErpFinBudgetScenario source, ErpFinBudgetScenario target,
                                                     String strategy) {
        IEntityDao<ErpFinBudgetLine> lineDao = daoProvider.daoFor(ErpFinBudgetLine.class);
        List<ErpFinBudgetLine> sourceLines = loadBudgetLines(source.getId());
        int yearDelta = target.getFiscalYear() - source.getFiscalYear();
        BigDecimal sourceAmountSum = BigDecimal.ZERO;
        int lineNo = 1;
        for (ErpFinBudgetLine sl : sourceLines) {
            Long mappedPeriodId = remapPeriodId(sl.getPeriodId(), yearDelta);
            BigDecimal sourceAmt = sl.getBudgetAmountFunctional() != null
                    ? sl.getBudgetAmountFunctional() : BigDecimal.ZERO;
            sourceAmountSum = sourceAmountSum.add(sourceAmt);
            BigDecimal targetAmt = adjustAmountByStrategy(strategy, sourceAmt);

            ErpFinBudgetLine tl = lineDao.newEntity();
            tl.setScenarioId(target.getId());
            tl.setLineNo(lineNo++);
            tl.setOrgId(target.getOrgId());
            tl.setAcctSchemaId(target.getAcctSchemaId());
            tl.setPeriodId(mappedPeriodId);
            tl.setSubjectId(sl.getSubjectId());
            tl.setSubjectCode(sl.getSubjectCode());
            tl.setCostCenterId(sl.getCostCenterId());
            tl.setDepartmentId(sl.getDepartmentId());
            tl.setProjectId(sl.getProjectId());
            tl.setPartnerId(sl.getPartnerId());
            tl.setWarehouseId(sl.getWarehouseId());
            tl.setMaterialId(sl.getMaterialId());
            tl.setBudgetAmountSource(targetAmt);
            tl.setBudgetAmountFunctional(targetAmt);
            tl.setCurrencyId(sl.getCurrencyId());
            tl.setExchangeRate(sl.getExchangeRate());
            lineDao.saveEntity(tl);
        }
        return sourceAmountSum;
    }

    private BigDecimal adjustAmountByStrategy(String strategy, BigDecimal sourceAmount) {
        if (sourceAmount == null) {
            return BigDecimal.ZERO;
        }
        switch (strategy) {
            case ErpFinConstants.BUDGET_ROLLFORWARD_ZERO_BASED:
                return BigDecimal.ZERO;
            case ErpFinConstants.BUDGET_ROLLFORWARD_INCREMENTAL:
                BigDecimal rate = AppConfig.var(ErpFinConstants.CONFIG_BUDGET_ROLLFORWARD_INCREMENTAL_RATE,
                        ErpFinConstants.DEFAULT_BUDGET_ROLLFORWARD_INCREMENTAL_RATE);
                return sourceAmount.multiply(BigDecimal.ONE.add(rate)).setScale(4, RoundingMode.HALF_UP);
            case ErpFinConstants.BUDGET_ROLLFORWARD_FIXED_PERCENTAGE:
            default:
                return sourceAmount;
        }
    }

    private Long remapPeriodId(Long sourcePeriodId, int yearDelta) {
        if (sourcePeriodId == null || yearDelta == 0) {
            return sourcePeriodId;
        }
        ErpFinAccountingPeriod source = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(sourcePeriodId);
        if (source == null) {
            return null;
        }
        int targetYear = source.getYear() + yearDelta;
        int targetMonth = source.getMonth() != null ? source.getMonth() : 0;
        QueryBean q = new QueryBean();
        q.addFilter(eq("year", targetYear));
        if (targetMonth > 0) {
            q.addFilter(eq("month", targetMonth));
        }
        q.setLimit(1);
        List<ErpFinAccountingPeriod> list = daoProvider.daoFor(ErpFinAccountingPeriod.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    private void validateCarryForwardPreconditions(ErpFinBudgetScenario source, ErpFinBudgetScenario target) {
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
    }

    /** 聚合源方案的预算/实际净额（按 subjectId × costCenterId 维度），用于结转计算。 */
    private Map<String, BigDecimal> aggregateSourceAmounts(ErpFinBudgetScenario source) {
        List<ErpFinBudgetLine> lines = loadBudgetLines(source.getId());
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

    private BigDecimal aggregateActualForScenario(ErpFinBudgetScenario source) {
        List<ErpFinBudgetLine> lines = loadBudgetLines(source.getId());
        BigDecimal actual = BigDecimal.ZERO;
        for (ErpFinBudgetLine l : lines) {
            actual = actual.add(aggregateActualForLine(l));
        }
        return actual;
    }

    /** 从 ErpFinVoucherLine 聚合该 BudgetLine 维度的实际数（postingType != BUDGET/COMMITMENT）。 */
    private BigDecimal aggregateActualForLine(ErpFinBudgetLine line) {
        if (line.getPeriodId() == null || line.getSubjectId() == null) {
            return BigDecimal.ZERO;
        }
        ErpMdSubject subject = daoProvider.daoFor(ErpMdSubject.class).getEntityById(line.getSubjectId());
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
        List<Long> voucherIds = new ArrayList<>(vouchers.size());
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
        List<ErpFinVoucherLine> vlines = daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lq);
        BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
        for (ErpFinVoucherLine vl : vlines) {
            if (!voucherIds.contains(vl.getVoucherId())) {
                continue;
            }
            debit = debit.add(vl.getDebitAmount() != null ? vl.getDebitAmount() : BigDecimal.ZERO);
            credit = credit.add(vl.getCreditAmount() != null ? vl.getCreditAmount() : BigDecimal.ZERO);
        }
        return ErpFinConstants.DC_CREDIT.equals(subject.getDirection())
                ? credit.subtract(debit) : debit.subtract(credit);
    }

    private BigDecimal computeCarriedAmount(String rule, BigDecimal budget, BigDecimal actual, BigDecimal remaining) {
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
    private void appendCarryForwardLines(ErpFinBudgetScenario source, ErpFinBudgetScenario target,
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
        ErpFinBudgetLine cl = lineDao.newEntity();
        cl.setScenarioId(target.getId());
        cl.setLineNo(maxLineNo + 1);
        cl.setOrgId(target.getOrgId());
        cl.setAcctSchemaId(target.getAcctSchemaId());
        cl.setSubjectId(source.getId());
        cl.setSubjectCode("CARRY-FORWARD-" + source.getCode());
        cl.setBudgetAmountSource(carried);
        cl.setBudgetAmountFunctional(carried);
        cl.setCurrencyId(target.getCurrencyId());
        cl.setExchangeRate(BigDecimal.ONE);
        cl.setRemark("Carry-forward from " + source.getCode() + " (rule=" + rule + ")");
        lineDao.saveEntity(cl);
    }

    /** 结转生成 BUDGET 凭证写入目标方案（简化：单边凭证，记录结转金额；实际部署可扩展为完整 Dr/Cr 分录）。 */
    private void writeCarryForwardVoucher(ErpFinBudgetScenario source, ErpFinBudgetScenario target,
                                          BigDecimal carriedAmount) {
        if (carriedAmount == null || carriedAmount.signum() == 0) {
            return;
        }
        Long periodId = resolveFirstPeriodId(source);
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);

        ErpFinVoucher v = vDao.newEntity();
        v.setCode("CARRY-FORWARD-" + source.getCode() + "-" + target.getCode() + "-"
                + StringHelper.generateUUID().substring(0, 8));
        v.setVoucherType("TRANSFER");
        v.setPostingType(ErpFinConstants.POSTING_TYPE_BUDGET);
        v.setVoucherDate(CoreMetrics.today());
        v.setOrgId(target.getOrgId());
        v.setAcctSchemaId(target.getAcctSchemaId());
        v.setPeriodId(periodId);
        v.setTotalDebit(carriedAmount);
        v.setTotalCredit(carriedAmount);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        v.setPostedAt(CoreMetrics.currentTimestamp());
        vDao.saveEntity(v);

        ErpFinVoucherLine d = lDao.newEntity();
        d.setVoucherId(v.getId());
        d.setLineNo(1);
        d.setSubjectId(source.getId());
        d.setSubjectCode("CARRY-FORWARD-" + source.getCode());
        d.setSubjectName("预算结转");
        d.setDcDirection(ErpFinConstants.DC_DEBIT);
        d.setDebitAmount(carriedAmount);
        d.setCreditAmount(BigDecimal.ZERO);
        d.setCurrencyId(target.getCurrencyId());
        d.setExchangeRate(BigDecimal.ONE);
        d.setAmountSource(carriedAmount);
        d.setAmountFunctional(carriedAmount);
        d.setAcctSchemaId(target.getAcctSchemaId());
        d.setOrgId(target.getOrgId());
        d.setBusinessType("BUDGET_SCENARIO_CARRY_FORWARD");
        d.setMemo("预算结转：" + source.getCode() + " → " + target.getCode());
        lDao.saveEntity(d);

        ErpFinVoucherLine c = lDao.newEntity();
        c.setVoucherId(v.getId());
        c.setLineNo(2);
        c.setSubjectId(source.getId());
        c.setSubjectCode("CARRY-FORWARD-" + source.getCode());
        c.setSubjectName("预算结转");
        c.setDcDirection(ErpFinConstants.DC_CREDIT);
        c.setDebitAmount(BigDecimal.ZERO);
        c.setCreditAmount(carriedAmount);
        c.setCurrencyId(target.getCurrencyId());
        c.setExchangeRate(BigDecimal.ONE);
        c.setAmountSource(carriedAmount);
        c.setAmountFunctional(carriedAmount);
        c.setAcctSchemaId(target.getAcctSchemaId());
        c.setOrgId(target.getOrgId());
        c.setBusinessType("BUDGET_SCENARIO_CARRY_FORWARD");
        c.setMemo("预算结转：" + source.getCode() + " → " + target.getCode());
        lDao.saveEntity(c);

        ErpFinVoucherBillR billR = billRDao.newEntity();
        billR.setVoucherId(v.getId());
        billR.setBillType("BUDGET_SCENARIO_CARRY_FORWARD");
        billR.setBillCode("CARRY-FORWARD-" + source.getCode() + "-" + target.getCode());
        billR.setBusinessType("BUDGET_SCENARIO_CARRY_FORWARD");
        billRDao.saveEntity(billR);
    }

    private void writeRollforwardLog(ErpFinBudgetScenario source, ErpFinBudgetScenario target,
                                     String strategy, int newFiscalYear, BigDecimal sourceAmount,
                                     IServiceContext context) {
        IEntityDao<ErpFinBudgetRollforwardLog> dao = daoProvider.daoFor(ErpFinBudgetRollforwardLog.class);
        ErpFinBudgetRollforwardLog log = dao.newEntity();
        log.setOrgId(source.getOrgId());
        log.setScenarioId(source.getId());
        log.setSourceScenarioId(source.getId());
        log.setTargetScenarioId(target.getId());
        log.setStrategy(strategy);
        log.setNewFiscalYear(newFiscalYear);
        log.setSourceAmount(sourceAmount);
        BigDecimal targetAmount = target.getAmountFunctional() != null ? target.getAmountFunctional() : sourceAmount;
        log.setTargetAmount(targetAmount);
        log.setRolledAt(CoreMetrics.currentTimestamp());
        log.setRolledBy(resolveUserId(context));
        dao.saveEntity(log);
    }

    private void writeCarryForwardLog(ErpFinBudgetScenario source, ErpFinBudgetScenario target,
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
        log.setCarriedBy(resolveUserId(context));
        dao.saveEntity(log);
    }

    private String resolveUserId(IServiceContext context) {
        try {
            if (context != null && context.getUserContext() != null) {
                return context.getUserContext().getUserId();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 审核通过时生成 BUDGET 影子凭证；首张凭证 ID 回写方案头供审计。 */
    protected void generateBudgetVoucher(ErpFinBudgetScenario scenario, IServiceContext context) {
        List<Long> voucherIds = budgetVoucherGenerator.generate(scenario);
        if (voucherIds.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_NO_LINES)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, scenario.getCode());
        }
        scenario.setVoucherId(voucherIds.get(0));
        LOG.info("预算方案 {} 审核通过，生成 {} 张 BUDGET 凭证：{}", scenario.getCode(), voucherIds.size(), voucherIds);
    }

    /** 作废时红冲全部 BUDGET 凭证。 */
    protected void reverseBudgetVoucher(ErpFinBudgetScenario scenario, IServiceContext context) {
        List<Long> reversalIds = budgetVoucherGenerator.reverse(scenario);
        LOG.info("预算方案 {} 作废，红冲 {} 张 BUDGET 凭证：{}", scenario.getCode(), reversalIds.size(), reversalIds);
    }

    protected void validateTransition(ErpFinBudgetScenario scenario, String target, String... allowedFrom) {
        String current = scenario.getDocStatus();
        boolean ok = false;
        for (String s : allowedFrom) {
            if (Objects.equals(s, current)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, scenario.getCode())
                    .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                    .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, join(allowedFrom));
        }
    }

    protected ErpFinBudgetScenario requireScenario(Long id) {
        IEntityDao<ErpFinBudgetScenario> dao = daoProvider.daoFor(ErpFinBudgetScenario.class);
        ErpFinBudgetScenario scenario = dao.getEntityById(id);
        if (scenario == null) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION)
                    .param(ErpFinErrors.ARG_SCENARIO_ID, id);
        }
        return scenario;
    }

    protected void save(ErpFinBudgetScenario scenario) {
        daoProvider.daoFor(ErpFinBudgetScenario.class).updateEntity(scenario);
    }

    /** 加载方案的预算行（按 scenarioId 查询，返回所有未逻辑删除的行）。 */
    protected List<ErpFinBudgetLine> loadBudgetLines(Long scenarioId) {
        IEntityDao<ErpFinBudgetLine> dao = daoProvider.daoFor(ErpFinBudgetLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("scenarioId", scenarioId));
        return dao.findAllByQuery(q);
    }

    /** 取源方案第一个 BudgetLine 的 periodId（结转凭证期间归属）。 */
    private Long resolveFirstPeriodId(ErpFinBudgetScenario source) {
        List<ErpFinBudgetLine> lines = loadBudgetLines(source.getId());
        for (ErpFinBudgetLine l : lines) {
            if (l.getPeriodId() != null) {
                return l.getPeriodId();
            }
        }
        return null;
    }

    protected static String join(String[] arr) {
        return String.join("/", arr);
    }
}
