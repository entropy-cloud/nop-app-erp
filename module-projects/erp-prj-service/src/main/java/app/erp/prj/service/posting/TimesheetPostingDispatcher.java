package app.erp.prj.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.biz.IErpMdCurrencyBiz;
import app.erp.md.biz.IErpMdExchangeRateBiz;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdExchangeRate;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.prj.dao.entity.ErpPrjActivityType;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectType;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.biz.api.IBizObjectManager;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 工时成本过账派发器。工时 APPROVED 后组装 {@link PostingEvent}(PROJECT_COST_COLLECTION)
 * 经 {@link ProjectPostingExecutor}（独立新事务由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW}
 * 承接）调用财务过账引擎。
 *
 * <p>对齐 assets/sales 失败语义：过账失败吞异常记日志、保持 APPROVED+{@code posted=false}，
 * 不阻塞终态。本类为 Facade 编排层，不持久化源单据——源单据 {@code posted} 标志由调用方 BizModel
 * 在主事务内统一持久化。
 *
 * <p>借方科目解析：项目类型 {@code defaultSubjectId}（缺失抛 {@link ErpPrjErrors#ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED}）；
 * 贷方科目：{@code erp-prj.default-payroll-subject-id}（为空抛
 * {@link ErpPrjErrors#ERR_PAYROLL_SUBJECT_NOT_CONFIGURED}）。
 */
public class TimesheetPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(TimesheetPostingDispatcher.class);

    @Inject
    ProjectPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IBizObjectManager bizObjectManager;

    static final String NOTIFY_EVENT_TIMESHEET_FAILURE = "prj.timesheet-posting-failure";

    /** dateBetween 开区间哨兵（对齐 hr 域 MAX_QUERY_DATE 先例）。 */
    static final LocalDate EPOCH_QUERY_DATE = LocalDate.of(1970, 1, 1);
    static final LocalDate MAX_QUERY_DATE = LocalDate.of(2999, 12, 31);

    /**
     * 工时审批通过后调用。成功返回 true（调用方据此置 posted=true）；失败吞异常返回 false（保持 posted=false）。
     * <p>G3 错误传播分级（plan 2026-07-30-0341-2 P1-MA2-068）：失败派发 IErpSysNotificationBiz 告警
     * 使 posted=false 悬挂可被感知。
     */
    public boolean tryPost(ErpPrjTimesheet timesheet) {
        PostingEvent event = buildEvent(timesheet);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("工时过账失败，工时单 {} 保持 APPROVED、posted=false：{}", timesheet.getCode(), e.getMessage());
            } else {
                LOG.error("工时过账异常，工时单 {} 保持 APPROVED、posted=false", timesheet.getCode(), e);
            }
            dispatchFailureAlert(timesheet, e);
            return false;
        }
    }

    /** 工时过账失败告警派发（G3；通知失败降级不阻断主流程）。 */
    protected void dispatchFailureAlert(ErpPrjTimesheet timesheet, Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("timesheetCode", timesheet.getCode());
        ctx.put("projectId", timesheet.getProjectId());
        ctx.put("errorCode", cause instanceof NopException ? ((NopException) cause).getErrorCode() : cause.getClass().getName());
        ctx.put("errorMessage", cause.getMessage());
        ctx.put("postingNo", timesheet.getCode());
        IServiceContext serviceCtx = new ServiceContextImpl();
        try {
            notificationBiz.notify(NOTIFY_EVENT_TIMESHEET_FAILURE, ctx, serviceCtx);
        } catch (Exception notifyErr) {
            LOG.warn("工时过账失败告警派发失败（降级）：timesheetCode={}, reason={}",
                    timesheet.getCode(), notifyErr.getMessage());
        }
    }

    /**
     * 反审批前红字冲销已过账凭证（对齐 posting.md §冲销）。冲销是硬前置，失败向上抛出阻断状态迁移。
     */
    public void reverse(ErpPrjTimesheet timesheet) {
        executor.reverse(timesheet.getCode(), ErpFinBusinessType.PROJECT_COST_COLLECTION);
    }

    private PostingEvent buildEvent(ErpPrjTimesheet timesheet) {
        ErpPrjProject project = loadProject(timesheet.getProjectId());
        ErpPrjProjectType projectType = project != null && project.getProjectTypeId() != null
                ? loadProjectType(project.getProjectTypeId()) : null;
        ErpPrjActivityType activityType = timesheet.getActivityTypeId() != null
                ? loadActivityType(timesheet.getActivityTypeId()) : null;

        String debitSubjectCode = resolveSubjectCode(
                projectType != null ? projectType.getDefaultSubjectId() : null,
                ProjectCostCollectionProvider.SUBJECT_PROJECT_COST_DEFAULT);
        if (projectType == null || projectType.getDefaultSubjectId() == null) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, timesheet.getProjectId());
        }

        String creditSubjectCode = ErpPrjConfigs.defaultPayrollSubjectCode();
        if (creditSubjectCode == null) {
            throw new NopException(ErpPrjErrors.ERR_PAYROLL_SUBJECT_NOT_CONFIGURED)
                    .param(ErpPrjErrors.ARG_SUBJECT_CODE, ErpPrjConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID);
        }

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.PROJECT_COST_COLLECTION);
        event.setBillHeadCode(timesheet.getCode());
        event.setOrgId(timesheet.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(timesheet.getOrgId()));
        event.setCurrencyId(timesheet.getCurrencyId());
        LocalDate voucherDate = timesheet.getWorkDate() != null ? timesheet.getWorkDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);
        // RC-R1.64：替代 BigDecimal.ONE 硬编码——非本位币经 ErpMdExchangeRate 按 currencyId+本位币+voucherDate
        // 边界解析（最近生效优先）；本位币/currencyId=null/币种不存在 → rate=1 回退（行为保持）；
        // 非本位币汇率缺失 → 抛 ERR_EXCHANGE_RATE_REQUIRED（对齐 R1.42 守卫与 UC-FIN-12 断言②语义）。
        event.setExchangeRate(resolveExchangeRate(timesheet.getCurrencyId(), voucherDate));

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpPrjConstants.BILL_DATA_PROJECT_ID, timesheet.getProjectId());
        billData.put(ErpPrjConstants.BILL_DATA_TASK_ID, timesheet.getTaskId());
        billData.put(ErpPrjConstants.BILL_DATA_ACTIVITY_TYPE_ID, timesheet.getActivityTypeId());
        billData.put(ErpPrjConstants.BILL_DATA_HOURS, timesheet.getHours());
        billData.put(ErpPrjConstants.BILL_DATA_COST_RATE, timesheet.getCostRate());
        billData.put(ErpPrjConstants.BILL_DATA_COST_AMOUNT, nz(timesheet.getCostAmount()));
        billData.put(ErpPrjConstants.BILL_DATA_DEBIT_SUBJECT_CODE, debitSubjectCode);
        billData.put(ErpPrjConstants.BILL_DATA_CREDIT_SUBJECT_CODE, creditSubjectCode);
        billData.put(ErpPrjConstants.BILL_DATA_SOURCE_BILL_TYPE, ErpPrjConstants.SOURCE_BILL_TYPE_TIMESHEET);
        if (activityType != null) {
            billData.put("ACTIVITY_TYPE_SUBJECT_CODE",
                    resolveSubjectCode(activityType.getSubjectId(), debitSubjectCode));
        }
        event.setBillData(billData);
        return event;
    }

    private ErpPrjProject loadProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        return dao.getEntityById(projectId);
    }

    private ErpPrjProjectType loadProjectType(Long projectTypeId) {
        IEntityDao<ErpPrjProjectType> dao = daoProvider.daoFor(ErpPrjProjectType.class);
        return dao.getEntityById(projectTypeId);
    }

    private ErpPrjActivityType loadActivityType(Long activityTypeId) {
        IEntityDao<ErpPrjActivityType> dao = daoProvider.daoFor(ErpPrjActivityType.class);
        return dao.getEntityById(activityTypeId);
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    /**
     * 工时过账汇率解析三态（RC-R1.64，D1 裁决 A）：
     * currencyId=null → rate=1；币种不存在 → rate=1 保守放行（镜像 guardExchangeRate D2 语义）；
     * 本位币（isFunctional=TRUE）→ rate=1；非本位币 → 经 ErpMdExchangeRate 按
     * from=currencyId + to=本位币 + validFrom<=voucherDate<=validTo 边界匹配（最近生效优先，limit 1）解析，
     * 本位币缺失或汇率行未命中 → 抛 {@link ErpFinErrors#ERR_EXCHANGE_RATE_REQUIRED}（跨域语义同源 R1.42 守卫）。
     */
    protected BigDecimal resolveExchangeRate(Long currencyId, LocalDate voucherDate) {
        if (currencyId == null) {
            return BigDecimal.ONE;
        }
        IServiceContext context = currentContext();
        ErpMdCurrency currency = findCurrencyById(currencyId, context);
        if (currency == null) {
            LOG.warn("工时过账汇率解析：币种 {} 不存在，无法判定本位币归属，保守放行 rate=1", currencyId);
            return BigDecimal.ONE;
        }
        if (Boolean.TRUE.equals(currency.getIsFunctional())) {
            return BigDecimal.ONE;
        }
        ErpMdCurrency functional = findFunctionalCurrency(context);
        if (functional == null) {
            throw exchangeRateRequired(currency);
        }
        BigDecimal rate = findExchangeRate(currencyId, functional.getId(), voucherDate, context);
        if (rate == null) {
            throw exchangeRateRequired(currency);
        }
        return rate;
    }

    /** 按 id 查询币种。跨域只读经 IErpMdCurrencyBiz（IBizObjectManager 按名解析，对齐 ErpFinPostingProcessor.findCurrencyById 范式）。 */
    protected ErpMdCurrency findCurrencyById(Long currencyId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("id", currencyId));
        q.setLimit(1);
        IErpMdCurrencyBiz currencyBiz = bizObjectManager.getBizObject(ErpMdCurrency.class.getSimpleName()).asProxy();
        return currencyBiz.findFirst(q, null, context);
    }

    /** 解析本位币（ErpMdCurrency.isFunctional=TRUE 主载体；schema 级细分归 successor，见 posting.md:445 注记）。 */
    protected ErpMdCurrency findFunctionalCurrency(IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("isFunctional", Boolean.TRUE));
        q.setLimit(1);
        IErpMdCurrencyBiz currencyBiz = bizObjectManager.getBizObject(ErpMdCurrency.class.getSimpleName()).asProxy();
        return currencyBiz.findFirst(q, null, context);
    }

    /**
     * 汇率行查找：eq(from)+eq(to) + validFrom<=voucherDate<=validTo 边界匹配 + validFrom 降序（最近生效优先）
     * + limit 1；无边界匹配行不回退更早汇率（避免静默用错期汇率）。
     * 边界经 dateBetween(epoch/2999 哨兵) 表达——XMeta 过滤算子白名单 [eq, in, dateBetween, dateTimeBetween]
     * 无 ge/le（对齐 ErpCtContractBizModel:340-342 / ErpHrLeaveRequestBizModel MAX_QUERY_DATE 先例）。
     * rateType 不作过滤（信息性维度：default SPOT 与 refresh API 写入 MIDDLE 并存，按类型过滤会漏另一类数据行）。
     */
    protected BigDecimal findExchangeRate(Long fromCurrencyId, Long toCurrencyId, LocalDate voucherDate,
                                          IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("fromCurrencyId", fromCurrencyId));
        q.addFilter(eq("toCurrencyId", toCurrencyId));
        q.addFilter(dateBetween("validFrom", EPOCH_QUERY_DATE, voucherDate));
        q.addFilter(dateBetween("validTo", voucherDate, MAX_QUERY_DATE));
        q.addOrderField("validFrom", true);
        q.setLimit(1);
        IErpMdExchangeRateBiz rateBiz = bizObjectManager.getBizObject(ErpMdExchangeRate.class.getSimpleName()).asProxy();
        ErpMdExchangeRate rate = rateBiz.findFirst(q, null, context);
        return rate != null && rate.getRate() != null ? rate.getRate() : null;
    }

    private IServiceContext currentContext() {
        IServiceContext context = IServiceContext.getCtx();
        return context != null ? context : new ServiceContextImpl();
    }

    private NopException exchangeRateRequired(ErpMdCurrency currency) {
        return new NopException(ErpFinErrors.ERR_EXCHANGE_RATE_REQUIRED)
                .param(ErpFinErrors.ARG_CURRENCY_CODE, currency.getCode());
    }

    private String resolveSubjectCode(Long subjectId, String defaultCode) {
        if (subjectId == null) {
            return defaultCode;
        }
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = dao.getEntityById(subjectId);
        if (subject == null || subject.getCode() == null || subject.getCode().trim().isEmpty()) {
            return defaultCode;
        }
        return subject.getCode().trim();
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
