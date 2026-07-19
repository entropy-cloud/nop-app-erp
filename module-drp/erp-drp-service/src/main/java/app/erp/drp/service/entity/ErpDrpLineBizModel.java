package app.erp.drp.service.entity;

import java.util.List;
import java.util.Objects;
import app.erp.drp.biz.IErpDrpLineBiz;
import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.drp.DrpReleaseService;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * DRP 明细行 BizModel。薄委派层：{@link #releaseLine}/{@link #releaseApproved} 委派给 {@link DrpReleaseService}；
 * 行级状态迁移 {@link #approveLine}/{@link #rejectLine}/{@link #cancelLine} 实现状态机 SUGGESTED→APPROVED→ORDERED/CANCELLED。
 */
@BizModel("ErpDrpLine")
public class ErpDrpLineBizModel extends CrudBizModel<ErpDrpLine> implements IErpDrpLineBiz {

    @Inject
    DrpReleaseService drpReleaseService;

    public ErpDrpLineBizModel() {
        setEntityName(ErpDrpLine.class.getName());
    }

    public void setDrpReleaseService(DrpReleaseService drpReleaseService) {
        this.drpReleaseService = drpReleaseService;
    }

    @Override
    @BizMutation
    public ErpDrpLine releaseLine(@Name("lineId") Long lineId, IServiceContext context) {
        drpReleaseService.releaseLine(lineId);
        return get(String.valueOf(lineId), false, context);
    }

    @Override
    @BizMutation
    public ErpDrpPlan releaseApproved(@Name("planId") Long planId, IServiceContext context) {
        drpReleaseService.releaseApproved(planId);
        return null;
    }

    @Override
    @BizMutation
    public ErpDrpLine approveLine(@Name("lineId") Long lineId, IServiceContext context) {
        ErpDrpLine line = requireEntity(String.valueOf(lineId), null, context);
        if (!Objects.equals(line.getStatus(), ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_ILLEGAL_TRANSITION)
                    .param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId)
                    .param(ErpDrpErrors.ARG_CURRENT_STATUS, line.getStatus());
        }
        line.setStatus(ErpDrpConstants.DRP_LINE_STATUS_APPROVED);
        updateEntity(line, null, context);
        return line;
    }

    @Override
    @BizMutation
    public ErpDrpLine rejectLine(@Name("lineId") Long lineId, IServiceContext context) {
        return doCancel(lineId, context);
    }

    @Override
    @BizMutation
    public ErpDrpLine cancelLine(@Name("lineId") Long lineId, IServiceContext context) {
        return doCancel(lineId, context);
    }

    private ErpDrpLine doCancel(Long lineId, IServiceContext context) {
        ErpDrpLine line = requireEntity(String.valueOf(lineId), null, context);
        String status = line.getStatus();
        if (Objects.equals(status, ErpDrpConstants.DRP_LINE_STATUS_ORDERED)
                || Objects.equals(status, ErpDrpConstants.DRP_LINE_STATUS_CANCELLED)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_ILLEGAL_TRANSITION)
                    .param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId)
                    .param(ErpDrpErrors.ARG_CURRENT_STATUS, status);
        }
        line.setStatus(ErpDrpConstants.DRP_LINE_STATUS_CANCELLED);
        updateEntity(line, null, context);
        return line;
    }

}
