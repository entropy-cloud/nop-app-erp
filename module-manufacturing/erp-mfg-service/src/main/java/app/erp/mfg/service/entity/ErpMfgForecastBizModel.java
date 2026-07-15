
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgForecastBiz;
import app.erp.mfg.dao.entity.ErpMfgForecast;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.util.List;
import java.util.Objects;

/**
 * 需求预测头 BizModel。在生成 CRUD 之上叠加状态机：
 * DRAFT→APPROVED（approve）/ DRAFT|APPROVED→CANCELLED（cancel）。
 * CONSUMED 状态值已预留于字典但本期不自动迁移（plan 2026-07-05-0427-1 §Deferred）。
 *
 * <p>权威：{@code docs/design/manufacturing/mrp.md} §预测来源、plan 2026-07-05-0427-1 §Goals。
 */
@BizModel("ErpMfgForecast")
public class ErpMfgForecastBizModel extends CrudBizModel<ErpMfgForecast> implements IErpMfgForecastBiz {

    public ErpMfgForecastBizModel() {
        setEntityName(ErpMfgForecast.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgForecast approve(@Name("id") String id, IServiceContext context) {
        ErpMfgForecast forecast = requireEntity(id, null, context);
        String status = forecast.getStatus();
        if (!Objects.equals(status, ErpMfgConstants.FORECAST_STATUS_DRAFT)) {
            throw new NopException(ErpMfgErrors.ERR_FORECAST_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpMfgErrors.ARG_FORECAST_CODE, forecast.getCode())
                    .param(ErpMfgErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpMfgErrors.ARG_EXPECTED_STATUS, ErpMfgConstants.FORECAST_STATUS_DRAFT);
        }
        forecast.setStatus(ErpMfgConstants.FORECAST_STATUS_APPROVED);
        updateEntity(forecast, null, context);
        return forecast;
    }

    @Override
    @BizMutation
    public ErpMfgForecast cancel(@Name("id") String id, IServiceContext context) {
        ErpMfgForecast forecast = requireEntity(id, null, context);
        String status = forecast.getStatus();
        if (Objects.equals(status, ErpMfgConstants.FORECAST_STATUS_CANCELLED)
                || Objects.equals(status, ErpMfgConstants.FORECAST_STATUS_CONSUMED)) {
            throw new NopException(ErpMfgErrors.ERR_FORECAST_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpMfgErrors.ARG_FORECAST_CODE, forecast.getCode())
                    .param(ErpMfgErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpMfgErrors.ARG_EXPECTED_STATUS,
                            ErpMfgConstants.FORECAST_STATUS_DRAFT + "/" + ErpMfgConstants.FORECAST_STATUS_APPROVED);
        }
        forecast.setStatus(ErpMfgConstants.FORECAST_STATUS_CANCELLED);
        updateEntity(forecast, null, context);
        return forecast;
    }

}
