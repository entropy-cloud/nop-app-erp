
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bEdiFormatBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiFormat;

@BizModel("ErpB2bEdiFormat")
public class ErpB2bEdiFormatBizModel extends CrudBizModel<ErpB2bEdiFormat> implements IErpB2bEdiFormatBiz{
    public ErpB2bEdiFormatBizModel(){
        setEntityName(ErpB2bEdiFormat.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpB2bEdiFormat.class)
    public List<String> orgName(@ContextSource List<ErpB2bEdiFormat> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bEdiFormat row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
