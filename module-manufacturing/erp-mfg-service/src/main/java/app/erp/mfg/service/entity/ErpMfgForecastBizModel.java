
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgForecastBiz;
import app.erp.mfg.dao.entity.ErpMfgForecast;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.statemachine.ErpMfgForecastStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 需求预测头 BizModel。在生成 CRUD 之上叠加状态机：
 * DRAFT→APPROVED（approve）/ DRAFT|APPROVED→CANCELLED（cancel）。
 * CONSUMED 状态值已预留于字典但本期不自动迁移（plan 2026-07-05-0427-1 §Deferred；
 * 状态机 Bean Decision A 见 plan 2026-08-12-1841-3）。
 *
 * <p>固定来源态/目标态判断经 {@link ErpMfgForecastStateMachine} Bean（契约 §4/§7）：Bean 抛 common 层
 * 非法迁移码，BizModel 映射为领域 {@code ERR_FORECAST_ILLEGAL_STATUS_TRANSITION} + 实体编号/上下文，
 * common 码作 cause 保留。动态守卫（requireEntity/乐观锁）保留原位。
 *
 * <p>权威：{@code docs/design/manufacturing/mrp.md} §预测来源、plan 2026-07-05-0427-1 §Goals、
 * plan 2026-08-12-1841-3。
 */
@BizModel("ErpMfgForecast")
public class ErpMfgForecastBizModel extends CrudBizModel<ErpMfgForecast> implements IErpMfgForecastBiz {

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
            throw illegalTransition(forecast, from, ErpMfgConstants.FORECAST_STATUS_DRAFT, e);
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
            throw illegalTransition(forecast, from,
                    ErpMfgConstants.FORECAST_STATUS_DRAFT + "/" + ErpMfgConstants.FORECAST_STATUS_APPROVED, e);
        }
        forecast.setStatus(stateMachine.cancelTargetStatus());
        updateEntity(forecast, null, context);
        return forecast;
    }

    /**
     * 经 StateMachine Bean 断言来源态合法；非法边（Bean 报告 common 层码）映射为领域
     * {@code ERR_FORECAST_ILLEGAL_STATUS_TRANSITION} + 实体编号/上下文，common 码作 cause 保留（契约 §7）。
     */
    private NopException illegalTransition(ErpMfgForecast forecast, String current, String expected, Throwable cause) {
        return new NopException(ErpMfgErrors.ERR_FORECAST_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpMfgErrors.ARG_FORECAST_CODE, forecast.getCode())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, expected);
    }
}
