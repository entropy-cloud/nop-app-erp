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
import app.erp.md.dao.AcctSchemaResolver;
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
import java.sql.Timestamp;
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
 * <p>事务边界：跟随 Facade {@code @BizMutation} 事务；ORM Session 由本类 {@link #orm()}
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
    @Inject
    app.erp.fin.service.posting.SchemaPropagator schemaPropagator;

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

    protected Long resolveDefaultOrgId() {
        // 默认 1L（与 findOrCreatePeriodStatus 的 acctSchema fallback 同范式）。
        return 1L;
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

    /**
     * GL 模块关账：汇兑重估→损益结转（§步骤5）→ 试算平衡表快照 → 标记 glStatus=CLOSED。
     *
     * <p>多套账模式：各子服务（汇兑重估/损益结转/坏账）内部已按 SchemaPropagator 逐账套循环；
     * 试算平衡快照在此处逐账套生成。
     */
    protected void closeGlModule(ErpFinAccountingPeriod period, ErpFinAccountingPeriodStatus status,
                                 IServiceContext context) {
        if (isExchangeRevaluationEnabled()) {
            exchangeRevaluationService.revalue(period, context);
        }
        profitLossClosingService.close(period, context);
        populateTrialBalanceForAllSchemas(period);
        advanceModule(status, Module.GL);
    }

    /**
     * 折旧集成门控（§步骤3，G3 错误传播分级）：{@code erp-ast.auto-depreciation-on-close=true} 时调 assets 批量折旧。
     * <p>「impl 未就绪」（bizObjectManager 解析失败，如单域 finance 测试无 assets 模块）容错跳过（告警不阻断）；
     * 「配置错误/真实故障」（NopException ErrorCode，如 ERR_DEPRECIATION_RATE_MISSING）阻断结账（rethrow），
     * 避免期间带病关闭（GL 缺折旧凭证致累计折旧/费用低估）。posting-log.md §错误传播分级策略 G3。
     */
    protected void runDepreciation(ErpFinAccountingPeriod period, IServiceContext context) {
        if (!isAutoDepreciationOnClose()) {
            return;
        }
        IErpAstDepreciationScheduleBiz depreciationBiz;
        try {
            depreciationBiz = bizObjectManager
                    .getBizObject(ErpAstDepreciationSchedule.class.getSimpleName()).asProxy();
        } catch (Exception e) {
            // impl 未就绪（assets 域未部署）：容错跳过，告警不阻断结账。
            LOG.warn("期末结账：期间 {} 折旧集成跳过（impl 未就绪：{}）", period.getCode(), e.getMessage());
            return;
        }
        try {
            int processed = depreciationBiz.executeBatchDepreciation(period.getCode(), context);
            LOG.info("期末结账：期间 {} 批量折旧完成，成功计提 {} 项资产", period.getCode(), processed);
        } catch (NopException e) {
            // G3 配置错误/真实故障：阻断结账，避免期间带病关闭（GL 缺折旧凭证）。
            LOG.error("期末结账：期间 {} 折旧失败（配置错误/真实故障，阻断结账）：{}", period.getCode(), e.getMessage());
            throw e;
        }
    }

    /**
     * 存货成本兜底重算集成门控（§步骤2，G3 错误传播分级）：{@code erp-fin.inv-costing-reclose-on-close=true}（默认）时调
     * inventory {@code IErpInvCostingBiz.reclosePeriodCosts}。finance→inventory R（DAG 合法，对齐折旧门控范式）。
     * <p>单域 finance 测试无 inv-service 时经 IBizObjectManager 解析失败→容错跳过；
     * 配置错误/真实故障（NopException ErrorCode）→阻断结账（rethrow），避免 GL 缺成本调整凭证。
     */
    protected void recloseInvCosts(ErpFinAccountingPeriod period, IServiceContext context) {
        if (!isInvCostingRecloseOnClose()) {
            return;
        }
        IErpInvCostingBiz costingBiz;
        try {
            costingBiz = bizObjectManager.getBizObject("ErpInvCosting").asProxy();
        } catch (Exception e) {
            // impl 未就绪（inventory 域未部署）：容错跳过，告警不阻断结账。
            LOG.warn("期末结账：期间 {} 存货成本兜底重算跳过（impl 未就绪：{}）", period.getCode(), e.getMessage());
            return;
        }
        try {
            CostingRecloseReport report = costingBiz.reclosePeriodCosts(period.getId(),
                    period.getStartDate(), period.getEndDate(), context);
            LOG.info("期末结账：期间 {} 存货成本兜底重算完成，扫描 {} 单，补算入库层 {} / 出库 COGS {}",
                    period.getCode(), report.getScannedMoves(),
                    report.getRecomputedIncomingLayers(), report.getRecomputedOutgoingLedgers());
        } catch (NopException e) {
            // G3 配置错误/真实故障：阻断结账，避免期间带病关闭（GL 缺成本调整凭证）。
            LOG.error("期末结账：期间 {} 存货成本兜底重算失败（配置错误/真实故障，阻断结账）：{}",
                    period.getCode(), e.getMessage());
            throw e;
        }
    }

    /**
     * 反结账时条件冲销本期折旧凭证（§反结账步骤4，G3 错误传播分级）。配置门控；
     * impl 未就绪容错跳过；配置错误/真实故障（NopException）阻断反结账。
     */
    protected void reverseDepreciation(ErpFinAccountingPeriod period, IServiceContext context) {
        IErpAstDepreciationScheduleBiz depreciationBiz;
        try {
            depreciationBiz = bizObjectManager
                    .getBizObject(ErpAstDepreciationSchedule.class.getSimpleName()).asProxy();
        } catch (Exception e) {
            // impl 未就绪（assets 域未部署）：容错跳过，告警不阻断反结账。
            LOG.warn("期末结账：期间 {} 反结账折旧冲销跳过（impl 未就绪：{}）", period.getCode(), e.getMessage());
            return;
        }
        try {
            IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
            // 仅按期间 + 已过账过滤（已过账折旧即冲销对象）。
            QueryBean q = new QueryBean();
            q.addFilter(and(eq("period", period.getCode()), eq("posted", Boolean.TRUE)));
            List<ErpAstDepreciationSchedule> schedules = dao.findAllByQuery(q);
            for (ErpAstDepreciationSchedule s : schedules) {
                depreciationBiz.reverseDepreciation(s.getAssetId(), period.getCode(), context);
            }
        } catch (NopException e) {
            // G3 配置错误/真实故障：阻断反结账，避免状态不一致。
            LOG.error("期末结账：期间 {} 反结账折旧冲销失败（配置错误/真实故障，阻断反结账）：{}",
                    period.getCode(), e.getMessage());
            throw e;
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

    protected String moduleStatusOf(ErpFinAccountingPeriodStatus status, Module module) {
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

    protected void setModuleStatus(ErpFinAccountingPeriodStatus status, Module module, String value) {
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

    /**
     * 多套账试算平衡快照：按凭证行的 acctSchemaId 分组聚合，每个账套独立生成试算平衡行。
     * 凭证行已有 acctSchemaId（过账时按账套写入），此处按实际归属分组。
     */
    protected void populateTrialBalanceForAllSchemas(ErpFinAccountingPeriod period) {
        List<Long> voucherIds = findPostedVoucherIds(period.getId());
        IEntityDao<ErpFinTrialBalance> tbDao = daoProvider.daoFor(ErpFinTrialBalance.class);
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

        Map<Long, Map<Long, TbAgg>> bySchema = new LinkedHashMap<>();
        Long fallbackSchema = resolveAcctSchemaId(period.getId());
        for (ErpFinVoucherLine l : lines) {
            if (l.getSubjectId() == null) {
                continue;
            }
            Long schemaId = l.getAcctSchemaId() != null ? l.getAcctSchemaId() : fallbackSchema;
            Map<Long, TbAgg> subjectMap = bySchema.computeIfAbsent(schemaId, k -> new LinkedHashMap<>());
            TbAgg a = subjectMap.computeIfAbsent(l.getSubjectId(), k -> new TbAgg(l));
            a.debit = a.debit.add(l.getDebitAmount() == null ? BigDecimal.ZERO : l.getDebitAmount());
            a.credit = a.credit.add(l.getCreditAmount() == null ? BigDecimal.ZERO : l.getCreditAmount());
        }

        Timestamp generatedAt = CoreMetrics.currentTimestamp();
        for (Map.Entry<Long, Map<Long, TbAgg>> schemaEntry : bySchema.entrySet()) {
            Long acctSchemaId = schemaEntry.getKey();
            for (TbAgg a : schemaEntry.getValue().values()) {
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
    }

    protected List<Long> findPostedVoucherIds(Long periodId) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        q.addFilter(eq("isReversed", Boolean.FALSE));
        // 预算凭证（postingType=BUDGET）是影子凭证，不得进入实际试算平衡快照（budget.md 规则4/6/8）。
        q.addFilter(or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET)));
        return dao.findAllByQuery(q).stream().map(ErpFinVoucher::getId).collect(Collectors.toList());
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

    protected static final class TbAgg {
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

    protected List<String> findUnpostedVoucherCodes(ErpFinAccountingPeriod period) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", period.getId()));
        return dao.findAllByQuery(q).stream()
                .filter(v -> !ErpFinConstants.VOUCHER_STATUS_POSTED.equals(v.getDocStatus()))
                .map(ErpFinVoucher::getCode)
                .collect(Collectors.toList());
    }

    protected List<String> findUnsettledArApCodes(ErpFinAccountingPeriod period) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(ge("businessDate", period.getStartDate()), le("businessDate", period.getEndDate())));
        // 多账套/多组织读路径隔离（P1-MA2-095）：按期间所属组织 + 主账套限定 AR/AP 明细，避免跨组织/跨账套双计
        Long orgId = period.getOrgId();
        if (orgId != null) {
            q.addFilter(eq("orgId", orgId));
            Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
            if (schemaId != null) {
                q.addFilter(eq("acctSchemaId", schemaId));
            }
        }
        return dao.findAllByQuery(q).stream()
                .filter(i -> i.getStatus() != null
                        && !Objects.equals(i.getStatus(), ErpFinConstants.AR_AP_STATUS_SETTLED)
                        && !Objects.equals(i.getStatus(), ErpFinConstants.AR_AP_STATUS_CANCELLED)
                        && !Objects.equals(i.getStatus(), ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF))
                .map(ErpFinArApItem::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 扫描本期未处置业财悬挂（posting-log.md §错误传播分级策略 §期末结账前置检查覆盖矩阵）。
     * <p>覆盖：(1) finance 异常工作台 PENDING/RETRYING/MANUAL（voucherId 为空即未补录）；
     * (2) assets 折旧 posted=false（G4 无 sweep 覆盖，期末兜底）；
     * (3) inventory 到岸成本 posted=false 且已审核（G4，期末兜底）。
     * 各域扫描独立 try/catch：单域测试无对应实体时安全跳过（dao 解析失败）。
     */
    protected List<String> findUnresolvedPostingExceptionKeys(ErpFinAccountingPeriod period) {
        List<String> keys = new java.util.ArrayList<>();
        keys.addAll(findUnresolvedFinanceExceptions(period));
        keys.addAll(findUnresolvedDepreciationSchedules(period));
        keys.addAll(findUnresolvedLandedCosts(period));
        return keys;
    }

    /** finance 异常工作台未处置记录（PENDING/RETRYING/MANUAL 且未补录 voucherId）。 */
    @SuppressWarnings("unchecked")
    protected List<String> findUnresolvedFinanceExceptions(ErpFinAccountingPeriod period) {
        List<String> keys = new java.util.ArrayList<>();
        try {
            IEntityDao<app.erp.fin.dao.entity.ErpFinPostingException> dao =
                    daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class);
            QueryBean q = new QueryBean();
            // MANUAL 终态（G2 MAX_RETRY 升级）voucherId 为空即未补录，阻断结账；RETRIED/IGNORED 已决策不阻断。
            q.addFilter(in("status", java.util.Arrays.asList(
                    ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING,
                    ErpFinConstants.POSTING_EXCEPTION_STATUS_RETRYING,
                    ErpFinConstants.POSTING_EXCEPTION_STATUS_MANUAL)));
            q.addFilter(isNull("voucherId"));
            if (period.getStartDate() != null && period.getEndDate() != null) {
                q.addFilter(and(ge("voucherDate", period.getStartDate()), le("voucherDate", period.getEndDate())));
            }
            for (app.erp.fin.dao.entity.ErpFinPostingException e : dao.findAllByQuery(q)) {
                keys.add(e.getBillHeadCode() == null ? ("trace:" + e.getTraceId()) : e.getBillHeadCode());
            }
        } catch (Exception e) {
            LOG.debug("期末前置检查：finance 异常工作台扫描跳过（{}）", e.getMessage());
        }
        return keys;
    }

    /** assets 折旧 posted=false 悬挂（G4 无 sweep 覆盖，期末兜底阻断带病关闭）。仅扫 EXECUTED 状态（排除 REVERSED/CANCELLED，避免反结账→重结账循环误判已冲销 schedule）。 */
    protected List<String> findUnresolvedDepreciationSchedules(ErpFinAccountingPeriod period) {
        List<String> keys = new java.util.ArrayList<>();
        try {
            IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("posted", Boolean.FALSE));
            // 仅 EXECUTED+posted=false 为未过账悬挂；REVERSED（已冲销，posted=false 合法）/CANCELLED 不阻断。
            // status 值 "EXECUTED" 对齐 erp-ast/schedule-status 字典（assets-service ErpAstConstants.SCHEDULE_STATUS_EXECUTED）。
            q.addFilter(eq("status", "EXECUTED"));
            if (period.getCode() != null) {
                q.addFilter(eq("period", period.getCode()));
            }
            for (ErpAstDepreciationSchedule s : dao.findAllByQuery(q)) {
                keys.add("depreciation:" + (s.getAssetId() == null ? s.getId() : s.getAssetId())
                        + "#" + period.getCode());
            }
        } catch (Exception e) {
            // assets 实体未注册（单域 finance 测试无 ast-dao impl）时安全跳过。
            LOG.debug("期末前置检查：assets 折旧 posted=false 扫描跳过（{}）", e.getMessage());
        }
        return keys;
    }

    /** inventory 到岸成本 posted=false 且已审核悬挂（G4，期末兜底阻断存货价值漂移）。 */
    protected List<String> findUnresolvedLandedCosts(ErpFinAccountingPeriod period) {
        List<String> keys = new java.util.ArrayList<>();
        try {
            IEntityDao<app.erp.inv.dao.entity.ErpInvLandedCost> dao =
                    daoProvider.daoFor(app.erp.inv.dao.entity.ErpInvLandedCost.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("posted", Boolean.FALSE));
            q.addFilter(eq("approveStatus", ErpFinConstants.APPROVE_STATUS_APPROVED));
            if (period.getStartDate() != null && period.getEndDate() != null) {
                q.addFilter(and(ge("businessDate", period.getStartDate()), le("businessDate", period.getEndDate())));
            }
            for (app.erp.inv.dao.entity.ErpInvLandedCost lc : dao.findAllByQuery(q)) {
                keys.add("landed-cost:" + lc.getCode());
            }
        } catch (Exception e) {
            // inventory 实体未注册时安全跳过。
            LOG.debug("期末前置检查：inventory 到岸成本 posted=false 扫描跳过（{}）", e.getMessage());
        }
        return keys;
    }

    // ===================== 反结账凭证冲销 =====================

    protected void reverseCloseVoucher(ErpFinAccountingPeriod period, String billHeadCode,
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
        Long scopeSchemaId = resolveAcctSchemaId(period);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", period.getId()));
        // 多账套读路径隔离（P1-MA2-095）：按主账套限定期间状态，避免多账套误取首个状态行
        if (scopeSchemaId != null) {
            q.addFilter(eq("acctSchemaId", scopeSchemaId));
        }
        List<ErpFinAccountingPeriodStatus> list = dao.findAllByQuery(q);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        ErpFinAccountingPeriodStatus status = dao.newEntity();
        status.setPeriodId(period.getId());
        status.setAcctSchemaId(scopeSchemaId);
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

    protected Long resolveAcctSchemaId(ErpFinAccountingPeriod period) {
        Long orgId = period != null ? period.getOrgId() : null;
        if (orgId != null) {
            Long schemaId = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
            if (schemaId != null) {
                return schemaId;
            }
        }
        Long periodId = period != null ? period.getId() : null;
        if (periodId != null) {
            IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("periodId", periodId));
            q.setLimit(1);
            List<ErpFinVoucher> vouchers = dao.findAllByQuery(q);
            if (!vouchers.isEmpty() && vouchers.get(0).getAcctSchemaId() != null) {
                return vouchers.get(0).getAcctSchemaId();
            }
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
