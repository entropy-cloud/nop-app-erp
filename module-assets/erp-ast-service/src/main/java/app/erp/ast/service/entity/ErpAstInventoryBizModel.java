
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstInventoryBiz;
import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.service.processor.ErpAstInventoryProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 资产盘点 BizModel（Facade）。所有动作经 xbiz 委托 {@link ErpAstInventoryProcessor} 全权处理。
 * 详见 owner doc {@code docs/design/assets/inventory.md}。
 */
@BizModel("ErpAstInventory")
public class ErpAstInventoryBizModel extends CrudBizModel<ErpAstInventory> implements IErpAstInventoryBiz {

    @Inject
    ErpAstInventoryProcessor inventoryProcessor;

    public ErpAstInventoryBizModel() {
        setEntityName(ErpAstInventory.class.getName());
    }

    @BizLoader(forType = ErpAstInventory.class)
    public List<String> orgName(@ContextSource List<ErpAstInventory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventory row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstInventory.class)
    public List<String> rangeDepartmentName(@ContextSource List<ErpAstInventory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("rangeDepartment"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventory row : rows) {
            result.add(row.getRangeDepartment() != null ? row.getRangeDepartment().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstInventory.class)
    public List<String> rangeCategoryName(@ContextSource List<ErpAstInventory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("rangeCategory"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventory row : rows) {
            result.add(row.getRangeCategory() != null ? row.getRangeCategory().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstInventory.class)
    public List<String> rangeLocationName(@ContextSource List<ErpAstInventory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("rangeLocation"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventory row : rows) {
            result.add(row.getRangeLocation() != null ? row.getRangeLocation().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstInventory.class)
    public List<String> responsibleByName(@ContextSource List<ErpAstInventory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("responsibleBy"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventory row : rows) {
            result.add(row.getResponsibleBy() != null ? row.getResponsibleBy().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstInventory.class)
    public List<String> currencyName(@ContextSource List<ErpAstInventory> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventory row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }

    @Override
    @BizMutation
    public ErpAstInventory createInventory(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.createInventory(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory submitForCount(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.submitForCount(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory reconcile(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.reconcile(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory processVariance(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.processVariance(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory approve(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.approve(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory post(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.post(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory cancel(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.cancel(id, context);
    }

    @Override
    @BizMutation
    public ErpAstInventory reverse(@Name("id") Long id, IServiceContext context) {
        return inventoryProcessor.reverse(id, context);
    }
}
