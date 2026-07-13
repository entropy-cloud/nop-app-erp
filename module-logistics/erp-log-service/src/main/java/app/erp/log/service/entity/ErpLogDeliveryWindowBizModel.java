
package app.erp.log.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.log.biz.IErpLogDeliveryWindowBiz;
import app.erp.log.dao.entity.ErpLogDeliveryWindow;

@BizModel("ErpLogDeliveryWindow")
public class ErpLogDeliveryWindowBizModel extends CrudBizModel<ErpLogDeliveryWindow> implements IErpLogDeliveryWindowBiz{
    public ErpLogDeliveryWindowBizModel(){
        setEntityName(ErpLogDeliveryWindow.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpLogDeliveryWindow.class)
    public List<String> partnerName(@ContextSource List<ErpLogDeliveryWindow> rows) {
        orm().batchLoadProps(rows, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpLogDeliveryWindow row : rows) {
            result.add(row.orm_attached() && row.getPartner() != null ? row.getPartner().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpLogDeliveryWindow.class)
    public List<String> orgName(@ContextSource List<ErpLogDeliveryWindow> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpLogDeliveryWindow row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
