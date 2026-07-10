package app.erp.fin.service.budget;

import app.erp.fin.biz.IErpFinBudgetControlBiz;
import app.erp.fin.dao.dto.BudgetCheckResult;
import app.erp.fin.dao.entity.ErpFinBudgetControlLog;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdSubject;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.ne;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 预算控制实现（{@code budget.md §业务规则2/4/8}）。在采购/付款/报销审核事务内同步校验预算余量。
 *
 * <p><b>余量计算</b>（均从 {@link ErpFinVoucherLine} 聚合，不写 GlBalance）：
 * <ul>
 *   <li>{@code budgetBalance} = 关联凭证 {@code postingType=BUDGET} 的行在匹配维度（subjectId + costCenterId + periodId）的净额</li>
 *   <li>{@code actualBalance} = 关联凭证 {@code postingType=NORMAL}（含 NULL）的行在匹配维度的净额</li>
 *   <li>{@code availableAmount} = budgetBalance − actualBalance</li>
 * </ul>
 *
 * <p><b>控制级别</b>（来自命中的 APPROVED 预算方案 {@link ErpFinBudgetScenario#getControlLevel()}）：
 * NONE → PASS；WARN → 不足时 WARNED 写日志放行；HARD → 不足时 BLOCKED 抛异常。
 *
 * <p>控制开关 {@code erp-fin.budget-check-enabled}（默认 false，向后兼容）。
 */
public class ErpFinBudgetControlBiz implements IErpFinBudgetControlBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinBudgetControlBiz.class);

    @Inject
    IDaoProvider daoProvider;

    @Override
    public BudgetCheckResult check(Long subjectId, Long costCenterId, Long periodId, BigDecimal amount,
                                   String sourceBillType, String sourceBillCode, IServiceContext context) {
        if (!isBudgetCheckEnabled()) {
            return new BudgetCheckResult(BudgetCheckResult.ACTION_PASS, BigDecimal.ZERO, null);
        }
        if (subjectId == null || amount == null || amount.signum() <= 0) {
            return new BudgetCheckResult(BudgetCheckResult.ACTION_PASS, BigDecimal.ZERO, null);
        }

        BudgetLineMatch match = findMatchingBudgetLine(subjectId, costCenterId, periodId);
        if (match == null) {
            return new BudgetCheckResult(BudgetCheckResult.ACTION_PASS, BigDecimal.ZERO, null);
        }

        ErpMdSubject subject = loadSubject(subjectId);
        String direction = subject != null ? subject.getDirection() : ErpFinConstants.DC_DEBIT;
        BigDecimal budgetBalance = aggregateAmount(periodId, subjectId, costCenterId, direction, true);
        BigDecimal actualBalance = aggregateAmount(periodId, subjectId, costCenterId, direction, false);
        BigDecimal available = budgetBalance.subtract(actualBalance);

        if (available.compareTo(amount) >= 0) {
            return new BudgetCheckResult(BudgetCheckResult.ACTION_PASS, available, match.line.getId());
        }

        String controlLevel = match.scenario.getControlLevel();
        if (ErpFinConstants.BUDGET_CONTROL_HARD.equals(controlLevel)) {
            writeControlLog(match, periodId, sourceBillType, sourceBillCode, amount, available,
                    BudgetCheckResult.ACTION_BLOCKED, context);
            throw new NopException(ErpFinErrors.ERR_BUDGET_EXCEEDED)
                    .param(ErpFinErrors.ARG_SUBJECT_ID, subjectId)
                    .param(ErpFinErrors.ARG_AVAILABLE_AMOUNT, available)
                    .param(ErpFinErrors.ARG_REQUESTED_AMOUNT, amount);
        }
        if (ErpFinConstants.BUDGET_CONTROL_WARN.equals(controlLevel)) {
            writeControlLog(match, periodId, sourceBillType, sourceBillCode, amount, available,
                    BudgetCheckResult.ACTION_WARNED, context);
            LOG.warn("预算告警放行：单据 {}/{} 科目 {} 申请 {} 余量 {}（WARN 模式）",
                    sourceBillType, sourceBillCode, subjectId, amount, available);
            return new BudgetCheckResult(BudgetCheckResult.ACTION_WARNED, available, match.line.getId());
        }
        return new BudgetCheckResult(BudgetCheckResult.ACTION_PASS, available, match.line.getId());
    }

    /** 聚合指定维度的净额。budget=true 取 BUDGET 凭证行；budget=false 取实际凭证行（NORMAL/NULL）。 */
    private BigDecimal aggregateAmount(Long periodId, Long subjectId, Long costCenterId, String direction, boolean budget) {
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean vq = new QueryBean();
        if (periodId != null) {
            vq.addFilter(eq("periodId", periodId));
        }
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        vq.addFilter(eq("isReversed", Boolean.FALSE));
        if (budget) {
            vq.addFilter(eq("postingType", ErpFinConstants.POSTING_TYPE_BUDGET));
        } else {
            vq.addFilter(or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET)));
        }
        List<Long> voucherIds = voucherDao.findAllByQuery(vq).stream()
                .map(ErpFinVoucher::getId).collect(java.util.stream.Collectors.toList());
        if (voucherIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(in("voucherId", voucherIds));
        lq.addFilter(eq("subjectId", subjectId));
        if (costCenterId != null) {
            lq.addFilter(eq("costCenterId", costCenterId));
        } else {
            lq.addFilter(isNull("costCenterId"));
        }
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : lineDao.findAllByQuery(lq)) {
            debit = debit.add(l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO);
            credit = credit.add(l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO);
        }
        return ErpFinConstants.DC_CREDIT.equals(direction) ? credit.subtract(debit) : debit.subtract(credit);
    }

    /** 查找命中的预算行（subjectId + costCenterId + periodId，所属方案 APPROVED）。无匹配返回 null。 */
    private BudgetLineMatch findMatchingBudgetLine(Long subjectId, Long costCenterId, Long periodId) {
        IEntityDao<ErpFinBudgetLine> lineDao = daoProvider.daoFor(ErpFinBudgetLine.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("subjectId", subjectId));
        lq.addFilter(eq("periodId", periodId));
        if (costCenterId != null) {
            lq.addFilter(eq("costCenterId", costCenterId));
        } else {
            lq.addFilter(isNull("costCenterId"));
        }
        for (ErpFinBudgetLine line : lineDao.findAllByQuery(lq)) {
            ErpFinBudgetScenario scenario = daoProvider.daoFor(ErpFinBudgetScenario.class)
                    .getEntityById(line.getScenarioId());
            if (scenario != null && Objects.equals(scenario.getDocStatus(), ErpFinConstants.BUDGET_STATUS_APPROVED)) {
                return new BudgetLineMatch(line, scenario);
            }
        }
        return null;
    }

    private void writeControlLog(BudgetLineMatch match, Long periodId, String sourceBillType, String sourceBillCode,
                                 BigDecimal requested, BigDecimal available, String actionResult, IServiceContext context) {
        IEntityDao<ErpFinBudgetControlLog> dao = daoProvider.daoFor(ErpFinBudgetControlLog.class);
        ErpFinBudgetControlLog logEntry = dao.newEntity();
        logEntry.setOrgId(match.scenario.getOrgId());
        logEntry.setBusinessDate(CoreMetrics.today());
        logEntry.setScenarioId(match.scenario.getId());
        logEntry.setBudgetLineId(match.line.getId());
        logEntry.setSourceBillType(sourceBillType != null ? sourceBillType : "UNKNOWN");
        logEntry.setSourceBillCode(sourceBillCode != null ? sourceBillCode : "");
        logEntry.setSubjectId(match.line.getSubjectId());
        logEntry.setCostCenterId(match.line.getCostCenterId());
        logEntry.setPeriodId(periodId);
        logEntry.setRequestedAmount(requested);
        logEntry.setCommittedAmount(actionResult.equals(BudgetCheckResult.ACTION_BLOCKED)
                ? BigDecimal.ZERO : requested);
        logEntry.setAvailableAmount(available);
        logEntry.setActionResult(actionResult);
        if (context != null && context.getUserContext() != null) {
            logEntry.setOperatorId(context.getUserContext().getUserId());
        }
        logEntry.setOperatedAt(CoreMetrics.currentDateTime());
        logEntry.setReason(actionResult.equals(BudgetCheckResult.ACTION_BLOCKED) ? "预算超支拦截" : "预算超支告警");
        dao.saveEntity(logEntry);
    }

    private boolean isBudgetCheckEnabled() {
        Boolean enabled = AppConfig.var(ErpFinConstants.CONFIG_BUDGET_CHECK_ENABLED, Boolean.FALSE);
        return Boolean.TRUE.equals(enabled);
    }

    private ErpMdSubject loadSubject(Long id) {
        return daoProvider.daoFor(ErpMdSubject.class).getEntityById(id);
    }

    private static final class BudgetLineMatch {
        final ErpFinBudgetLine line;
        final ErpFinBudgetScenario scenario;

        BudgetLineMatch(ErpFinBudgetLine line, ErpFinBudgetScenario scenario) {
            this.line = line;
            this.scenario = scenario;
        }
    }
}
