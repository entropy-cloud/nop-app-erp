
package app.erp.md.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdAcctSchemaCoaBiz;
import app.erp.md.dao.entity.ErpMdAcctSchemaCoa;

@BizModel("ErpMdAcctSchemaCoa")
public class ErpMdAcctSchemaCoaBizModel extends CrudBizModel<ErpMdAcctSchemaCoa> implements IErpMdAcctSchemaCoaBiz{
    public ErpMdAcctSchemaCoaBizModel(){
        setEntityName(ErpMdAcctSchemaCoa.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpMdAcctSchemaCoa.class)
    public List<String> acctSchemaName(@ContextSource List<ErpMdAcctSchemaCoa> rows) {
        orm().batchLoadProps(rows, Collections.singleton("acctSchema"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdAcctSchemaCoa row : rows) {
            result.add(row.orm_attached() && row.getAcctSchema() != null ? row.getAcctSchema().getName() : null);
        }
        return result;
    }

}
