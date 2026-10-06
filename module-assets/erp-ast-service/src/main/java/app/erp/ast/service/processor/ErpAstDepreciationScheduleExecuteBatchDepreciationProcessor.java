package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpAstAsset）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpAstDepreciationSchedule executeBatchDepreciation per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含批量折旧计提编排（期间控制 + 逐资产委托 {@link ErpAstDepreciationScheduleExecuteDepreciationProcessor} + 错误隔离）；
 * 共享 protected helper 单一真相源在 {@link ErpAstDepreciationScheduleProcessor}（delete-after-extract facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor.class);

    @Inject
    ErpAstDepreciationScheduleProcessor facade;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpAstDepreciationScheduleExecuteDepreciationProcessor executeDepreciationProcessor;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    static final String NOTIFY_EVENT_DEPRECIATION_FAILURE = "ast.depreciation-posting-failure";

    public int executeBatchDepreciation(String period, IServiceContext context) {
        facade.requirePeriodOpen(period, context);

        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpAstConstants.ASSET_STATUS_IN_SERVICE));
        List<ErpAstAsset> assets = dao.findAllByQuery(q);

        int processed = 0;
        for (ErpAstAsset asset : assets) {
            try {
                executeDepreciationProcessor.executeDepreciation(asset.getId(), period, context);
                processed++;
                // P2-CK-ast2-008（隔离最小修）：逐资产成功后 flush，单资产失败时 clearSession 丢弃
                // 半途脏状态——修复前单事务逐资产 catch 仅 LOG.warn，session 脏写 + 凭证孤儿 + 无告警。
                ormTemplate().flushSession();
            } catch (Exception e) {
                ormTemplate().clearSession();
                LOG.warn("batch depreciation: asset {} period {} accrual failed, skipped: {}", asset.getCode(), period, e.getMessage());
                dispatchBatchFailureAlert(asset, period, e);
            }
        }
        return processed;
    }

    private IOrmTemplate ormTemplate() {
        return ((IOrmEntityDao<?>) daoProvider.daoFor(ErpAstDepreciationSchedule.class)).getOrmTemplate();
    }

    /**
     * P2-CK-ast2-008：批量单资产失败告警派发（镜像 {@link DepreciationPostingDispatcher} G4 分级范式；
     * 通知失败降级 warn 不阻断主流程；其余资产照常完成）。
     */
    protected void dispatchBatchFailureAlert(ErpAstAsset asset, String period, Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("assetCode", asset.getCode());
        ctx.put("assetId", asset.getId());
        ctx.put("period", period);
        ctx.put("errorCode", cause instanceof NopException ? ((NopException) cause).getErrorCode() : cause.getClass().getName());
        ctx.put("errorMessage", cause.getMessage());
        ctx.put("billHeadCode", asset.getCode() + "#" + period + "#DEPRECIATION");
        IServiceContext serviceCtx = IServiceContext.getCtx();
        IServiceContext svcCtx = serviceCtx != null ? serviceCtx : new ServiceContextImpl();
        try {
            notificationBiz.notify(NOTIFY_EVENT_DEPRECIATION_FAILURE, ctx, svcCtx);
        } catch (Exception notifyErr) {
            LOG.warn("batch depreciation failure alert dispatch failed (degraded): assetCode={}, reason={}",
                    asset.getCode(), notifyErr.getMessage());
        }
    }
}
