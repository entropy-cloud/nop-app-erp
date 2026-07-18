package app.erp.fin.service.baddebt;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.dto.BadDebtProvisionResult;
import app.erp.fin.dao.dto.BadDebtProvisionReversalResult;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.close.CloseVoucherWriter;
import app.erp.fin.service.close.CloseVoucherWriter.Line;
import app.erp.fin.service.posting.FinPostingExecutor;
import app.erp.fin.service.posting.SchemaPropagator;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.ne;
import static io.nop.api.core.beans.FilterBeans.notIn;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 坏账准备期末计提/释放服务（{@code bad-debt.md §步骤2 计提 / §步骤5 释放 / §步骤2b 反向红冲}）。
 *
 * <p>{@link #runBadDebtProvision(Long, IServiceContext)} 作为期末计提/释放入口：
 * <ol>
 *   <li>按账龄分桶法计算必需准备（{@link BadDebtProvisionCalculator}）</li>
 *   <li>查询当前 Allowance GL 账面（cumulative，所有已过账非红冲凭证分录聚合）</li>
 *   <li>必需 &gt; 账面 → 补提 BAD_DEBT_RESERVE（借信用减值损失/贷坏账准备）</li>
 *   <li>必需 &lt; 账面 → 释放 BAD_DEBT_RELEASE（借坏账准备/贷信用减值损失）</li>
 *   <li>精度内相等 → 无动作</li>
 * </ol>
 *
 * <p>{@link #reverseBadDebtProvision(Long, IServiceContext)} 为反向红冲入口（plan 2026-07-18-2251-2）：
 * 按 {@code ErpFinVoucherBillR} 反查指定期间全部 BAD_DEBT_RESERVE/RELEASE 已过账未冲销 NORMAL 凭证 →
 * 调 {@link FinPostingExecutor#reverse} 原子红冲。
 *
 * <p>计提是期间估计批量动作（设计 Decision：无审批）；释放直接 P&L 影响，期末门控仅提示，
 * 实际释放由财务主管经本入口审批后执行（本计划范围内释放随计提批量，审批门控为 follow-up 接线点）。
 *
 * <p>凭证经 {@link CloseVoucherWriter} 直接持久化（与损益结转/汇兑重估同范式：分录来自余额/辅助账聚合，
 * 非 来源单据，不走 {@code IErpFinVoucherBiz.post} 的 Provider 模型，避免触发 ArApItem 生成）。
 */
public class BadDebtProvisionService {

    public static final String RESERVE_MEMO = "期末坏账准备计提";
    public static final String RELEASE_MEMO = "期末坏账准备释放";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    BadDebtProvisionCalculator calculator;

    @Inject
    SchemaPropagator schemaPropagator;

    @Inject
    FinPostingExecutor finPostingExecutor;

    /**
     * 期末计提/释放入口。返回结果含必需准备、Allowance 账面、动作与凭证 ID。
     *
     * @return 计提结果（action=NONE 时无凭证生成）
     */
    public BadDebtProvisionResult runBadDebtProvision(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = requirePeriod(periodId);
        Long primarySchemaId = resolveAcctSchemaId(periodId);
        List<Long> schemas = schemaPropagator.resolveTargetSchemas(period.getOrgId(), primarySchemaId);
        BadDebtProvisionResult lastResult = null;
        for (Long schemaId : schemas) {
            lastResult = runBadDebtProvisionForSchema(period, schemaId, context);
        }
        return lastResult;
    }

    /**
     * 反向坏账准备计提/释放入口（{@code bad-debt.md §步骤2b 反向红冲}，plan 2026-07-18-2251-2）。
     *
     * <p>经 {@link ErpFinVoucherBillR} 反查该期间全部 BAD_DEBT_RESERVE/RELEASE 凭证（按
     * {@code BAD_DEBT_RESERVE_BILL_CODE_PREFIX + period.code} / {@code BAD_DEBT_RELEASE_BILL_CODE_PREFIX + period.code}
     * 完全匹配——{@code CloseVoucherWriter.writeVoucher} 实测 billCode 确定性派生无 UUID 后缀）→
     * 过滤未冲销 NORMAL 凭证 → 按 (billCode, businessType) 调一次 {@link FinPostingExecutor#reverse}：
     * 平台 {@code ErpFinPostingProcessor.reverseProcess} 内部循环反查的全部未冲销凭证原子红冲 +
     * {@code markOriginalVoucherReversed} 补标 isReversed=true（参 Phase 1 Decision (a)）。
     *
     * <p>守卫：(1) period 不存在抛 {@code ERR_PERIOD_NOT_FOUND}；(2) period.status=CLOSED_FINAL 抛
     * {@code ERR_BAD_DEBT_PROVISION_PERIOD_FINAL_CLOSED}；(3) 未找到任何 BAD_DEBT_RESERVE/RELEASE
     * 已过账未冲销凭证抛 {@code ERR_BAD_DEBT_PROVISION_NOT_FOUND}。
     *
     * <p>事务边界：跟随 Facade {@code @BizMutation} 事务，本方法不带 {@code @Transactional}；
     * 任一 billCode 红冲失败抛 NopException 触发事务回滚（强一致：反审核为补救路径，须保证无残留半状态——
     * 参 Phase 2 §执行期注意 Decision (b)，对齐 {@code ErpFinBadDebtProcessor.reverseApprove} 范式）。
     *
     * @return 反向结果（含红冲凭证数量 + 反向金额合计，按 BDR/BDL 方向拆分）
     */
    public BadDebtProvisionReversalResult reverseBadDebtProvision(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = requirePeriod(periodId);

        if (Objects.equals(period.getStatus(), ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL)) {
            throw new NopException(ErpFinErrors.ERR_BAD_DEBT_PROVISION_PERIOD_FINAL_CLOSED)
                    .param(ErpFinErrors.ARG_PERIOD_ID, periodId)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode());
        }

        String reserveBillCode = ErpFinConstants.BAD_DEBT_RESERVE_BILL_CODE_PREFIX + period.getCode();
        String releaseBillCode = ErpFinConstants.BAD_DEBT_RELEASE_BILL_CODE_PREFIX + period.getCode();

        List<ErpFinVoucher> reserveVouchers = findUnreversedProvisionVouchers(reserveBillCode, ErpFinBusinessType.BAD_DEBT_RESERVE);
        List<ErpFinVoucher> releaseVouchers = findUnreversedProvisionVouchers(releaseBillCode, ErpFinBusinessType.BAD_DEBT_RELEASE);

        if (reserveVouchers.isEmpty() && releaseVouchers.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_BAD_DEBT_PROVISION_NOT_FOUND)
                    .param(ErpFinErrors.ARG_PERIOD_ID, periodId)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode());
        }

        BigDecimal reversedReserveAmount = sumVoucherAmount(reserveVouchers);
        BigDecimal reversedReleaseAmount = sumVoucherAmount(releaseVouchers);

        if (!reserveVouchers.isEmpty()) {
            finPostingExecutor.reverse(reserveBillCode, ErpFinBusinessType.BAD_DEBT_RESERVE);
        }
        if (!releaseVouchers.isEmpty()) {
            finPostingExecutor.reverse(releaseBillCode, ErpFinBusinessType.BAD_DEBT_RELEASE);
        }

        BadDebtProvisionReversalResult result = new BadDebtProvisionReversalResult();
        result.setPeriodId(periodId);
        result.setPeriodCode(period.getCode());
        result.setReversedReserveCount(reserveVouchers.size());
        result.setReversedReleaseCount(releaseVouchers.size());
        result.setReversedReserveAmount(reversedReserveAmount);
        result.setReversedReleaseAmount(reversedReleaseAmount);
        return result;
    }

    /**
     * 按 (billCode, businessType) 反查 {@link ErpFinVoucherBillR} 关联的已过账未冲销 NORMAL 凭证
     * （过滤条件对齐 {@code ErpFinPostingProcessor.findAllPostedVouchers}）。
     */
    public List<ErpFinVoucher> findUnreversedProvisionVouchers(String billCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("billCode", billCode),
                eq("businessType", businessType.name())
        ));
        List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(q);
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        List<ErpFinVoucher> result = new ArrayList<>();
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher voucher = voucherDao.getEntityById(link.getVoucherId());
            if (voucher != null
                    && ErpFinConstants.VOUCHER_STATUS_POSTED.equals(voucher.getDocStatus())
                    && !Boolean.TRUE.equals(voucher.getIsReversed())
                    && (voucher.getPostingType() == null
                        || Objects.equals(voucher.getPostingType(), ErpFinConstants.POSTING_TYPE_NORMAL))) {
                result.add(voucher);
            }
        }
        return result;
    }

    /** 红冲金额合计（voucher.totalDebit = voucher.totalCredit，二者等价）。 */
    protected BigDecimal sumVoucherAmount(List<ErpFinVoucher> vouchers) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpFinVoucher v : vouchers) {
            total = total.add(nz(v.getTotalDebit()));
        }
        return total;
    }

    private BadDebtProvisionResult runBadDebtProvisionForSchema(ErpFinAccountingPeriod period, Long acctSchemaId, IServiceContext context) {
        BadDebtProvisionResult result = calculateRequiredProvision(period);
        BigDecimal allowanceBalance = getAllowanceBalance();
        result.setAllowanceBalance(allowanceBalance);

        int cmp = result.getRequiredProvision().compareTo(allowanceBalance);
        if (cmp > 0) {
            BigDecimal amount = result.getRequiredProvision().subtract(allowanceBalance);
            ErpMdSubject expense = requireSubject(ErpFinConstants.CONFIG_BAD_DEBT_EXPENSE_SUBJECT_CODE, "信用减值损失");
            ErpMdSubject allowance = requireSubject(ErpFinConstants.CONFIG_BAD_DEBT_ALLOWANCE_SUBJECT_CODE, "坏账准备");
            List<Line> lines = Arrays.asList(
                    new Line(expense.getId(), expense.getCode(), expense.getName(),
                            ErpFinConstants.DC_DEBIT, amount, null),
                    new Line(allowance.getId(), allowance.getCode(), allowance.getName(),
                            ErpFinConstants.DC_CREDIT, amount, null));
            Long voucherId = CloseVoucherWriter.writeVoucher(daoProvider, "BDR",
                    ErpFinConstants.BAD_DEBT_RESERVE_BILL_CODE_PREFIX + period.getCode(),
                    ErpFinBusinessType.BAD_DEBT_RESERVE.name(), ErpFinBusinessType.BAD_DEBT_RESERVE.name(),
                    period.getOrgId(), acctSchemaId, period.getId(),
                    resolveFunctionalCurrencyId(), BigDecimal.ONE, period.getEndDate(), lines, RESERVE_MEMO);
            result.setAction("RESERVE");
            result.setVoucherId(voucherId);
        } else if (cmp < 0) {
            BigDecimal amount = allowanceBalance.subtract(result.getRequiredProvision());
            ErpMdSubject expense = requireSubject(ErpFinConstants.CONFIG_BAD_DEBT_EXPENSE_SUBJECT_CODE, "信用减值损失");
            ErpMdSubject allowance = requireSubject(ErpFinConstants.CONFIG_BAD_DEBT_ALLOWANCE_SUBJECT_CODE, "坏账准备");
            List<Line> lines = Arrays.asList(
                    new Line(allowance.getId(), allowance.getCode(), allowance.getName(),
                            ErpFinConstants.DC_DEBIT, amount, null),
                    new Line(expense.getId(), expense.getCode(), expense.getName(),
                            ErpFinConstants.DC_CREDIT, amount, null));
            Long voucherId = CloseVoucherWriter.writeVoucher(daoProvider, "BDL",
                    ErpFinConstants.BAD_DEBT_RELEASE_BILL_CODE_PREFIX + period.getCode(),
                    ErpFinBusinessType.BAD_DEBT_RELEASE.name(), ErpFinBusinessType.BAD_DEBT_RELEASE.name(),
                    period.getOrgId(), acctSchemaId, period.getId(),
                    resolveFunctionalCurrencyId(), BigDecimal.ONE, period.getEndDate(), lines, RELEASE_MEMO);
            result.setAction("RELEASE");
            result.setVoucherId(voucherId);
        } else {
            result.setAction("NONE");
        }
        return result;
    }

    /**
     * 计算必需准备（不查 Allowance 账面，不做凭证）。供期末门控（{@code ErpFinAccountingPeriodProcessor}）复用。
     */
    public BadDebtProvisionResult calculateRequiredProvision(ErpFinAccountingPeriod period) {
        List<ErpFinArApItem> receivableOpenItems = findReceivableOpenItems();
        return calculator.calculate(receivableOpenItems, period.getEndDate());
    }

    /**
     * 查询当前 Allowance GL 账面（cumulative 期末余额）。
     *
     * <p>Allowance 是抵减资产（贷方余额），账面 = Σ(credit − debit) over 所有已过账非红冲凭证分录。
     * ErpFinGlBalance 当前未由过账引擎维护（参 ProfitLossClosingService），故以 VoucherLine 为权威。
     */
    public BigDecimal getAllowanceBalance() {
        ErpMdSubject allowance = findSubject(ErpFinConstants.CONFIG_BAD_DEBT_ALLOWANCE_SUBJECT_CODE);
        if (allowance == null) {
            return BigDecimal.ZERO;
        }
        List<Long> voucherIds = findPostedVoucherIds();
        if (voucherIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("subjectId", allowance.getId()));
        q.addFilter(in("voucherId", voucherIds));
        List<ErpFinVoucherLine> lines = lineDao.findAllByQuery(q);
        BigDecimal balance = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : lines) {
            balance = balance.add(nz(l.getCreditAmount()).subtract(nz(l.getDebitAmount())));
        }
        return balance;
    }

    /**
     * 查询应收总额（未核销 AR openAmount 合计）。供 NRV 呈现与门控复用。
     */
    public BigDecimal getReceivableTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpFinArApItem item : findReceivableOpenItems()) {
            BigDecimal open = item.getOpenAmountFunctional();
            if (open != null && open.signum() > 0) {
                total = total.add(open);
            }
        }
        return total;
    }

    // ---------- helpers ----------

    protected List<ErpFinArApItem> findReceivableOpenItems() {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("direction", ErpFinConstants.DIRECTION_RECEIVABLE));
        q.addFilter(notIn("status", Arrays.asList(
                ErpFinConstants.AR_AP_STATUS_SETTLED,
                ErpFinConstants.AR_AP_STATUS_CANCELLED,
                ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF)));
        return dao.findAllByQuery(q);
    }

    /** 公开查询入口：应收方向未核销辅助账项（供测试与 NRV 呈现复用）。 */
    public List<ErpFinArApItem> getReceivableOpenItems() {
        return findReceivableOpenItems();
    }

    protected List<Long> findPostedVoucherIds() {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        q.addFilter(eq("isReversed", Boolean.FALSE));
        // 预算凭证（postingType=BUDGET）是影子凭证，不得计入实际坏账准备余额（budget.md 规则4/6/8）。
        q.addFilter(or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET)));
        return dao.findAllByQuery(q).stream().map(ErpFinVoucher::getId).collect(Collectors.toList());
    }

    protected ErpMdSubject requireSubject(String configKey, String label) {
        ErpMdSubject subject = findSubject(configKey);
        if (subject == null) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, configKey);
        }
        return subject;
    }

    protected ErpMdSubject findSubject(String configKey) {
        String code = io.nop.api.core.config.AppConfig.var(configKey, null);
        if (code == null || code.isEmpty()) {
            return null;
        }
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected ErpFinAccountingPeriod requirePeriod(Long periodId) {
        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        if (period == null) {
            throw new NopException(ErpFinErrors.ERR_PERIOD_NOT_FOUND).param(ErpFinErrors.ARG_PERIOD_ID, periodId);
        }
        return period;
    }

    protected Long resolveAcctSchemaId(Long periodId) {
        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        Long orgId = period != null ? period.getOrgId() : null;
        if (orgId != null) {
            Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
            if (schemaId != null) {
                return schemaId;
            }
        }
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

    protected Long resolveFunctionalCurrencyId() {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isFunctional", Boolean.TRUE));
        q.setLimit(1);
        List<ErpMdCurrency> list = dao.findAllByQuery(q);
        return list.isEmpty() ? 1L : list.get(0).getId();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
