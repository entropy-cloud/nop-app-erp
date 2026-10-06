package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstConstants;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.exceptions.NopException;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.api.core.beans.query.QueryBean;
import static io.nop.api.core.beans.FilterBeans.eq;
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
 * 维修费用化过账派发器（UC-AST-10）。维修工单 EXPENSE 路径 post 时组装 {@link PostingEvent}(MAINTENANCE_EXPENSE)
 * 经 {@link AssetPostingExecutor} 调用财务过账引擎。billHeadCode = 维修单 code，作为幂等/红冲键。
 *
 * <p>billData 汇总费用合计 + 科目映射 + 关联维护工单标志（防双重扣减分支）；
 * Provider 按分支科目分解（镜像 {@code ValueAdjustmentPostingDispatcher} 范式）。
 */
public class MaintenanceExpensePostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceExpensePostingDispatcher.class);

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
                LOG.warn("maintenance expense posting failed, maintenance bill {} keeps posted=false: {}", maintenance.getCode(), e.getMessage());
            } else {
                LOG.error("maintenance expense posting error, maintenance bill {} keeps posted=false", maintenance.getCode(), e);
            }
            dispatchFailureAlert(maintenance, asset, e);
            return null;
        }
    }

    /**
     * P2-CK-ast2-009：维修过账失败告警派发（镜像 {@link DepreciationPostingDispatcher} 同名范式，
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

    public void reverse(ErpAstMaintenance maintenance) {
        try {
            executor.reverse(maintenance.getCode(), ErpFinBusinessType.MAINTENANCE_EXPENSE);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("maintenance expense reversal failed, maintenance bill {}: {}", maintenance.getCode(), e.getMessage());
            } else {
                LOG.error("maintenance expense reversal error, maintenance bill {}", maintenance.getCode(), e);
            }
            throw e;
        }
    }

    /** SPARE_PART 成本行合计（ast2-013 分支 a 贷方分腿金额）。 */
    protected BigDecimal sumSparePartAmount(String maintenanceId) {
        BigDecimal spare = BigDecimal.ZERO;
        IEntityDao<app.erp.ast.dao.entity.ErpAstMaintenanceCost> costDao =
                daoProvider.daoFor(app.erp.ast.dao.entity.ErpAstMaintenanceCost.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("maintenanceId", maintenanceId));
        q.addFilter(eq("costType", ErpAstConstants.MAINTENANCE_COST_TYPE_SPARE_PART));
        for (app.erp.ast.dao.entity.ErpAstMaintenanceCost line : costDao.findAllByQuery(q)) {
            spare = spare.add(line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO);
        }
        return spare;
    }

    private PostingEvent buildEvent(ErpAstMaintenance maintenance, ErpAstAsset asset, ErpAstAssetCategory category) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.MAINTENANCE_EXPENSE);
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
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_TREATMENT, ErpAstConstants.MAINTENANCE_TREATMENT_EXPENSE);
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_TOTAL_COST, nz(maintenance.getTotalCostAmount()));
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_LINKED_VISIT, linkedVisit);
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_EXPENSE_SUBJECT_CODE, "6602");
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_CLEARING_SUBJECT_CODE, "2502");
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_BANK_SUBJECT_CODE, "1002");
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_INVENTORY_SUBJECT_CODE, "1403");
        // P2-CK-ast2-013（分支 a）：备件消耗额供 Provider 贷方分腿（SPARE_PART→存货 1403 / 其余→银行 1002，
        // 落地 maintenance.md L88/L154 既有声明）；无备件行时为 0（Provider 单腿贷银行，行为同旧）。
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_SPARE_PART_AMOUNT, sumSparePartAmount(maintenance.getId()));
        billData.put(ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getSubjectId() : null, "1601"));
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
