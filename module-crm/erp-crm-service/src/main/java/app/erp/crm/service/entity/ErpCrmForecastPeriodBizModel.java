package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmForecastPeriodBiz;
import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.processor.ErpCrmForecastPeriodClosePeriodProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import app.erp.common.service.AbstractErpCrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * 预测期间 BizModel。期间状态机：{@code OPEN → FROZEN}（锁定不再重算）/ {@code OPEN → CLOSED}（关闭后触发准确率计算，
 * config-gated {@code erp-crm.forecast.accuracy-auto-compute}）。FROZEN/CLOSED 为终态，拒绝状态回退。
 * {@code closePeriod} 委托 {@link ErpCrmForecastPeriodClosePeriodProcessor}；{@code freeze} 仍内联。
 *
 * <p>对齐 {@code docs/design/crm/sales-forecast.md §状态机}。
 */
@BizModel("ErpCrmForecastPeriod")
public class ErpCrmForecastPeriodBizModel extends AbstractErpCrudBizModel<ErpCrmForecastPeriod>
        implements IErpCrmForecastPeriodBiz {

    @Inject
    ErpCrmForecastPeriodClosePeriodProcessor closePeriodProcessor;

    public ErpCrmForecastPeriodBizModel() {
        setEntityName(ErpCrmForecastPeriod.class.getName());
    }

    /**
     * 创建路径守卫（P3-CK-crm-022-r3 修复面）：status 列 ORM 无 defaultValue，创建可携带任意 status
     * 直建 FROZEN/CLOSED 绕过 requireOpen 语义——强制初始态 OPEN（Lead/Event「初始态由创建路径写入」
     * 同族范式；crm2-012 update 侧 fixed 的创建侧承接）。
     */
    @Override
    protected void defaultPrepareSave(EntityData<ErpCrmForecastPeriod> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        entityData.getEntity().setStatus(ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN);
    }

    @Override
    @BizMutation
    public ErpCrmForecastPeriod freeze(@Name("periodId") String periodId, IServiceContext context) {
        ErpCrmForecastPeriod period = requirePeriod(periodId, context);
        requireOpen(period);
        period.setStatus(ErpCrmConstants.FORECAST_PERIOD_STATUS_FROZEN);
        updateEntity(period, null, context);
        return period;
    }

    @Override
    @BizMutation
    public ErpCrmForecastPeriod closePeriod(@Name("periodId") String periodId, IServiceContext context) {
        return closePeriodProcessor.closePeriod(periodId, context);
    }

    // ---------- 内部辅助 ----------

    protected ErpCrmForecastPeriod requirePeriod(String periodId, IServiceContext context) {
        ErpCrmForecastPeriod period = get(periodId, false, context);
        if (period == null) {
            throw new NopException(ErpCrmErrors.ERR_FORECAST_PERIOD_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_PERIOD_ID, periodId);
        }
        return period;
    }

    protected void requireOpen(ErpCrmForecastPeriod period) {
        if (!Objects.equals(period.getStatus(), ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN)) {
            throw new NopException(ErpCrmErrors.ERR_FORECAST_PERIOD_NOT_OPEN)
                    .param(ErpCrmErrors.ARG_PERIOD_ID, period.getId())
                    .param(ErpCrmErrors.ARG_CURRENT_STATUS, period.getStatus())
                    .param(ErpCrmErrors.ARG_EXPECTED_STATUS, ErpCrmConstants.FORECAST_PERIOD_STATUS_OPEN);
        }
    }

    

}
