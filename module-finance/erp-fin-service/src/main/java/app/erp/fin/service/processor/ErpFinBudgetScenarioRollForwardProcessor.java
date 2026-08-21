package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetRollforwardLog;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.budget.ErpFinBudgetScenarioProcessor;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpFinBudgetScenario rollForward per-mutation Processor（R6.9，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>自包含滚动预算自动复制编排（budget.md §滚动预算自动复制引擎）。共享 protected helper 单一真相源留
 * {@link ErpFinBudgetScenarioProcessor} facade（requireScenario / loadBudgetLines / resolveUserId），
 * 本类按方案 A（facade-as-helper-holder）{@code @Inject} facade 调用共享 helper。下游可经 Delta beans.xml
 * 同名 bean id 覆盖本类。
 */
public class ErpFinBudgetScenarioRollForwardProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinBudgetScenarioRollForwardProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpFinBudgetScenarioProcessor facade;

    public ErpFinBudgetScenario rollForward(String id, Integer newFiscalYear, String strategy, IServiceContext context) {
        validateEnabled(id);
        ErpFinBudgetScenario source = facade.requireScenario(id);
        validateApproved(source);
        validateNewFiscalYear(source, newFiscalYear);
        String actualStrategy = resolveStrategy(strategy);

        ErpFinBudgetScenario target = createRollForwardScenario(source, newFiscalYear, actualStrategy);
        BigDecimal sourceAmount = copyBudgetLinesForRollForward(source, target, actualStrategy);

        writeRollforwardLog(source, target, actualStrategy, newFiscalYear, sourceAmount, context);
        LOG.info("预算滚动复制：{} → {}（strategy={}, newFiscalYear={}, sourceAmt={})",
                source.getCode(), target.getCode(), actualStrategy, newFiscalYear, sourceAmount);
        return target;
    }

    protected void validateEnabled(String id) {
        if (!isRollForwardEnabled()) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_NOT_APPROVED)
                    .param(ErpFinErrors.ARG_SCENARIO_ID, id)
                    .param("reason", "erp-fin.budget-roll-forward-enabled=false");
        }
    }

    protected void validateApproved(ErpFinBudgetScenario source) {
        if (!Objects.equals(source.getDocStatus(), ErpFinConstants.BUDGET_STATUS_APPROVED)) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_NOT_APPROVED)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, source.getCode())
                    .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, source.getDocStatus());
        }
    }

    protected void validateNewFiscalYear(ErpFinBudgetScenario source, Integer newFiscalYear) {
        if (newFiscalYear == null || newFiscalYear <= source.getFiscalYear()) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_PERIOD_MISMATCH)
                    .param(ErpFinErrors.ARG_PERIOD_ID, newFiscalYear)
                    .param(ErpFinErrors.ARG_YEAR, source.getFiscalYear());
        }
    }

    protected boolean isRollForwardEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpFinConstants.CONFIG_BUDGET_ROLL_FORWARD_ENABLED, Boolean.FALSE));
    }

    protected String resolveStrategy(String strategy) {
        if (strategy != null && !strategy.isEmpty()) {
            return strategy;
        }
        return AppConfig.var(ErpFinConstants.CONFIG_BUDGET_ROLLFORWARD_DEFAULT_STRATEGY,
                ErpFinConstants.BUDGET_ROLLFORWARD_FIXED_PERCENTAGE);
    }

    protected ErpFinBudgetScenario createRollForwardScenario(ErpFinBudgetScenario source, int newFiscalYear,
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

    protected BigDecimal copyBudgetLinesForRollForward(ErpFinBudgetScenario source, ErpFinBudgetScenario target,
                                                        String strategy) {
        IEntityDao<ErpFinBudgetLine> lineDao = daoProvider.daoFor(ErpFinBudgetLine.class);
        List<ErpFinBudgetLine> sourceLines = facade.loadBudgetLines(source.getId());
        int yearDelta = target.getFiscalYear() - source.getFiscalYear();
        BigDecimal sourceAmountSum = BigDecimal.ZERO;
        int lineNo = 1;
        for (ErpFinBudgetLine sl : sourceLines) {
            String mappedPeriodId = remapPeriodId(sl.getPeriodId(), yearDelta);
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

    protected BigDecimal adjustAmountByStrategy(String strategy, BigDecimal sourceAmount) {
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

    protected String remapPeriodId(String sourcePeriodId, int yearDelta) {
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

    protected void writeRollforwardLog(ErpFinBudgetScenario source, ErpFinBudgetScenario target,
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
        log.setRolledBy(facade.resolveUserId(context));
        dao.saveEntity(log);
    }
}
