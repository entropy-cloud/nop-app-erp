
package app.erp.drp.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpInvDrpDockAppointmentBiz;
import app.erp.drp.dao.entity.ErpInvDrpDockAppointment;

@BizModel("ErpInvDrpDockAppointment")
public class ErpInvDrpDockAppointmentBizModel extends CrudBizModel<ErpInvDrpDockAppointment> implements IErpInvDrpDockAppointmentBiz{
    public ErpInvDrpDockAppointmentBizModel(){
        setEntityName(ErpInvDrpDockAppointment.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpInvDrpDockAppointment.class)
    public List<String> warehouseName(@ContextSource List<ErpInvDrpDockAppointment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpDockAppointment row : rows) {
            result.add(row.orm_attached() && row.getWarehouse() != null ? row.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpDockAppointment.class)
    public List<String> dockName(@ContextSource List<ErpInvDrpDockAppointment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("dock"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpDockAppointment row : rows) {
            result.add(row.orm_attached() && row.getDock() != null ? row.getDock().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpDockAppointment.class)
    public List<String> crossDockName(@ContextSource List<ErpInvDrpDockAppointment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("crossDock"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpDockAppointment row : rows) {
            result.add(row.orm_attached() && row.getCrossDock() != null ? row.getCrossDock().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpDockAppointment.class)
    public List<String> orgName(@ContextSource List<ErpInvDrpDockAppointment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpDockAppointment row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
