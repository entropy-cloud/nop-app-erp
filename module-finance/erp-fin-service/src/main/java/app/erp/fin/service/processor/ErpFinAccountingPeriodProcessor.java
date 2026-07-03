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
import static io.nop.api.core.beans.FilterBeans.le;

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

    public PeriodPreCheckReport preCheck(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = requirePeriod(periodId);
        PeriodPreCheckReport report = new PeriodPreCheckReport();
        report.setUnpostedVoucherCodes(findUnpostedVoucherCodes(period));
        report.setUnsettledArApCodes(findUnsettledArApCodes(period));
        return report;
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

        // 期末凭证生成完成（期间仍 OPEN）后，状态簿记：CLOSING→CLOSED。flush 落库。
        period.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSING);
        period.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSED);
        period.setClosedAt(CoreMetrics.currentDateTime());
        period.setClosedBy(currentUserId());
        orm().flushSession();
        return period;
    }

    public ErpFinAccountingPeriod finalizePeriod(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = requirePeriod(periodId);
        assertPeriodStatus(period, ErpFinConstants.PERIOD_STATUS_CLOSED, "最终锁定");
        period.setStatus(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL);
        orm().flushSession();
        return period;
    }

    public ErpFinAccountingPeriod reverseClose(Long periodId, IServiceContext context) {
        ErpFinAccountingPeriod period = requirePeriod(periodId);
        assertPeriodStatus(period, ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, "反结账");

        if (isReverseCloseApprovalRequired()) {
            throw new NopException(ErpFinErrors.ERR_REVERSE_CLOSE_APPROVAL_REQUIRED)
                    .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode());
        }

        // 先回开期间为 OPEN，使红冲可经引擎过账（resolveOpenPeriod 要求 OPEN）。
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);

        // 冲销本期结转 / 汇兑（及条件折旧）凭证（红字）。
        reverseCloseVoucher(period, PL_BILL_CODE_PREFIX + period.getCode(), ErpFinBusinessType.PERIOD_CLOSE, context);
        reverseCloseVoucher(period, FX_BILL_CODE_PREFIX + period.getCode(), ErpFinBusinessType.EXCHANGE_GAIN_LOSS, context);
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
                        && !Objects.equals(i.getStatus(), ErpFinConstants.AR_AP_STATUS_CANCELLED))
                .map(ErpFinArApItem::getCode)
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
