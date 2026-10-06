package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstMaintenance;
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
import java.util.Map;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpMdSubject）=跨域批量聚合（md），只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * 维修资本化过账派发器（UC-AST-10）。维修工单 CAPITALIZE 路径 post 时（资产原值增量 + 折旧重算之后）
 * 组装 {@link PostingEvent}(MAINTENANCE_CAPITALIZATION) 经 {@link AssetPostingExecutor} 调用财务过账引擎。
 * billHeadCode = 维修单 code，作为幂等/红冲键。
 *
 * <p>资本化维修 = 既有资产原值增量（区别于新建 CAPITALIZATION(80) 建卡）。
 */
public class MaintenanceCapitalizationPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceCapitalizationPostingDispatcher.class);

    @Inject
    AssetPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    static final String NOTIFY_EVENT_MAINTENANCE_FAILURE = "ast.maintenance-posting-failure";

    public String tryPost(ErpAstMaintenance maintenance, ErpAstAsset asset, ErpAstAssetCategory category) {
        PostingEvent event = buildEvent(maintenance, asset, category);
        try {
            return executor.postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("maintenance capitalization posting failed, maintenance bill {} keeps posted=false: {}", maintenance.getCode(), e.getMessage());
            } else {
                LOG.error("maintenance capitalization posting error, maintenance bill {} keeps posted=false", maintenance.getCode(), e);
            }
            dispatchFailureAlert(maintenance, asset, e);
            return null;
        }
    }

    public void reverse(ErpAstMaintenance maintenance) {
        try {
            executor.reverse(maintenance.getCode(), ErpFinBusinessType.MAINTENANCE_CAPITALIZATION);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("maintenance capitalization reversal failed, maintenance bill {}: {}", maintenance.getCode(), e.getMessage());
            } else {
                LOG.error("maintenance capitalization reversal error, maintenance bill {}", maintenance.getCode(), e);
            }
            throw e;
        }
    }

    /**
     * P2-CK-ast2-009：维修过账失败告警派发（镜像 DepreciationPostingDispatcher 同名范式，
     * G4 错误传播分级；通知失败降级 warn 不阻断主流程；维修链无 DeferredPostingSweepJob 覆盖，
     * 自愈路径为修复后重试 post）。
     */
    protected void dispatchFailureAlert(ErpAstMaintenance maintenance, ErpAstAsset asset, Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("maintenanceCode", maintenance.getCode());
        ctx.put("maintenanceId", maintenance.getId());
        ctx.put("assetCode", asset.getCode());
        ctx.put("assetId", asset.getId());
        ctx.put("treatment", maintenance.getTreatment());
        ctx.put("errorCode", cause instanceof NopException ? ((NopException) cause).getErrorCode() : cause.getClass().getName());
        ctx.put("errorMessage", cause.getMessage());
        ctx.put("billHeadCode", maintenance.getCode());
        IServiceContext serviceCtx = serviceContext();
        try {
            notificationBiz.notify(NOTIFY_EVENT_MAINTENANCE_FAILURE, ctx, serviceCtx);
        } catch (Exception notifyErr) {
            LOG.warn("maintenance posting failure alert dispatch failed (degraded): maintenanceCode={}, reason={}",
                    maintenance.getCode(), notifyErr.getMessage());
        }
    }

    /** 当前服务上下文；无绑定时兜底新建（镜像 DepreciationPostingDispatcher 同名范式）。 */
    private static IServiceContext serviceContext() {
        IServiceContext context = IServiceContext.getCtx();
        return context != null ? context : new ServiceContextImpl();
    }

    private PostingEvent buildEvent(ErpAstMaintenance maintenance, ErpAstAsset asset, ErpAstAssetCategory category) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.MAINTENANCE_CAPITALIZATION);
        event.setBillHeadCode(maintenance.getCode());
        event.setOrgId(maintenance.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(maintenance.getOrgId()));
        event.setCurrencyId(maintenance.getCurrencyId());
        event.setExchangeRate(maintenance.getExchangeRate() != null ? maintenance.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = maintenance.getBusinessDate() != null ? maintenance.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        boolean linkedVisit = maintenance.getMaintenanceVisitId() != null;
        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_TREATMENT, ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE);
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_CAPITALIZED_AMOUNT, nz(maintenance.getCapitalizedAmount()));
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_TOTAL_COST, nz(maintenance.getTotalCostAmount()));
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_LINKED_VISIT, linkedVisit);
        billData.put(ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getSubjectId() : null, "1601"));
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_CLEARING_SUBJECT_CODE, "2502");
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_BANK_SUBJECT_CODE, "1002");
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_INVENTORY_SUBJECT_CODE, "1403");
        event.setBillData(billData);
        return event;
    }

    private String resolveAcctSchemaId(String orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    private String resolveSubjectCode(String subjectId, String defaultCode) {
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
