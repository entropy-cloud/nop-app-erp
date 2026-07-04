
package app.erp.aps.service.entity;

import app.erp.aps.biz.IErpApsScheduleBiz;
import app.erp.aps.dao.entity.ErpApsSchedule;
import app.erp.aps.service.ErpApsConstants;
import app.erp.aps.service.ErpApsErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.util.Objects;

/**
 * 排产方案状态机（{@code scheduling.md §九}）：DRAFT→PUBLISHED→ARCHIVED。
 *
 * <p>{@code publish}：DRAFT→PUBLISHED，锁定为执行参照；{@code archive}：DRAFT|PUBLISHED→ARCHIVED。
 * 状态迁移非法时抛 {@code ERR_APS_SCHEDULE_ILLEGAL_STATUS}。
 */
@BizModel("ErpApsSchedule")
public class ErpApsScheduleBizModel extends CrudBizModel<ErpApsSchedule> implements IErpApsScheduleBiz {

    public ErpApsScheduleBizModel() {
        setEntityName(ErpApsSchedule.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpApsSchedule publish(@Name("id") Long id, IServiceContext context) {
        ErpApsSchedule schedule = this.get(String.valueOf(id), false, context);
        if (schedule == null) {
            throw new NopException(ErpApsErrors.ERR_APS_SCHEDULE_NOT_FOUND)
                    .param(ErpApsErrors.ARG_SCHEDULE_ID, id);
        }
        if (!Objects.equals(schedule.getStatus(), ErpApsConstants.SCHEDULE_STATUS_DRAFT)) {
            throw new NopException(ErpApsErrors.ERR_APS_SCHEDULE_ILLEGAL_STATUS)
                    .param(ErpApsErrors.ARG_SCHEDULE_ID, id)
                    .param(ErpApsErrors.ARG_CURRENT_STATUS, schedule.getStatus());
        }
        schedule.setStatus(ErpApsConstants.SCHEDULE_STATUS_PUBLISHED);
        this.updateEntity(schedule, null, context);
        return schedule;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpApsSchedule archive(@Name("id") Long id, IServiceContext context) {
        ErpApsSchedule schedule = this.get(String.valueOf(id), false, context);
        if (schedule == null) {
            throw new NopException(ErpApsErrors.ERR_APS_SCHEDULE_NOT_FOUND)
                    .param(ErpApsErrors.ARG_SCHEDULE_ID, id);
        }
        String status = schedule.getStatus();
        if (Objects.equals(status, ErpApsConstants.SCHEDULE_STATUS_ARCHIVED)) {
            return schedule;
        }
        if (!Objects.equals(status, ErpApsConstants.SCHEDULE_STATUS_DRAFT)
                && !Objects.equals(status, ErpApsConstants.SCHEDULE_STATUS_PUBLISHED)) {
            throw new NopException(ErpApsErrors.ERR_APS_SCHEDULE_ILLEGAL_STATUS)
                    .param(ErpApsErrors.ARG_SCHEDULE_ID, id)
                    .param(ErpApsErrors.ARG_CURRENT_STATUS, status);
        }
        schedule.setStatus(ErpApsConstants.SCHEDULE_STATUS_ARCHIVED);
        this.updateEntity(schedule, null, context);
        return schedule;
    }
}
