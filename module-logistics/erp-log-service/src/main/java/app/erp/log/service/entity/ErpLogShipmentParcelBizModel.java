
package app.erp.log.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.log.biz.IErpLogShipmentParcelBiz;
import app.erp.log.dao.entity.ErpLogShipmentParcel;

@BizModel("ErpLogShipmentParcel")
public class ErpLogShipmentParcelBizModel extends CrudBizModel<ErpLogShipmentParcel> implements IErpLogShipmentParcelBiz{
    public ErpLogShipmentParcelBizModel(){
        setEntityName(ErpLogShipmentParcel.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpLogShipmentParcel.class)
    public List<String> shipmentName(@ContextSource List<ErpLogShipmentParcel> rows) {
        orm().batchLoadProps(rows, Collections.singleton("shipment"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpLogShipmentParcel row : rows) {
            result.add(row.orm_attached() && row.getShipment() != null ? row.getShipment().getCode() : null);
        }
        return result;
    }

}
