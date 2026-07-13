
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsContractBiz;
import app.erp.cs.dao.entity.ErpCsContract;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCsContract")
public class ErpCsContractBizModel extends CrudBizModel<ErpCsContract> implements IErpCsContractBiz{
    public ErpCsContractBizModel(){
        setEntityName(ErpCsContract.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCsContract> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpCsContract entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }


    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCsContract.class)
    public List<String> orgName(@ContextSource List<ErpCsContract> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsContract row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCsContract.class)
    public List<String> partnerName(@ContextSource List<ErpCsContract> rows) {
        orm().batchLoadProps(rows, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsContract row : rows) {
            result.add(row.orm_attached() && row.getPartner() != null ? row.getPartner().getName() : null);
        }
        return result;
    }

}
