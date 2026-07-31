package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpDrpLine cancelLine per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含行取消编排（SUGGESTED/APPROVED→CANCELLED）：回读行 + 终态门（ORDERED/CANCELLED 不可再取消）+ 置 CANCELLED。
 * 与 {@link ErpDrpLineRejectLineProcessor} 同语义（原 BizModel rejectLine/cancelLine 共用 doCancel），按自包含要求各自内联副本。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpDrpLineCancelLineProcessor {

    @Inject
    IDaoProvider daoProvider;

    public ErpDrpLine cancelLine(Long lineId, IServiceContext context) {
        return doCancel(lineId);
    }

    // ---------- 内部辅助 ----------

    protected ErpDrpLine doCancel(Long lineId) {
        ErpDrpLine line = requireLine(lineId);
        String status = line.getStatus();
        if (Objects.equals(status, ErpDrpConstants.DRP_LINE_STATUS_ORDERED)
                || Objects.equals(status, ErpDrpConstants.DRP_LINE_STATUS_CANCELLED)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_ILLEGAL_TRANSITION)
                    .param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId)
                    .param(ErpDrpErrors.ARG_CURRENT_STATUS, status);
        }
        line.setStatus(ErpDrpConstants.DRP_LINE_STATUS_CANCELLED);
        dao().updateEntity(line);
        return line;
    }

    protected ErpDrpLine requireLine(Long lineId) {
        ErpDrpLine line = dao().getEntityById(lineId);
        if (line == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_NOT_FOUND)
                    .param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId);
        }
        return line;
    }

    private IEntityDao<ErpDrpLine> dao() {
        return daoProvider.daoFor(ErpDrpLine.class);
    }
}
