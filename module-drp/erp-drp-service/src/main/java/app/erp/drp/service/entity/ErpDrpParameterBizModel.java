
package app.erp.drp.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpDrpParameterBiz;
import app.erp.drp.dao.entity.ErpDrpParameter;

@BizModel("ErpDrpParameter")
public class ErpDrpParameterBizModel extends CrudBizModel<ErpDrpParameter> implements IErpDrpParameterBiz{
    public ErpDrpParameterBizModel(){
        setEntityName(ErpDrpParameter.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpDrpParameter.class)
    public List<String> warehouseName(@ContextSource List<ErpDrpParameter> rows) {
        orm().batchLoadProps(rows, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpParameter row : rows) {
            result.add(row.orm_attached() && row.getWarehouse() != null ? row.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpDrpParameter.class)
    public List<String> materialName(@ContextSource List<ErpDrpParameter> rows) {
        orm().batchLoadProps(rows, Collections.singleton("material"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpParameter row : rows) {
            result.add(row.orm_attached() && row.getMaterial() != null ? row.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpDrpParameter.class)
    public List<String> preferredSourceWarehouseName(@ContextSource List<ErpDrpParameter> rows) {
        orm().batchLoadProps(rows, Collections.singleton("preferredSourceWarehouse"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpParameter row : rows) {
            result.add(row.orm_attached() && row.getPreferredSourceWarehouse() != null ? row.getPreferredSourceWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpDrpParameter.class)
    public List<String> preferredSupplierName(@ContextSource List<ErpDrpParameter> rows) {
        orm().batchLoadProps(rows, Collections.singleton("preferredSupplier"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpParameter row : rows) {
            result.add(row.orm_attached() && row.getPreferredSupplier() != null ? row.getPreferredSupplier().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpDrpParameter.class)
    public List<String> orgName(@ContextSource List<ErpDrpParameter> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpParameter row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
