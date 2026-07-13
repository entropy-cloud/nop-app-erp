
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtDocumentBiz;
import app.erp.contract.dao.entity.ErpCtDocument;

@BizModel("ErpCtDocument")
public class ErpCtDocumentBizModel extends CrudBizModel<ErpCtDocument> implements IErpCtDocumentBiz{
    public ErpCtDocumentBizModel(){
        setEntityName(ErpCtDocument.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCtDocument.class)
    public List<String> contractName(@ContextSource List<ErpCtDocument> rows) {
        orm().batchLoadProps(rows, Collections.singleton("contract"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtDocument row : rows) {
            result.add(row.orm_attached() && row.getContract() != null ? row.getContract().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCtDocument.class)
    public List<String> orgName(@ContextSource List<ErpCtDocument> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtDocument row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
