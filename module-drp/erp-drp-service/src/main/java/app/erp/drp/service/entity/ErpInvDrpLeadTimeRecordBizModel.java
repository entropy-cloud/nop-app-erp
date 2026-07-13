
package app.erp.drp.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpInvDrpLeadTimeRecordBiz;
import app.erp.drp.dao.entity.ErpInvDrpLeadTimeRecord;

@BizModel("ErpInvDrpLeadTimeRecord")
public class ErpInvDrpLeadTimeRecordBizModel extends CrudBizModel<ErpInvDrpLeadTimeRecord> implements IErpInvDrpLeadTimeRecordBiz{
    public ErpInvDrpLeadTimeRecordBizModel(){
        setEntityName(ErpInvDrpLeadTimeRecord.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpInvDrpLeadTimeRecord.class)
    public List<String> supplierName(@ContextSource List<ErpInvDrpLeadTimeRecord> rows) {
        orm().batchLoadProps(rows, Collections.singleton("supplier"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpLeadTimeRecord row : rows) {
            result.add(row.orm_attached() && row.getSupplier() != null ? row.getSupplier().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpLeadTimeRecord.class)
    public List<String> materialName(@ContextSource List<ErpInvDrpLeadTimeRecord> rows) {
        orm().batchLoadProps(rows, Collections.singleton("material"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpLeadTimeRecord row : rows) {
            result.add(row.orm_attached() && row.getMaterial() != null ? row.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpLeadTimeRecord.class)
    public List<String> orgName(@ContextSource List<ErpInvDrpLeadTimeRecord> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpLeadTimeRecord row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
