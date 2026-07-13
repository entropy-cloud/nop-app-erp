
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bEdiLogBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiLog;

@BizModel("ErpB2bEdiLog")
public class ErpB2bEdiLogBizModel extends CrudBizModel<ErpB2bEdiLog> implements IErpB2bEdiLogBiz{
    public ErpB2bEdiLogBizModel(){
        setEntityName(ErpB2bEdiLog.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpB2bEdiLog.class)
    public List<String> ediDocName(@ContextSource List<ErpB2bEdiLog> rows) {
        orm().batchLoadProps(rows, Collections.singleton("ediDoc"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bEdiLog row : rows) {
            result.add(row.orm_attached() && row.getEdiDoc() != null ? row.getEdiDoc().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpB2bEdiLog.class)
    public List<String> orgName(@ContextSource List<ErpB2bEdiLog> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bEdiLog row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
