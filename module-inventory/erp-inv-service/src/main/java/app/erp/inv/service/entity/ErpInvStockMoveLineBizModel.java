
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.inv.biz.IErpInvStockMoveLineBiz;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpInvStockMoveLine")
public class ErpInvStockMoveLineBizModel extends CrudBizModel<ErpInvStockMoveLine> implements IErpInvStockMoveLineBiz{
    public ErpInvStockMoveLineBizModel(){
        setEntityName(ErpInvStockMoveLine.class.getName());
    }

    @BizLoader(forType = ErpInvStockMoveLine.class)
    public List<String> materialName(@ContextSource List<ErpInvStockMoveLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpInvStockMoveLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }
}
