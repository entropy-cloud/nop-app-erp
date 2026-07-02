package app.erp.fin.service.profitloss;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.close.CloseVoucherWriter;
import app.erp.fin.service.close.CloseVoucherWriter.Line;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.notIn;

/**
 * 损益结转服务（{@code period-close.md §步骤5}）。按 {@code erp-md/subject-class} 识别收入(40)/费用(50)/成本(60)
 * 三类科目，聚合本期发生额，生成本年利润结转凭证（业财类型 PERIOD_CLOSE）。
 *
 * <p><b>数据源</b>：聚合自 {@link ErpFinVoucherLine}（已过账、非红冲凭证的分录，排除 close 类分录自身）。
 * ErpFinGlBalance 在当前阶段未由过账引擎维护，故以 VoucherLine 为权威本期发生额来源（等价的期末活动聚合）。
 *
 * <p>结转方向：收入类（贷方余额）→ 借各收入科目 / 贷本年利润；费用+成本类（借方余额）→ 借本年利润 / 贷各费用成本科目。
 * 结转后损益类科目净发生额（含结转凭证）为零。
 */
public class ProfitLossClosingService {

    /** 结转凭证业财回链 billHeadCode 前缀（与 ErpFinAccountingPeriodBizModel.PL_BILL_CODE_PREFIX 一致）。 */
    public static final String BILL_CODE_PREFIX = "PERIOD-CLOSE-";

    @Inject
    IDaoProvider daoProvider;

    public Long close(ErpFinAccountingPeriod period, IServiceContext context) {
        // 收集本期已过账、非红冲凭证 ID。
        List<Long> voucherIds = findPostedVoucherIds(period.getId());
        if (voucherIds.isEmpty()) {
            return null;
        }
        // 本期分录（排除 close 类分录自身：PERIOD_CLOSE/FX，避免重复结转；Java 过滤以 null 安全）。
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("voucherId", voucherIds));
        List<ErpFinVoucherLine> lines = lineDao.findAllByQuery(q);

        // 按科目聚合（缓存科目取 subjectClass）。
        Map<Long, SubjectAgg> agg = new HashMap<>();
        Map<Long, ErpMdSubject> subjectCache = new HashMap<>();
        for (ErpFinVoucherLine l : lines) {
            if (l.getSubjectId() == null) {
                continue;
            }
            Integer bt = l.getBusinessType();
            if (bt != null && (bt == ErpFinBusinessType.PERIOD_CLOSE.getCode()
                    || bt == ErpFinBusinessType.EXCHANGE_GAIN_LOSS.getCode())) {
                continue;
            }
            ErpMdSubject subject = subjectCache.computeIfAbsent(l.getSubjectId(), this::loadSubject);
            if (subject == null) {
                continue;
            }
            int subjectClass = subject.getSubjectClass() == null ? 0 : subject.getSubjectClass();
            if (subjectClass != ErpFinConstants.SUBJECT_CLASS_INCOME
                    && subjectClass != ErpFinConstants.SUBJECT_CLASS_EXPENSE
                    && subjectClass != ErpFinConstants.SUBJECT_CLASS_COST) {
                continue;
            }
            SubjectAgg a = agg.computeIfAbsent(l.getSubjectId(), k -> new SubjectAgg(subject));
            a.debit = a.debit.add(nz(l.getDebitAmount()));
            a.credit = a.credit.add(nz(l.getCreditAmount()));
        }
        if (agg.isEmpty()) {
            return null;
        }

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenseCost = BigDecimal.ZERO;
        List<Line> plLines = new ArrayList<>();
        for (SubjectAgg a : agg.values()) {
            int cls = a.subject.getSubjectClass();
            if (cls == ErpFinConstants.SUBJECT_CLASS_INCOME) {
                BigDecimal net = a.credit.subtract(a.debit);
                if (net.compareTo(BigDecimal.ZERO) != 0) {
                    plLines.add(new Line(a.subject.getId(), a.subject.getCode(), a.subject.getName(),
                            ErpFinConstants.DC_DEBIT, net, null));
                    totalIncome = totalIncome.add(net);
                }
            } else {
                // 费用(50)+成本(60)
                BigDecimal net = a.debit.subtract(a.credit);
                if (net.compareTo(BigDecimal.ZERO) != 0) {
                    plLines.add(new Line(a.subject.getId(), a.subject.getCode(), a.subject.getName(),
                            ErpFinConstants.DC_CREDIT, net, null));
                    totalExpenseCost = totalExpenseCost.add(net);
                }
            }
        }
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0 && totalExpenseCost.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        // 有损益类发生额才解析本年利润科目（无发生额的干净期间无需配置 CYP）。
        ErpMdSubject cypSubject = requireSubject(ErpFinConstants.CONFIG_CURRENT_YEAR_PROFIT_SUBJECT_CODE,
                "本年利润");
        // 本年利润：贷方=收入合计，借方=费用+成本合计。
        if (totalIncome.compareTo(BigDecimal.ZERO) != 0) {
            plLines.add(new Line(cypSubject.getId(), cypSubject.getCode(), cypSubject.getName(),
                    ErpFinConstants.DC_CREDIT, totalIncome, null));
        }
        if (totalExpenseCost.compareTo(BigDecimal.ZERO) != 0) {
            plLines.add(new Line(cypSubject.getId(), cypSubject.getCode(), cypSubject.getName(),
                    ErpFinConstants.DC_DEBIT, totalExpenseCost, null));
        }

        Long orgId = period.getOrgId();
        Long acctSchemaId = resolveAcctSchemaId(period.getId());
        Long functionalCurrencyId = resolveFunctionalCurrencyId();
        return CloseVoucherWriter.writeVoucher(daoProvider, "CLP", BILL_CODE_PREFIX + period.getCode(),
                ErpFinBusinessType.PERIOD_CLOSE.getCode(), ErpFinBusinessType.PERIOD_CLOSE.name(),
                orgId, acctSchemaId, period.getId(), functionalCurrencyId, BigDecimal.ONE,
                period.getEndDate(), plLines, "期末损益结转");
    }

    private ErpMdSubject requireSubject(String configKey, String label) {
        String code = AppConfig.var(configKey, null);
        if (code == null || code.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, configKey);
        }
        ErpMdSubject subject = findSubjectByCode(code);
        if (subject == null) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, configKey);
        }
        return subject;
    }

    private ErpMdSubject findSubjectByCode(String code) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpMdSubject loadSubject(Long id) {
        return daoProvider.daoFor(ErpMdSubject.class).getEntityById(id);
    }

    private List<Long> findPostedVoucherIds(Long periodId) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        q.addFilter(eq("isReversed", Boolean.FALSE));
        return dao.findAllByQuery(q).stream().map(ErpFinVoucher::getId).collect(java.util.stream.Collectors.toList());
    }

    private Long resolveAcctSchemaId(Long periodId) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.setLimit(1);
        List<ErpFinVoucher> list = dao.findAllByQuery(q);
        if (!list.isEmpty() && list.get(0).getAcctSchemaId() != null) {
            return list.get(0).getAcctSchemaId();
        }
        return 1L;
    }

    private Long resolveFunctionalCurrencyId() {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isFunctional", Boolean.TRUE));
        q.setLimit(1);
        List<ErpMdCurrency> list = dao.findAllByQuery(q);
        if (!list.isEmpty()) {
            return list.get(0).getId();
        }
        return 1L;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static final class SubjectAgg {
        final ErpMdSubject subject;
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;

        SubjectAgg(ErpMdSubject subject) {
            this.subject = subject;
        }
    }
}
