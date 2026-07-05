package app.erp.fin.service.annualclose;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinGlBalance;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 年度结转服务（{@code period-close.md §年度结转规则} 步骤3 + 步骤4 对账基线）。承接月度结账之后：
 * <ol>
 *   <li>本年利润科目余额 → 未分配利润科目结转凭证（业财类型 {@link ErpFinBusinessType#PROFIT_TO_RETAINED_EARNINGS}，
 *       本年利润清零）；</li>
 *   <li>populate 次年 1 月期间各科目 {@link ErpFinGlBalance#getYearOpeningDebit()} /
 *       {@link ErpFinGlBalance#getYearOpeningCredit()} 为次年比较基线（步骤4 跨账对账门控的年初快照）。</li>
 * </ol>
 *
 * <p><b>数据源</b>：聚合自 {@link ErpFinVoucherLine}（本年度各期间已过账、非红冲凭证的分录，排除年度结转自身凭证）。
 * ErpFinGlBalance 在当前阶段未由过账引擎维护（参 {@code ProfitLossClosingService}），故以 VoucherLine 为权威
 * 本年发生额来源；年度结转时创建次年 1 月的 GlBalance 快照行记录年初余额。
 *
 * <p>本年利润结转方向：本年利润为贷方余额（净利润）→ 借本年利润 / 贷未分配利润；借方余额（净亏损）→ 借未分配利润 / 贷本年利润。
 * 结转后本年利润科目本年净发生额（含年度结转凭证）为零。
 */
public class AnnualCloseService {

    /** 年度结转凭证业财回链 billHeadCode 前缀（反结账按此反查冲销）。 */
    public static final String BILL_CODE_PREFIX = "ANNUAL-CLOSE-";

    @Inject
    IDaoProvider daoProvider;

    /**
     * 执行年度结转：本年利润→未分配利润结转凭证 + 次年年初余额 populate。返回结转凭证 ID（无本年利润余额时返回 null）。
     *
     * @param period          年末期间（12 月）
     * @param nextYearPeriods 次年已生成的期间列表（用于 populate 年初余额，可为空——此时仅做结转不 populate）
     */
    public Long executeAnnualClose(ErpFinAccountingPeriod period, IServiceContext context) {
        Long voucherId = transferProfitToRetainedEarnings(period);
        populateNextYearOpening(period);
        return voucherId;
    }

    /**
     * 本年利润科目余额 → 未分配利润科目结转（{@code period-close.md §年度结转规则} 步骤3）。
     * 聚合本年所有期间的本年利润科目净余额，生成 PROFIT_TO_RETAINED_EARNINGS 凭证使本年利润清零。
     */
    public Long transferProfitToRetainedEarnings(ErpFinAccountingPeriod period) {
        Integer year = period.getYear();
        if (year == null) {
            return null;
        }
        // 本年利润科目净余额 = Σ(credit − debit) over 本年度已过账非红冲凭证分录。
        ErpMdSubject cypSubject = requireSubject(ErpFinConstants.CONFIG_CURRENT_YEAR_PROFIT_SUBJECT_CODE, "本年利润");
        BigDecimal cypNet = subjectNetForYear(cypSubject.getId(), year);
        if (cypNet.compareTo(BigDecimal.ZERO) == 0) {
            // 本年利润已为零（无发生或已结转），无需结转。
            return null;
        }
        ErpMdSubject retainedSubject = requireSubject(ErpFinConstants.CONFIG_RETAINED_EARNINGS_SUBJECT_CODE, "未分配利润");

        List<Line> lines = new ArrayList<>();
        // cypNet > 0 表示贷方余额（净利润）：借本年利润 / 贷未分配利润。
        // cypNet < 0 表示借方余额（净亏损）：借未分配利润 / 贷本年利润（取绝对值）。
        if (cypNet.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new Line(cypSubject.getId(), cypSubject.getCode(), cypSubject.getName(),
                    ErpFinConstants.DC_DEBIT, cypNet, null));
            lines.add(new Line(retainedSubject.getId(), retainedSubject.getCode(), retainedSubject.getName(),
                    ErpFinConstants.DC_CREDIT, cypNet, null));
        } else {
            BigDecimal abs = cypNet.negate();
            lines.add(new Line(retainedSubject.getId(), retainedSubject.getCode(), retainedSubject.getName(),
                    ErpFinConstants.DC_DEBIT, abs, null));
            lines.add(new Line(cypSubject.getId(), cypSubject.getCode(), cypSubject.getName(),
                    ErpFinConstants.DC_CREDIT, abs, null));
        }

        Long acctSchemaId = resolveAcctSchemaId(period.getId());
        Long functionalCurrencyId = resolveFunctionalCurrencyId();
        return CloseVoucherWriter.writeVoucher(daoProvider, "ACY", BILL_CODE_PREFIX + period.getCode(),
                ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS.name(),
                ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS.name(),
                period.getOrgId(), acctSchemaId, period.getId(), functionalCurrencyId, BigDecimal.ONE,
                period.getEndDate(), lines, "年度结转：本年利润→未分配利润");
    }

    /**
     * Populate 次年 1 月期间各科目的年初余额（{@code period-close.md §年度结转规则} 步骤4 对账基线）。
     * 取本年各科目年末净余额（贷/借），写入次年 1 月 GlBalance.yearOpening{Debit,Credit}。
     * 次年 1 月期间不存在时跳过（由 closePeriod 在调用前确保 generateNextYearPeriods 已执行）。
     */
    public void populateNextYearOpening(ErpFinAccountingPeriod period) {
        Integer year = period.getYear();
        if (year == null) {
            return;
        }
        ErpFinAccountingPeriod nextJan = findNextYearJanuaryPeriod(year + 1, period.getOrgId());
        if (nextJan == null) {
            // 次年期间未创建（config 关闭自动创建或手工未建），年初余额 populate 跳过，不阻断结转。
            return;
        }
        Long functionalCurrencyId = resolveFunctionalCurrencyId();
        Long acctSchemaId = resolveAcctSchemaId(period.getId());

        // 本年各科目净余额：按科目聚合 debit/credit。
        Map<Long, SubjectYearAgg> agg = aggregateYearSubjectActivity(year);
        if (agg.isEmpty()) {
            return;
        }
        IEntityDao<ErpFinGlBalance> glDao = daoProvider.daoFor(ErpFinGlBalance.class);
        // 先清除次年 1 月既有年初快照（幂等：支持反结账后重新结转）。
        QueryBean clearQ = new QueryBean();
        clearQ.addFilter(eq("periodId", nextJan.getId()));
        for (ErpFinGlBalance old : glDao.findAllByQuery(clearQ)) {
            glDao.deleteEntity(old);
        }

        for (SubjectYearAgg a : agg.values()) {
            BigDecimal net = a.debit.subtract(a.credit);
            if (net.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            ErpFinGlBalance gl = glDao.newEntity();
            gl.setOrgId(period.getOrgId());
            gl.setAcctSchemaId(acctSchemaId);
            gl.setPeriodId(nextJan.getId());
            gl.setSubjectId(a.subjectId);
            gl.setCurrencyId(functionalCurrencyId);
            gl.setOpeningDebit(BigDecimal.ZERO);
            gl.setOpeningCredit(BigDecimal.ZERO);
            gl.setPeriodDebit(BigDecimal.ZERO);
            gl.setPeriodCredit(BigDecimal.ZERO);
            gl.setClosingDebit(BigDecimal.ZERO);
            gl.setClosingCredit(BigDecimal.ZERO);
            // 年初借/贷：净借方余额→yearOpeningDebit；净贷方余额→yearOpeningCredit。
            gl.setYearOpeningDebit(net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO);
            gl.setYearOpeningCredit(net.compareTo(BigDecimal.ZERO) < 0 ? net.negate() : BigDecimal.ZERO);
            glDao.saveEntity(gl);
        }
    }

    // ===================== 辅助账对账门控 =====================

    /**
     * 辅助账跨年对账门控（{@code period-close.md §年度结转规则} 步骤4 跨账对账）。
     * 校验 AR/AP 辅助账合计与总账 AR/AP 科目余额一致（差异超精度抛错阻止年度结账）。
     * 不做物理搬移——存货/AR-AP/资产辅助账天然跨年延续。
     */
    public void assertAuxiliaryReconciles(ErpFinAccountingPeriod period) {
        Integer year = period.getYear();
        if (year == null) {
            return;
        }
        BigDecimal precision = resolveReconcilePrecision();

        // AR 辅助账：本年应收方向未核销项 openAmountFunctional 合计。
        BigDecimal arAux = sumArApOpenFunctional(ErpFinConstants.DIRECTION_RECEIVABLE);
        BigDecimal apAux = sumArApOpenFunctional(ErpFinConstants.DIRECTION_PAYABLE);

        // 总账 AR/AP 科目年末净余额。
        // 总账 AR/AP 科目年末净余额。subjectNetForYear 返回 credit−debit：
        // AR（资产，借方余额）→ GL 正值 = debit−credit = subjectNet.negate()；AP（负债，贷方余额）→ GL 正值 = credit−debit = subjectNet。
        BigDecimal arGl = BigDecimal.ZERO;
        BigDecimal apGl = BigDecimal.ZERO;
        String arCode = AppConfig.var(ErpFinConstants.CONFIG_AR_SUBJECT_CODE, null);
        String apCode = AppConfig.var(ErpFinConstants.CONFIG_AP_SUBJECT_CODE, null);
        ErpMdSubject arSubject = arCode == null ? null : findSubjectByCode(arCode);
        ErpMdSubject apSubject = apCode == null ? null : findSubjectByCode(apCode);
        if (arSubject != null) {
            arGl = subjectNetForYear(arSubject.getId(), year).negate().max(BigDecimal.ZERO);
        }
        if (apSubject != null) {
            apGl = subjectNetForYear(apSubject.getId(), year).max(BigDecimal.ZERO);
        }

        if (arSubject != null && arAux.subtract(arGl).abs().compareTo(precision) > 0) {
            throw new NopException(ErpFinErrors.ERR_AUXILIARY_RECON_MISMATCH)
                    .param(ErpFinErrors.ARG_AUX_MODULE, "AR")
                    .param(ErpFinErrors.ARG_AUX_AMOUNT, arAux.toPlainString())
                    .param(ErpFinErrors.ARG_GL_AMOUNT, arGl.toPlainString());
        }
        if (apSubject != null && apAux.subtract(apGl).abs().compareTo(precision) > 0) {
            throw new NopException(ErpFinErrors.ERR_AUXILIARY_RECON_MISMATCH)
                    .param(ErpFinErrors.ARG_AUX_MODULE, "AP")
                    .param(ErpFinErrors.ARG_AUX_AMOUNT, apAux.toPlainString())
                    .param(ErpFinErrors.ARG_GL_AMOUNT, apGl.toPlainString());
        }
    }

    private BigDecimal sumArApOpenFunctional(String direction) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinArApItem> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("direction", direction));
        q.addFilter(in("status", java.util.Arrays.asList(
                ErpFinConstants.AR_AP_STATUS_OPEN, ErpFinConstants.AR_AP_STATUS_PARTIAL)));
        BigDecimal sum = BigDecimal.ZERO;
        for (app.erp.fin.dao.entity.ErpFinArApItem i : dao.findAllByQuery(q)) {
            BigDecimal open = i.getOpenAmountFunctional();
            if (open != null) {
                sum = sum.add(open);
            }
        }
        return sum;
    }

    private BigDecimal resolveReconcilePrecision() {
        Object raw = AppConfig.var(ErpFinConstants.CONFIG_RECONCILE_PRECISION, new BigDecimal("0.01"));
        if (raw instanceof BigDecimal) {
            return (BigDecimal) raw;
        }
        if (raw == null) {
            return new BigDecimal("0.01");
        }
        try {
            return new BigDecimal(raw.toString());
        } catch (NumberFormatException e) {
            return new BigDecimal("0.01");
        }
    }

    // ===================== helpers =====================

    private BigDecimal subjectNetForYear(Long subjectId, int year) {
        List<Long> voucherIds = findYearPostedVoucherIds(year);
        if (voucherIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("subjectId", subjectId));
        q.addFilter(in("voucherId", voucherIds));
        BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : lineDao.findAllByQuery(q)) {
            debit = debit.add(l.getDebitAmount() == null ? BigDecimal.ZERO : l.getDebitAmount());
            credit = credit.add(l.getCreditAmount() == null ? BigDecimal.ZERO : l.getCreditAmount());
        }
        return credit.subtract(debit);
    }

    private Map<Long, SubjectYearAgg> aggregateYearSubjectActivity(int year) {
        List<Long> voucherIds = findYearPostedVoucherIds(year);
        Map<Long, SubjectYearAgg> agg = new LinkedHashMap<>();
        if (voucherIds.isEmpty()) {
            return agg;
        }
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("voucherId", voucherIds));
        Set<String> excludeTypes = new HashSet<>();
        excludeTypes.add(ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS.name());
        for (ErpFinVoucherLine l : lineDao.findAllByQuery(q)) {
            if (l.getSubjectId() == null) {
                continue;
            }
            String bt = l.getBusinessType();
            if (bt != null && excludeTypes.contains(bt)) {
                continue;
            }
            SubjectYearAgg a = agg.computeIfAbsent(l.getSubjectId(), k -> new SubjectYearAgg(l.getSubjectId()));
            a.debit = a.debit.add(l.getDebitAmount() == null ? BigDecimal.ZERO : l.getDebitAmount());
            a.credit = a.credit.add(l.getCreditAmount() == null ? BigDecimal.ZERO : l.getCreditAmount());
        }
        return agg;
    }

    private List<Long> findYearPostedVoucherIds(int year) {
        // 经期间表关联本年所有期间，再取这些期间内已过账非红冲凭证。
        IEntityDao<ErpFinAccountingPeriod> pDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean pq = new QueryBean();
        pq.addFilter(eq("year", year));
        List<Long> periodIds = new ArrayList<>();
        for (ErpFinAccountingPeriod p : pDao.findAllByQuery(pq)) {
            if (p.getId() != null) {
                periodIds.add(p.getId());
            }
        }
        if (periodIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean vq = new QueryBean();
        vq.addFilter(in("periodId", periodIds));
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        vq.addFilter(eq("isReversed", Boolean.FALSE));
        List<Long> ids = new ArrayList<>();
        for (ErpFinVoucher v : vDao.findAllByQuery(vq)) {
            ids.add(v.getId());
        }
        return ids;
    }

    private ErpFinAccountingPeriod findNextYearJanuaryPeriod(int nextYear, Long orgId) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("year", nextYear), eq("month", 1)));
        List<ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
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

    private static final class SubjectYearAgg {
        final Long subjectId;
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;

        SubjectYearAgg(Long subjectId) {
            this.subjectId = subjectId;
        }
    }
}
