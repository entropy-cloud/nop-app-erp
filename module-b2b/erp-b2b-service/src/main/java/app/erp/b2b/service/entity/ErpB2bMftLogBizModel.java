
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bMftLogBiz;
import app.erp.b2b.dao.entity.ErpB2bMftLog;

@BizModel("ErpB2bMftLog")
public class ErpB2bMftLogBizModel extends CrudBizModel<ErpB2bMftLog> implements IErpB2bMftLogBiz{
    public ErpB2bMftLogBizModel(){
        setEntityName(ErpB2bMftLog.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpB2bMftLog.class)
    public List<String> configName(@ContextSource List<ErpB2bMftLog> rows) {
        orm().batchLoadProps(rows, Collections.singleton("config"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bMftLog row : rows) {
            result.add(row.orm_attached() && row.getConfig() != null ? row.getConfig().getProtocol() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpB2bMftLog.class)
    public List<String> orgName(@ContextSource List<ErpB2bMftLog> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bMftLog row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
