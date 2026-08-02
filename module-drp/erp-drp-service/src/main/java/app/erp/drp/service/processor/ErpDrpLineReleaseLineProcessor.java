package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.drp.DrpReleaseService;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpDrpLine releaseLine per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含单行释放编排（APPROVED→ORDERED）：委派 {@link DrpReleaseService#releaseLine}（生成调拨/采购单 + 回写 orderBillCode）+ 回读行。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpDrpLineReleaseLineProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    DrpReleaseService drpReleaseService;

    public ErpDrpLine releaseLine(Long lineId, IServiceContext context) {
        drpReleaseService.releaseLine(lineId);
        return requireLine(lineId);
    }

    // ---------- 内部辅助 ----------

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
