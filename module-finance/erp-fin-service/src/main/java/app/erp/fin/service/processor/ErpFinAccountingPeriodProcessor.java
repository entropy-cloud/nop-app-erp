package app.erp.fin.service.processor;

import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PeriodPreCheckReport;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinTrialBalance;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.inv.biz.CostingRecloseReport;
import app.erp.inv.biz.IErpInvCostingBiz;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.baddebt.BadDebtProvisionService;
import app.erp.fin.service.annualclose.AnnualCloseService;
import app.erp.fin.service.fx.ExchangeRevaluationService;
import app.erp.fin.service.profitloss.ProfitLossClosingService;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.api.IBizObjectManager;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.ne;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 会计期间期末结账编排 Processor（{@code processor-extension-pattern.md} Facade + Processor）。
 * Facade {@code ErpFinAccountingPeriodBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>配置余地：模块关账各步（{@link #closeInvModule}/{@link #closeAssetModule}/{@link #closeGlModule}）、
 * 状态机迁移、试算平衡快照均为 {@code protected} 方法、以 {@link IServiceContext} 为末参，下游可逐 step 覆盖。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation}+{@code @SingleSession} 事务；ORM Session 由本类 {@link #orm()}
 * 获取（与 CrudBizModel.orm() 同源），期末凭证生成完成后再做状态簿记 + flush。
 */
public class ErpFinAccountingPeriodProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinAccountingPeriodProcessor.class);

    /** 损益结转凭证业财回链 billHeadCode 前缀（反结账按此反查冲销）。 */
    static final String PL_BILL_CODE_PREFIX = "PERIOD-CLOSE-";
    /** 汇兑重估凭证业财回链 billHeadCode 前缀。 */
    static final String FX_BILL_CODE_PREFIX = "FX-REVAL-";
    /** 年度结转凭证业财回链 billHeadCode 前缀（与 AnnualCloseService.BILL_CODE_PREFIX 一致）。 */
    static final String ANNUAL_BILL_CODE_PREFIX = "ANNUAL-CLOSE-";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IBizObjectManager bizObjectManager;
    @Inject
    IErpFinVoucherBiz voucherBiz;
    @Inject
    ProfitLossClosingService profitLossClosingService;
    @Inject
    ExchangeRevaluationService exchangeRevaluationService;
    @Inject
    BadDebtProvisionService badDebtProvisionService;
    @Inject
    AnnualCloseService annualCloseService;

    public PeriodPreCheckReport preCheck(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = requirePeriod(periodId);
        PeriodPreCheckReport report = new PeriodPreCheckReport();
        report.setUnpostedVoucherCodes(findUnpostedVoucherCodes(period));
        report.setUnsettledArApCodes(findUnsettledArApCodes(period));
        report.setUnresolvedPostingExceptionKeys(findUnresolvedPostingExceptionKeys(period));
        populateAllowanceCheck(period, report);
        return report;
    }

    /**
     * 坏账准备充足性门控（{@code bad-debt.md §期末 allowance 充足性门控}，对标 ar-close-engine C-R1）。
     * 必需准备（账龄分桶法）vs Allowance 账面：不足→阻止结账（shortfall &gt; 0）；超额→提示释放（excess &gt; 0，非阻断）。
     * 配置门控 {@code erp-fin.bad-debt-allowance-gate-enabled}（默认 true）。
     */
    protected void populateAllowanceCheck(ErpFinAccountingPeriod period, PeriodPreCheckReport report) {
        if (!isAllowanceGateEnabled()) {
            return;
        }
        try {
            app.erp.fin.dao.dto.BadDebtProvisionResult result = badDebtProvisionService.calculateRequiredProvision(period);
            BigDecimal balance = badDebtProvisionService.getAllowanceBalance();
            report.setAllowanceRequired(result.getRequiredProvision());
            report.setAllowanceBalance(balance);
            int cmp = result.getRequiredProvision().compareTo(balance);
            if (cmp > 0) {
                report.setAllowanceShortfall(result.getRequiredProvision().subtract(balance));
            } else if (cmp < 0) {
                report.setAllowanceExcess(balance.subtract(result.getRequiredProvision()));
            }
        } catch (NopException e) {
            // Allowance/Expense 科目未配置时门控跳过（告警不阻断，避免阻塞未启用坏账模块的账套）。
            LOG.warn("期末结账：期间 {} 坏账准备充足性门控跳过（{}）", period.getCode(), e.getMessage());
        }
    }

    public ErpFinAccountingPeriod closePeriod(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = requirePeriod(periodId);
        assertPeriodStatus(period, ErpFinConstants.PERIOD_STATUS_OPEN, "结账");

        PeriodPreCheckReport report = preCheck(periodId, context);
        if (!isAutoPostOnClose() && report.hasIssues()) {
            throw new NopException(ErpFinErrors.ERR_PRE_CHECK_BLOCKED)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode())
                    .param(ErpFinErrors.ARG_ISSUE_COUNT, report.issueCount());
        }

        ErpFinAccountingPeriodStatus status = findOrCreatePeriodStatus(period);
        // 模块按序关账：AR→AP→INV→AST→GL。INV 内运行存货成本兜底重算，AST 内运行折旧，GL 内运行汇兑重估→损益结转（均在期间仍 OPEN 时）。
        advanceModule(status, Module.AR);
        advanceModule(status, Module.AP);
        closeInvModule(period, status, context);
        closeAssetModule(period, status, context);
        closeGlModule(period, status, context);

        // 年度结转分支（period-close.md §年度结转规则）：12 月/年末结账时，常规月度结账后追加——
        // 辅助账对账门控 → 本年利润→未分配利润结转 → populate 次年年初余额 → 触发次年期间创建。
        // config-gated erp-fin.annual-close-enabled。
        if (isAnnualCloseEnabled() && isYearEnd(period)) {
            closeAnnual(period, status, context);
        }

        // 期末凭证生成完成（期间仍 OPEN）后，状态簿记：CLOSING→CLOSED。flush 落库。
        period.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSING);
        period.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSED);
        period.setClosedAt(CoreMetrics.currentDateTime());
        period.setClosedBy(currentUserId());
        orm().flushSession();
        return period;
    }

    /**
     * 年度结转追加步骤（{@code period-close.md §年度结转规则} 步骤3-5）。各 step 为 protected 供下游覆盖：
     * <ol>
     *   <li>{@link #assertAuxiliaryReconciles}——辅助账跨年对账门控（config-gated）；</li>
     *   <li>{@link AnnualCloseService#executeAnnualClose}——本年利润→未分配利润结转 + 次年年初余额 populate；</li>
     *   <li>{@link #generateNextYearPeriods}——次年 12 期间创建（config-gated 是否自动触发）。</li>
     * </ol>
     * 执行顺序：先创建次年期间（使 populate 年初余额有目标期间），再执行结转与 populate。
     */
    protected void closeAnnual(ErpFinAccountingPeriod period, ErpFinAccountingPeriodStatus status,
                               IServiceContext context) {
        // 步骤4 对账门控（结转前校验辅助账与总账一致）。
        if (isAuxiliaryReconGateEnabled()) {
            annualCloseService.assertAuxiliaryReconciles(period);
        }
        // 步骤5 次年期间创建（先于 populate，使年初余额有目标期间）。
        if (isAutoGenerateNextYearPeriods() && period.getYear() != null) {
            generateNextYearPeriods(period.getYear() + 1, context);
        }
        // 步骤3 本年利润→未分配利润 + 步骤4 年初余额 populate。
        annualCloseService.executeAnnualClose(period, context);
    }

    /** 判定期间是否为年末（year 非空且 month=12）。 */
    protected boolean isYearEnd(ErpFinAccountingPeriod period) {
        return period.getYear() != null
                && period.getMonth() != null
                && period.getMonth() == 12;
    }

    /** 次年期间是否已存在（反结账门控用）。 */
    protected boolean hasNextYearPeriods(int nextYear) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("year", nextYear));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    public ErpFinAccountingPeriod finalizePeriod(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = requirePeriod(periodId);
        assertPeriodStatus(period, ErpFinConstants.PERIOD_STATUS_CLOSED, "最终锁定");
        period.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL);
        orm().flushSession();
        return period;
    }

    /**
     * 批量生成指定年度 1-12 月会计期间（{@code period-close.md §年度结转规则} 步骤5）。
     *
     * <p>幂等策略（Decision）：同年期间已存在时，默认抛 {@code ERR_PERIODS_ALREADY_EXIST}；
     * 配置 {@code erp-fin.period-generate-skip-existing=true} 时仅补建缺失月份。
     * 状态分派：1 月 OPEN（假定次年即将开始核算），2-12 月 NEVER_OPENED（待运营按月开启）。
     */
    public Integer generateNextYearPeriods(Integer year, IServiceContext context) {
        if (year == null) {
            throw new NopException(ErpFinErrors.ERR_PERIOD_NOT_FOUND)
                    .param(ErpFinErrors.ARG_YEAR, year);
        }
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean existingQ = new QueryBean();
        existingQ.addFilter(eq("year", year));
        List<ErpFinAccountingPeriod> existing = dao.findAllByQuery(existingQ);

        if (!existing.isEmpty() && !isPeriodGenerateSkipExisting()) {
            throw new NopException(ErpFinErrors.ERR_PERIODS_ALREADY_EXIST)
                    .param(ErpFinErrors.ARG_YEAR, year)
                    .param(ErpFinErrors.ARG_EXISTING_PERIOD_COUNT, existing.size());
        }

        java.util.Set<Integer> existingMonths = existing.stream()
                .map(ErpFinAccountingPeriod::getMonth)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Long orgId = existing.isEmpty() ? resolveDefaultOrgId() : existing.get(0).getOrgId();

        int created = 0;
        java.time.YearMonth ym = java.time.YearMonth.of(year, 1);
        for (int month = 1; month <= 12; month++) {
            if (existingMonths.contains(month)) {
                continue;
            }
            java.time.YearMonth m = ym.withMonth(month);
            ErpFinAccountingPeriod p = dao.newEntity();
            String code = year + "-" + String.format("%02d", month);
            p.setCode(code);
            p.setName(code);
            p.setOrgId(orgId);
            p.setYear(year);
            p.setMonth(month);
            p.setStartDate(m.atDay(1));
            p.setEndDate(m.atEndOfMonth());
            p.setQuarter((month - 1) / 3 + 1);
            p.setIsAdjustment(Boolean.FALSE);
            // 1 月设为 OPEN（假定次年即将开始核算），其余 NEVER_OPENED 待运营开启。
            p.setStatus(month == 1 ? ErpFinConstants.PERIOD_STATUS_OPEN
                    : ErpFinConstants.PERIOD_STATUS_NEVER_OPENED);
            dao.saveEntity(p);
            created++;
        }
        orm().flushSession();
        return created;
    }

    private Long resolveDefaultOrgId() {
        // 默认 1L（与 findOrCreatePeriodStatus 的 acctSchema fallback 同范式）。
        return 1L;
    }

    public ErpFinAccountingPeriod reverseClose(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = requirePeriod(periodId);
        assertPeriodStatus(period, ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, "反结账");

        if (isReverseCloseApprovalRequired()) {
            throw new NopException(ErpFinErrors.ERR_REVERSE_CLOSE_APPROVAL_REQUIRED)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode());
        }

        // 年度结转反结账门控：若该期间为年末且次年期间已创建，阻止反结账（须先删次年期间）。
        if (isYearEnd(period) && period.getYear() != null && hasNextYearPeriods(period.getYear() + 1)) {
            throw new NopException(ErpFinErrors.ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode())
                    .param(ErpFinErrors.ARG_NEXT_YEAR, period.getYear() + 1);
        }

        // 先回开期间为 OPEN，使红冲可经引擎过账（resolveOpenPeriod 要求 OPEN）。
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);

        // 冲销本期结转 / 汇兑 / 年度结转（及条件折旧）凭证（红字）。
        reverseCloseVoucher(period, PL_BILL_CODE_PREFIX + period.getCode(), ErpFinBusinessType.PERIOD_CLOSE, context);
        reverseCloseVoucher(period, FX_BILL_CODE_PREFIX + period.getCode(), ErpFinBusinessType.EXCHANGE_GAIN_LOSS, context);
        if (isYearEnd(period)) {
            reverseCloseVoucher(period, ANNUAL_BILL_CODE_PREFIX + period.getCode(),
                    ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS, context);
        }
        if (isAutoDepreciationOnClose()) {
            reverseDepreciation(period, context);
        }

        // 回开各模块状态。
        ErpFinAccountingPeriodStatus status = findOrCreatePeriodStatus(period);
        reopenModules(status);
        orm().flushSession();
        return period;
    }

    // ===================== 模块关账（AR→AP→INV→AST→GL） =====================

    /** INV 模块关账：存货成本兜底重算（§步骤2，引用 inventory 域 IErpInvCostingBiz）→ 标记 invStatus=CLOSED。 */
    protected void closeInvModule(ErpFinAccountingPeriod period, ErpFinAccountingPeriodStatus status,
                                  IServiceContext context) {
        recloseInvCosts(period, context);
        advanceModule(status, Module.INV);
    }

    /** AST 模块关账：折旧计提（配置门控，§步骤3）→ 标记 assetStatus=CLOSED。 */
    protected void closeAssetModule(ErpFinAccountingPeriod period, ErpFinAccountingPeriodStatus status,
                                    IServiceContext context) {
        runDepreciation(period, context);
        advanceModule(status, Module.AST);
    }

    /** GL 模块关账：汇兑重估→损益结转（§步骤5）→ 试算平衡表快照 → 标记 glStatus=CLOSED。运行顺序 FX→P&L 使汇兑损益进入当期结转。 */
    protected void closeGlModule(ErpFinAccountingPeriod period, ErpFinAccountingPeriodStatus status,
                                 IServiceContext context) {
        if (isExchangeRevaluationEnabled()) {
            exchangeRevaluationService.revalue(period, context);
        }
        profitLossClosingService.close(period, context);
        populateTrialBalance(period);
        advanceModule(status, Module.GL);
    }

    /** 折旧集成门控（§步骤3）：{@code erp-ast.auto-depreciation-on-close=true} 时调 assets 批量折旧。失败告警不阻断。 */
    protected void runDepreciation(ErpFinAccountingPeriod period, IServiceContext context) {
        if (!isAutoDepreciationOnClose()) {
            return;
        }
        try {
            IErpAstDepreciationScheduleBiz depreciationBiz = bizObjectManager
                    .getBizObject(ErpAstDepreciationSchedule.class.getSimpleName()).asProxy();
            int processed = depreciationBiz.executeBatchDepreciation(period.getCode(), context);
            LOG.info("期末结账：期间 {} 批量折旧完成，成功计提 {} 项资产", period.getCode(), processed);
        } catch (Exception e) {
            // assets 域 impl 未就绪或折旧失败：告警不阻断结账（§ Non-Goal 配置门控）。
            LOG.warn("期末结账：期间 {} 折旧集成跳过（{}）", period.getCode(), e.getMessage());
        }
    }

    /**
     * 存货成本兜底重算集成门控（§步骤2）：{@code erp-fin.inv-costing-reclose-on-close=true}（默认）时调
     * inventory {@code IErpInvCostingBiz.reclosePeriodCosts}。finance→inventory R（DAG 合法，对齐折旧门控范式）。
     * 单域 finance 测试无 inv-service 时经 IBizObjectManager 解析失败→告警不阻断。
     */
    protected void recloseInvCosts(ErpFinAccountingPeriod period, IServiceContext context) {
        if (!isInvCostingRecloseOnClose()) {
            return;
        }
        try {
            IErpInvCostingBiz costingBiz = bizObjectManager.getBizObject("ErpInvCosting").asProxy();
            CostingRecloseReport report = costingBiz.reclosePeriodCosts(period.getId(),
                    period.getStartDate(), period.getEndDate(), context);
            LOG.info("期末结账：期间 {} 存货成本兜底重算完成，扫描 {} 单，补算入库层 {} / 出库 COGS {}",
                    period.getCode(), report.getScannedMoves(),
                    report.getRecomputedIncomingLayers(), report.getRecomputedOutgoingLedgers());
        } catch (Exception e) {
            LOG.warn("期末结账：期间 {} 存货成本兜底重算跳过（{}）", period.getCode(), e.getMessage());
        }
    }

    /** 反结账时条件冲销本期折旧凭证（§反结账步骤4）。配置门控 + 失败告警。 */
    protected void reverseDepreciation(ErpFinAccountingPeriod period, IServiceContext context) {
        try {
            IErpAstDepreciationScheduleBiz depreciationBiz = bizObjectManager
                    .getBizObject(ErpAstDepreciationSchedule.class.getSimpleName()).asProxy();
            IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
            // 仅按期间 + 已过账过滤（已过账折旧即冲销对象）。
            QueryBean q = new QueryBean();
            q.addFilter(and(eq("period", period.getCode()), eq("posted", Boolean.TRUE)));
            List<ErpAstDepreciationSchedule> schedules = dao.findAllByQuery(q);
            for (ErpAstDepreciationSchedule s : schedules) {
                depreciationBiz.reverseDepreciation(s.getAssetId(), period.getCode(), context);
            }
        } catch (Exception e) {
            LOG.warn("期末结账：期间 {} 反结账折旧冲销跳过（{}）", period.getCode(), e.getMessage());
        }
    }

    // ===================== 模块状态推进 =====================

    public void advanceModule(ErpFinAccountingPeriodStatus status, Module module) {
        Module prev = module.predecessor();
        if (prev != null && !Objects.equals(moduleStatusOf(status, prev), ErpFinConstants.MODULE_CLOSE_CLOSED)) {
            throw new NopException(ErpFinErrors.ERR_MODULE_OUT_OF_ORDER)
                    .param(ErpFinErrors.ARG_MODULE, module.name())
                    .param(ErpFinErrors.ARG_PREV_MODULE, prev.name());
        }
        setModuleStatus(status, module, ErpFinConstants.MODULE_CLOSE_CLOSING);
        setModuleStatus(status, module, ErpFinConstants.MODULE_CLOSE_CLOSED);
    }

    protected void reopenModules(ErpFinAccountingPeriodStatus status) {
        status.setArStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setApStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setInvStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setAssetStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setGlStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
    }

    private String moduleStatusOf(ErpFinAccountingPeriodStatus status, Module module) {
        switch (module) {
            case AR:
                return status.getArStatus();
            case AP:
                return status.getApStatus();
            case INV:
                return status.getInvStatus();
            case AST:
                return status.getAssetStatus();
            case GL:
                return status.getGlStatus();
            default:
                return ErpFinConstants.MODULE_CLOSE_OPEN;
        }
    }

    private void setModuleStatus(ErpFinAccountingPeriodStatus status, Module module, String value) {
        switch (module) {
            case AR:
                status.setArStatus(value);
                break;
            case AP:
                status.setApStatus(value);
                break;
            case INV:
                status.setInvStatus(value);
                break;
            case AST:
                status.setAssetStatus(value);
                break;
            case GL:
                status.setGlStatus(value);
                break;
            default:
                break;
        }
    }

    // ===================== 试算平衡表快照 =====================

    protected void populateTrialBalance(ErpFinAccountingPeriod period) {
        // 结转后试算平衡快照：聚合本期所有已过账、非红冲凭证分录（含结转凭证，反映结转后余额）。
        List<Long> voucherIds = findPostedVoucherIds(period.getId());
        IEntityDao<ErpFinTrialBalance> tbDao = daoProvider.daoFor(ErpFinTrialBalance.class);
        // 先清除本期既有快照（支持反结账后重新结账幂等）。
        QueryBean clearQ = new QueryBean();
        clearQ.addFilter(eq("periodId", period.getId()));
        for (ErpFinTrialBalance old : tbDao.findAllByQuery(clearQ)) {
            tbDao.deleteEntity(old);
        }

        if (voucherIds.isEmpty()) {
            return;
        }
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("voucherId", voucherIds));
        List<ErpFinVoucherLine> lines = lineDao.findAllByQuery(q);

        Map<Long, TbAgg> agg = new LinkedHashMap<>();
        for (ErpFinVoucherLine l : lines) {
            if (l.getSubjectId() == null) {
                continue;
            }
            TbAgg a = agg.computeIfAbsent(l.getSubjectId(), k -> new TbAgg(l));
            a.debit = a.debit.add(l.getDebitAmount() == null ? BigDecimal.ZERO : l.getDebitAmount());
            a.credit = a.credit.add(l.getCreditAmount() == null ? BigDecimal.ZERO : l.getCreditAmount());
        }

        Long acctSchemaId = resolveAcctSchemaId(period.getId());
        LocalDateTime generatedAt = CoreMetrics.currentDateTime();
        for (TbAgg a : agg.values()) {
            ErpFinTrialBalance tb = tbDao.newEntity();
            tb.setOrgId(period.getOrgId());
            tb.setAcctSchemaId(acctSchemaId);
            tb.setPeriodId(period.getId());
            tb.setSubjectId(a.subjectId);
            tb.setSubjectCode(a.subjectCode);
            tb.setSubjectName(a.subjectName);
            tb.setOpeningDebit(BigDecimal.ZERO);
            tb.setOpeningCredit(BigDecimal.ZERO);
            tb.setPeriodDebit(a.debit);
            tb.setPeriodCredit(a.credit);
            BigDecimal net = a.debit.subtract(a.credit);
            tb.setClosingDebit(net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO);
            tb.setClosingCredit(net.compareTo(BigDecimal.ZERO) < 0 ? net.negate() : BigDecimal.ZERO);
            tb.setGeneratedAt(generatedAt);
            tbDao.saveEntity(tb);
        }
    }

    private List<Long> findPostedVoucherIds(Long periodId) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        q.addFilter(eq("isReversed", Boolean.FALSE));
        // 预算凭证（postingType=BUDGET）是影子凭证，不得进入实际试算平衡快照（budget.md 规则4/6/8）。
        q.addFilter(or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET)));
        return dao.findAllByQuery(q).stream().map(ErpFinVoucher::getId).collect(Collectors.toList());
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

    private static final class TbAgg {
        final Long subjectId;
        final String subjectCode;
        final String subjectName;
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;

        TbAgg(ErpFinVoucherLine l) {
            this.subjectId = l.getSubjectId();
            this.subjectCode = l.getSubjectCode();
            this.subjectName = l.getSubjectName();
        }
    }

    // ===================== 前置检查查询 =====================

    private List<String> findUnpostedVoucherCodes(ErpFinAccountingPeriod period) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", period.getId()));
        return dao.findAllByQuery(q).stream()
                .filter(v -> !ErpFinConstants.VOUCHER_STATUS_POSTED.equals(v.getDocStatus()))
                .map(ErpFinVoucher::getCode)
                .collect(Collectors.toList());
    }

    private List<String> findUnsettledArApCodes(ErpFinAccountingPeriod period) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(ge("businessDate", period.getStartDate()), le("businessDate", period.getEndDate())));
        return dao.findAllByQuery(q).stream()
                .filter(i -> i.getStatus() != null
                        && !Objects.equals(i.getStatus(), ErpFinConstants.AR_AP_STATUS_SETTLED)
                        && !Objects.equals(i.getStatus(), ErpFinConstants.AR_AP_STATUS_CANCELLED)
                        && !Objects.equals(i.getStatus(), ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF))
                .map(ErpFinArApItem::getCode)
                .collect(Collectors.toList());
    }

    /** 扫描本期未处置过账异常（status=PENDING/RETRYING 且 voucherDate 落在本期，见 posting-log.md §失败不静默丢弃）。 */
    private List<String> findUnresolvedPostingExceptionKeys(ErpFinAccountingPeriod period) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinPostingException> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("status", java.util.Arrays.asList(
                ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING,
                ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRYING)));
        if (period.getStartDate() != null && period.getEndDate() != null) {
            q.addFilter(and(ge("voucherDate", period.getStartDate()), le("voucherDate", period.getEndDate())));
        }
        return dao.findAllByQuery(q).stream()
                .map(e -> e.getBillHeadCode() == null ? ("trace:" + e.getTraceId()) : e.getBillHeadCode())
                .collect(Collectors.toList());
    }

    // ===================== 反结账凭证冲销 =====================

    private void reverseCloseVoucher(ErpFinAccountingPeriod period, String billHeadCode,
                                     ErpFinBusinessType businessType, IServiceContext context) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.name())));
        if (dao.findAllByQuery(q).isEmpty()) {
            return;
        }
        voucherBiz.reverse(billHeadCode, businessType, context);
    }

    // ===================== helpers =====================

    protected ErpFinAccountingPeriod requirePeriod(Long periodId) {
        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        if (period == null) {
            throw new NopException(ErpFinErrors.ERR_PERIOD_NOT_FOUND).param(ErpFinErrors.ARG_PERIOD_ID, periodId);
        }
        return period;
    }

    protected void assertPeriodStatus(ErpFinAccountingPeriod period, String expected, String action) {
        if (!Objects.equals(period.getStatus(), expected)) {
            throw new NopException(ErpFinErrors.ERR_PERIOD_ILLEGAL_TRANSITION)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode())
                    .param(ErpFinErrors.ARG_CURRENT_PERIOD_STATUS, period.getStatus())
                    .param(ErpFinErrors.ARG_EXPECTED_PERIOD_STATUS, expected);
        }
    }

    protected ErpFinAccountingPeriodStatus findOrCreatePeriodStatus(ErpFinAccountingPeriod period) {
        IEntityDao<ErpFinAccountingPeriodStatus> dao = daoProvider.daoFor(ErpFinAccountingPeriodStatus.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", period.getId()));
        List<ErpFinAccountingPeriodStatus> list = dao.findAllByQuery(q);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        ErpFinAccountingPeriodStatus status = dao.newEntity();
        status.setPeriodId(period.getId());
        status.setAcctSchemaId(resolveAcctSchemaId(period));
        status.setTotalVouchers(0);
        status.setPostedVouchers(0);
        status.setUnpostedVouchers(0);
        status.setArStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setApStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setInvStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setGlStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setAssetStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        dao.saveEntity(status);
        return status;
    }

    private Long resolveAcctSchemaId(ErpFinAccountingPeriod period) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", period.getId()));
        q.setLimit(1);
        List<ErpFinVoucher> vouchers = dao.findAllByQuery(q);
        if (!vouchers.isEmpty() && vouchers.get(0).getAcctSchemaId() != null) {
            return vouchers.get(0).getAcctSchemaId();
        }
        return 1L;
    }

    protected boolean isAutoPostOnClose() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_AUTO_POST_ON_CLOSE, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    protected boolean isAutoDepreciationOnClose() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_AUTO_DEPRECIATION_ON_CLOSE, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    protected boolean isInvCostingRecloseOnClose() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_INV_COSTING_RECLOSE_ON_CLOSE, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    protected boolean isExchangeRevaluationEnabled() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_EXCHANGE_REVALUATION_ENABLED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    protected boolean isReverseCloseApprovalRequired() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_REVERSE_CLOSE_APPROVAL_REQUIRED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    /** 坏账准备充足性门控开关（{@code erp-fin.bad-debt-allowance-gate-enabled}，默认 true）。 */
    protected boolean isAllowanceGateEnabled() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_BAD_DEBT_ALLOWANCE_GATE_ENABLED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    /** 年度结转总开关（{@code erp-fin.annual-close-enabled}，默认 true）。 */
    protected boolean isAnnualCloseEnabled() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_ANNUAL_CLOSE_ENABLED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    /** 次年期间生成幂等策略：已存在时是否仅补缺失（{@code erp-fin.period-generate-skip-existing}，默认 false=抛错）。 */
    protected boolean isPeriodGenerateSkipExisting() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_PERIOD_GENERATE_SKIP_EXISTING, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    /** 银行存款外币重估开关（{@code erp-fin.bank-fx-revaluation-enabled}，默认 true）。 */
    protected boolean isBankFxRevaluationEnabled() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_BANK_FX_REVALUATION_ENABLED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    /** 年度结转时是否自动触发次年期间创建（{@code erp-fin.auto-generate-next-year-periods}，默认 true）。 */
    protected boolean isAutoGenerateNextYearPeriods() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_AUTO_GENERATE_NEXT_YEAR_PERIODS, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    /** 辅助账跨年对账门控开关（{@code erp-fin.auxiliary-recon-gate-enabled}，默认 true）。 */
    protected boolean isAuxiliaryReconGateEnabled() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_AUXILIARY_RECON_GATE_ENABLED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) daoProvider.daoFor(ErpFinAccountingPeriod.class)).getOrmTemplate();
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /** 关账模块顺序：AR→AP→INV→AST→GL。 */
    public enum Module {
        AR, AP, INV, AST, GL;

        Module predecessor() {
            switch (this) {
                case AP:
                    return AR;
                case INV:
                    return AP;
                case AST:
                    return INV;
                case GL:
                    return AST;
                default:
                    return null;
            }
        }
    }
}
