
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtConsumptionLineBiz;
import app.erp.contract.dao.entity.ErpCtConsumptionLine;

@BizModel("ErpCtConsumptionLine")
public class ErpCtConsumptionLineBizModel extends CrudBizModel<ErpCtConsumptionLine> implements IErpCtConsumptionLineBiz{
    public ErpCtConsumptionLineBizModel(){
        setEntityName(ErpCtConsumptionLine.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCtConsumptionLine.class)
    public List<String> contractLineName(@ContextSource List<ErpCtConsumptionLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("contractLine"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtConsumptionLine row : rows) {
            result.add(row.orm_attached() && row.getContractLine() != null ? row.getContractLine().getDescription() : null);
        }
        return result;
    }

}
