
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgForecastBiz;
import app.erp.mfg.dao.entity.ErpMfgForecast;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.statemachine.ErpMfgForecastStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 需求预测头 BizModel。在生成 CRUD 之上叠加状态机：
 * DRAFT→APPROVED（approve）/ DRAFT|APPROVED→CANCELLED（cancel）。
 * CONSUMED 状态值已预留于字典但本期不自动迁移（plan 2026-07-05-0427-1 §Deferred；
 * 状态机 Bean Decision A 见 plan 2026-08-12-1841-3）。
 *
 * <p>固定来源态/目标态判断经 {@link ErpMfgForecastStateMachine} Bean：Bean 自 plan 2026-09-07-2200-1 起
 * 直抛领域码 {@code ERR_FORECAST_ILLEGAL_STATUS_TRANSITION}（action/currentStatus/expectedStatus），
 * BizModel 仅同码补参 {@code forecastCode}。动态守卫（requireEntity/乐观锁）保留原位。
 *
 * <p>权威：{@code docs/design/manufacturing/mrp.md} §预测来源、plan 2026-07-05-0427-1 §Goals、
 * plan 2026-08-12-1841-3。
 */
@BizModel("ErpMfgForecast")
public class ErpMfgForecastBizModel extends AbstractErpCrudBizModel<ErpMfgForecast> implements IErpMfgForecastBiz {

    @Inject
    ErpMfgForecastStateMachine stateMachine;

    public ErpMfgForecastBizModel() {
        setEntityName(ErpMfgForecast.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgForecast approve(@Name("id") String id, IServiceContext context) {
        ErpMfgForecast forecast = requireEntity(id, null, context);
        String from = forecast.getStatus();
        try {
            stateMachine.assertCanApprove(from);
        } catch (NopException e) {
            throw e.param(ErpMfgErrors.ARG_FORECAST_CODE, forecast.getCode());
        }
        forecast.setStatus(stateMachine.approveTargetStatus());
        updateEntity(forecast, null, context);
        return forecast;
    }

    @Override
    @BizMutation
    public ErpMfgForecast cancel(@Name("id") String id, IServiceContext context) {
        ErpMfgForecast forecast = requireEntity(id, null, context);
        String from = forecast.getStatus();
        try {
            stateMachine.assertCanCancel(from);
        } catch (NopException e) {
            throw e.param(ErpMfgErrors.ARG_FORECAST_CODE, forecast.getCode());
        }
        forecast.setStatus(stateMachine.cancelTargetStatus());
        updateEntity(forecast, null, context);
        return forecast;
    }
}
