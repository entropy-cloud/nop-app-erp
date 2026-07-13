
package app.erp.aps.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
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
import io.nop.biz.crud.EntityData;

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
    protected void defaultPrepareSave(EntityData<ErpApsSchedule> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpApsSchedule entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
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

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpApsSchedule.class)
    public List<String> orgName(@ContextSource List<ErpApsSchedule> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpApsSchedule row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
