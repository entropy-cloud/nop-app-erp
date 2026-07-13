
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtContractLineBiz;
import app.erp.contract.dao.entity.ErpCtContractLine;

@BizModel("ErpCtContractLine")
public class ErpCtContractLineBizModel extends CrudBizModel<ErpCtContractLine> implements IErpCtContractLineBiz{
    public ErpCtContractLineBizModel(){
        setEntityName(ErpCtContractLine.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCtContractLine.class)
    public List<String> contractName(@ContextSource List<ErpCtContractLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("contract"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtContractLine row : rows) {
            result.add(row.orm_attached() && row.getContract() != null ? row.getContract().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCtContractLine.class)
    public List<String> materialName(@ContextSource List<ErpCtContractLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("material"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtContractLine row : rows) {
            result.add(row.orm_attached() && row.getMaterial() != null ? row.getMaterial().getName() : null);
        }
        return result;
    }

}
