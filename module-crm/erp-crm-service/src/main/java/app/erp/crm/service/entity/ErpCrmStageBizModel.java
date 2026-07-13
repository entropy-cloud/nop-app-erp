
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmStageBiz;
import app.erp.crm.dao.entity.ErpCrmStage;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCrmStage")
public class ErpCrmStageBizModel extends CrudBizModel<ErpCrmStage> implements IErpCrmStageBiz{
    public ErpCrmStageBizModel(){
        setEntityName(ErpCrmStage.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmStage.class)
    public List<String> orgName(@ContextSource List<ErpCrmStage> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmStage row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmStage.class)
    public List<String> teamName(@ContextSource List<ErpCrmStage> rows) {
        orm().batchLoadProps(rows, Collections.singleton("team"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmStage row : rows) {
            result.add(row.orm_attached() && row.getTeam() != null ? row.getTeam().getName() : null);
        }
        return result;
    }

}
