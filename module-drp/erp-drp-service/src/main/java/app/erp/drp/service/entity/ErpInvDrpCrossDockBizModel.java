
package app.erp.drp.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.drp.biz.IErpInvDrpCrossDockBiz;
import app.erp.drp.dao.entity.ErpInvDrpCrossDock;

@BizModel("ErpInvDrpCrossDock")
public class ErpInvDrpCrossDockBizModel extends CrudBizModel<ErpInvDrpCrossDock> implements IErpInvDrpCrossDockBiz{
    public ErpInvDrpCrossDockBizModel(){
        setEntityName(ErpInvDrpCrossDock.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpInvDrpCrossDock.class)
    public List<String> drpLineName(@ContextSource List<ErpInvDrpCrossDock> rows) {
        orm().batchLoadProps(rows, Collections.singleton("drpLine"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpCrossDock row : rows) {
            result.add(row.orm_attached() && row.getDrpLine() != null ? row.getDrpLine().getOrderBillCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpCrossDock.class)
    public List<String> materialName(@ContextSource List<ErpInvDrpCrossDock> rows) {
        orm().batchLoadProps(rows, Collections.singleton("material"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpCrossDock row : rows) {
            result.add(row.orm_attached() && row.getMaterial() != null ? row.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpCrossDock.class)
    public List<String> stagingLocationName(@ContextSource List<ErpInvDrpCrossDock> rows) {
        orm().batchLoadProps(rows, Collections.singleton("stagingLocation"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpCrossDock row : rows) {
            result.add(row.orm_attached() && row.getStagingLocation() != null ? row.getStagingLocation().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpCrossDock.class)
    public List<String> inboundMoveName(@ContextSource List<ErpInvDrpCrossDock> rows) {
        orm().batchLoadProps(rows, Collections.singleton("inboundMove"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpCrossDock row : rows) {
            result.add(row.orm_attached() && row.getInboundMove() != null ? row.getInboundMove().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpCrossDock.class)
    public List<String> outboundMoveName(@ContextSource List<ErpInvDrpCrossDock> rows) {
        orm().batchLoadProps(rows, Collections.singleton("outboundMove"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpCrossDock row : rows) {
            result.add(row.orm_attached() && row.getOutboundMove() != null ? row.getOutboundMove().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpInvDrpCrossDock.class)
    public List<String> orgName(@ContextSource List<ErpInvDrpCrossDock> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpInvDrpCrossDock row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
