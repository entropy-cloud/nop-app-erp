
package app.erp.log.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.log.biz.IErpLogCarrierConfigBiz;
import app.erp.log.dao.entity.ErpLogCarrierConfig;

@BizModel("ErpLogCarrierConfig")
public class ErpLogCarrierConfigBizModel extends CrudBizModel<ErpLogCarrierConfig> implements IErpLogCarrierConfigBiz{
    public ErpLogCarrierConfigBizModel(){
        setEntityName(ErpLogCarrierConfig.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpLogCarrierConfig.class)
    public List<String> carrierName(@ContextSource List<ErpLogCarrierConfig> rows) {
        orm().batchLoadProps(rows, Collections.singleton("carrier"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpLogCarrierConfig row : rows) {
            result.add(row.orm_attached() && row.getCarrier() != null ? row.getCarrier().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpLogCarrierConfig.class)
    public List<String> orgName(@ContextSource List<ErpLogCarrierConfig> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpLogCarrierConfig row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
