package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.dao.entity.ErpAstInventoryLine;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * ErpAstInventory processVariance per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含差异处置编排（盘盈建卡/盘亏置 SCRAPPED）；共享 protected helper 单一真相源在 {@link ErpAstInventoryProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstInventoryProcessVarianceProcessor {

    @Inject
    ErpAstInventoryProcessor facade;

    public ErpAstInventory processVariance(String id, IServiceContext context) {
        ErpAstInventory inv = facade.requireInventory(id, context);
        facade.validateReconciling(inv);
        List<ErpAstInventoryLine> lines = facade.findLines(inv.getId());
        for (ErpAstInventoryLine line : lines) {
            facade.handleLineVariance(inv, line, context);
            facade.lineDao().saveOrUpdateEntity(line);
        }
        return inv;
    }
}
