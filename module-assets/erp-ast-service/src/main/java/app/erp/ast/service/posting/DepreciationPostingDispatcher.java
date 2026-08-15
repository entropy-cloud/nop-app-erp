package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 折旧过账派发器。折旧执行后（计划条目与资产卡片汇总列同事务确立之后）组装 {@link PostingEvent}(DEPRECIATION)
 * 经 {@link AssetPostingExecutor} 调用财务过账引擎。billHeadCode = 资产编码#期间，作为幂等/红冲键。
 *
 * <p>失败语义对齐 sales/inventory：过账失败吞异常返回 null（保持 posted=false），不阻塞折旧终态。
 * 反向（reverse）为硬前置，失败向上抛出。本类不持久化源单据——{@code posted}/voucherId 由调用方 BizModel 持久化。
 */
public class DepreciationPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(DepreciationPostingDispatcher.class);

    @Inject
    AssetPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    static final String NOTIFY_EVENT_DEPRECIATION_FAILURE = "ast.depreciation-posting-failure";

    /**
     * 折旧执行后调用。成功返回 voucherId（调用方据此置 posted=true + voucherId）；失败返回 null（保持 posted=false）。
     * <p>G4 错误传播分级（posting-log.md）：折旧路径无 DeferredPostingSweepJob 覆盖——失败派发
     * IErpSysNotificationBiz 告警使运营感知，自愈路径为手动重跑 executeDepreciation / reverseDepreciation。
     */
    public Long tryPost(ErpAstDepreciationSchedule schedule, ErpAstAsset asset, ErpAstAssetCategory category) {
        PostingEvent event = buildEvent(schedule, asset, category);
        try {
            return executor.postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("折旧过账失败，资产 {} 期间 {} 保持 posted=false：{}",
                        asset.getCode(), schedule.getPeriod(), e.getMessage());
            } else {
                LOG.error("折旧过账异常，资产 {} 期间 {} 保持 posted=false",
                        asset.getCode(), schedule.getPeriod(), e);
            }
            dispatchFailureAlert(asset, schedule, e);
            return null;
        }
    }

    /**
     * 折旧过账失败告警派发（G4 错误传播分级；plan 2026-07-30-0341-2 P1-MA4-013）。
     * 通知失败降级（warn）不阻断主流程。
     */
    protected void dispatchFailureAlert(ErpAstAsset asset, ErpAstDepreciationSchedule schedule, Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("assetCode", asset.getCode());
        ctx.put("assetId", asset.getId());
        ctx.put("period", schedule.getPeriod());
        ctx.put("errorCode", cause instanceof NopException ? ((NopException) cause).getErrorCode() : cause.getClass().getName());
        ctx.put("errorMessage", cause.getMessage());
        ctx.put("billHeadCode", billHeadCode(asset.getCode(), schedule.getPeriod()));
        IServiceContext serviceCtx = new ServiceContextImpl();
        try {
            notificationBiz.notify(NOTIFY_EVENT_DEPRECIATION_FAILURE, ctx, serviceCtx);
        } catch (Exception notifyErr) {
            LOG.warn("折旧过账失败告警派发失败（降级）：assetCode={}, reason={}",
                    asset.getCode(), notifyErr.getMessage());
        }
    }

    /**
     * 折旧补提汇总凭证派发（RC-R1.52 方式B，L1 UC-AST-07「当期一次性补提前期漏提额」）。
     * 多月漏提额在开放当前期间一次性汇总为单张凭证：billHeadCode = 资产编码#当前期间#CATCHUP（幂等/红冲键 +
     * 期间可追溯标注，镜像 A4.2.66 先例），voucherDate = 当前期间首日（财务引擎 resolveOpenPeriod 按凭证日期落账）。
     * 逐漏提期的归属经 billData 键 {@code CATCHUP_PERIODS} 传递，Provider 在凭证行 memo 标注「补提 {periods}」
     * （L1「补提凭证标注所属期间(审计)」）。失败语义对齐 {@link #tryPost}：吞异常返回 null（保持 posted=false），不阻塞补提终态。
     */
    public Long tryPostCatchUp(ErpAstAsset asset, ErpAstAssetCategory category, String currentPeriod,
                               BigDecimal totalAmount, List<String> caughtUpPeriods) {
        PostingEvent event = buildCatchUpEvent(asset, category, currentPeriod, totalAmount, caughtUpPeriods);
        try {
            return executor.postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("折旧补提过账失败，资产 {} 当前期间 {} 保持 posted=false：{}",
                        asset.getCode(), currentPeriod, e.getMessage());
            } else {
                LOG.error("折旧补提过账异常，资产 {} 当前期间 {} 保持 posted=false",
                        asset.getCode(), currentPeriod, e);
            }
            dispatchFailureAlert(asset, currentPeriod, caughtUpPeriods, e);
            return null;
        }
    }

    /** 折旧补提过账失败告警派发（镜像 {@link #dispatchFailureAlert}，event key 复用折旧失败事件使运营感知悬挂）。 */
    protected void dispatchFailureAlert(ErpAstAsset asset, String currentPeriod, List<String> caughtUpPeriods,
                                        Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("assetCode", asset.getCode());
        ctx.put("assetId", asset.getId());
        ctx.put("period", currentPeriod);
        ctx.put("catchUpPeriods", String.join(",", caughtUpPeriods));
        ctx.put("errorCode", cause instanceof NopException ? ((NopException) cause).getErrorCode() : cause.getClass().getName());
        ctx.put("errorMessage", cause.getMessage());
        ctx.put("billHeadCode", billHeadCode(asset.getCode(), currentPeriod));
        IServiceContext serviceCtx = new ServiceContextImpl();
        try {
            notificationBiz.notify(NOTIFY_EVENT_DEPRECIATION_FAILURE, ctx, serviceCtx);
        } catch (Exception notifyErr) {
            LOG.warn("折旧补提失败告警派发失败（降级）：assetCode={}, reason={}",
                    asset.getCode(), notifyErr.getMessage());
        }
    }

    private PostingEvent buildCatchUpEvent(ErpAstAsset asset, ErpAstAssetCategory category, String currentPeriod,
                                           BigDecimal totalAmount, List<String> caughtUpPeriods) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.DEPRECIATION);
        event.setBillHeadCode(catchUpBillHeadCode(asset.getCode(), currentPeriod));
        event.setOrgId(asset.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));
        event.setCurrencyId(asset.getCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(java.time.YearMonth.parse(currentPeriod).atDay(1));

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpAstConstants.BILL_DATA_DEPRECIATION_AMOUNT, nz(totalAmount));
        billData.put(ErpAstConstants.BILL_DATA_ASSET_ID, asset.getId());
        billData.put(ErpAstConstants.BILL_DATA_CATEGORY_ID, asset.getCategoryId());
        billData.put(ErpAstConstants.BILL_DATA_DEPARTMENT_ID, asset.getDepartmentId());
        billData.put(ErpAstConstants.BILL_DATA_PERIOD, currentPeriod);
        billData.put(ErpAstConstants.BILL_DATA_EXPENSE_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getExpenseSubjectId() : null, "6602"));
        billData.put(ErpAstConstants.BILL_DATA_ACCUM_DEPRE_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getDepreciationSubjectId() : null, "1602"));
        billData.put(ErpAstConstants.BILL_DATA_CATCHUP_PERIODS, String.join(",", caughtUpPeriods));
        event.setBillData(billData);
        return event;
    }

    static String catchUpBillHeadCode(String assetCode, String currentPeriod) {
        return billHeadCode(assetCode, currentPeriod) + ErpAstConstants.CATCHUP_BILL_SUFFIX;
    }

    /**
     * 幂等重执行前红字冲销已过账折旧凭证（对齐 §5.1）。冲销是硬前置，失败向上抛出。
     */
    public void reverse(ErpAstAsset asset, String period) {
        try {
            executor.reverse(billHeadCode(asset.getCode(), period), ErpFinBusinessType.DEPRECIATION);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("折旧红字冲销失败，资产 {} 期间 {}：{}", asset.getCode(), period, e.getMessage());
            } else {
                LOG.error("折旧红字冲销异常，资产 {} 期间 {}", asset.getCode(), period, e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpAstDepreciationSchedule schedule, ErpAstAsset asset,
                                    ErpAstAssetCategory category) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.DEPRECIATION);
        event.setBillHeadCode(billHeadCode(asset.getCode(), schedule.getPeriod()));
        event.setOrgId(schedule.getOrgId() != null ? schedule.getOrgId() : asset.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));
        event.setCurrencyId(asset.getCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = schedule.getBusinessDate() != null ? schedule.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpAstConstants.BILL_DATA_DEPRECIATION_AMOUNT, nz(schedule.getActualAmount()));
        billData.put(ErpAstConstants.BILL_DATA_ASSET_ID, asset.getId());
        billData.put(ErpAstConstants.BILL_DATA_CATEGORY_ID, asset.getCategoryId());
        billData.put(ErpAstConstants.BILL_DATA_DEPARTMENT_ID, asset.getDepartmentId());
        billData.put(ErpAstConstants.BILL_DATA_PERIOD, schedule.getPeriod());
        billData.put(ErpAstConstants.BILL_DATA_EXPENSE_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getExpenseSubjectId() : null, "6602"));
        billData.put(ErpAstConstants.BILL_DATA_ACCUM_DEPRE_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getDepreciationSubjectId() : null, "1602"));
        event.setBillData(billData);
        return event;
    }

    static String billHeadCode(String assetCode, String period) {
        return assetCode + "#" + period;
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
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
