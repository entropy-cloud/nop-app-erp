
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bAsnLineBiz;
import app.erp.b2b.dao.entity.ErpB2bAsnLine;

@BizModel("ErpB2bAsnLine")
public class ErpB2bAsnLineBizModel extends CrudBizModel<ErpB2bAsnLine> implements IErpB2bAsnLineBiz{
    public ErpB2bAsnLineBizModel(){
        setEntityName(ErpB2bAsnLine.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpB2bAsnLine.class)
    public List<String> asnName(@ContextSource List<ErpB2bAsnLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("asn"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bAsnLine row : rows) {
            result.add(row.orm_attached() && row.getAsn() != null ? row.getAsn().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpB2bAsnLine.class)
    public List<String> materialName(@ContextSource List<ErpB2bAsnLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("material"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bAsnLine row : rows) {
            result.add(row.orm_attached() && row.getMaterial() != null ? row.getMaterial().getName() : null);
        }
        return result;
    }

}
