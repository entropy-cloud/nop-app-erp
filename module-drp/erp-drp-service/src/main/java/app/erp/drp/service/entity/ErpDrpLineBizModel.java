package app.erp.drp.service.entity;

import java.util.Objects;
import app.erp.drp.biz.IErpDrpLineBiz;
import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.processor.ErpDrpLineCancelLineProcessor;
import app.erp.drp.service.processor.ErpDrpLineReleaseApprovedProcessor;
import app.erp.drp.service.processor.ErpDrpLineReleaseLineProcessor;
import app.erp.drp.service.processor.ErpDrpLineRejectLineProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * DRP 明细行 BizModel。薄委派层（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）：
 * {@link #releaseLine}/{@link #releaseApproved}/{@link #rejectLine}/{@link #cancelLine} 各委派独立自包含 Processor；
 * {@link #approveLine}（单步状态迁移 SUGGESTED→APPROVED）保留内联实现。
 */
@BizModel("ErpDrpLine")
public class ErpDrpLineBizModel extends CrudBizModel<ErpDrpLine> implements IErpDrpLineBiz {

    @Inject
    ErpDrpLineReleaseLineProcessor releaseLineProcessor;
    @Inject
    ErpDrpLineReleaseApprovedProcessor releaseApprovedProcessor;
    @Inject
    ErpDrpLineRejectLineProcessor rejectLineProcessor;
    @Inject
    ErpDrpLineCancelLineProcessor cancelLineProcessor;

    public ErpDrpLineBizModel() {
        setEntityName(ErpDrpLine.class.getName());
    }

    @Override
    @BizMutation
    public ErpDrpLine releaseLine(@Name("lineId") Long lineId, IServiceContext context) {
        return releaseLineProcessor.releaseLine(lineId, context);
    }

    @Override
    @BizMutation
    public app.erp.drp.dao.entity.ErpDrpPlan releaseApproved(@Name("planId") Long planId, IServiceContext context) {
        return releaseApprovedProcessor.releaseApproved(planId, context);
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
        return rejectLineProcessor.rejectLine(lineId, context);
    }

    @Override
    @BizMutation
    public ErpDrpLine cancelLine(@Name("lineId") Long lineId, IServiceContext context) {
        return cancelLineProcessor.cancelLine(lineId, context);
    }
}
