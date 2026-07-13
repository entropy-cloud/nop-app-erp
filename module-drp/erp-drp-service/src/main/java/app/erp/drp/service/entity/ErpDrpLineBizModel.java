package app.erp.drp.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import app.erp.drp.biz.IErpDrpLineBiz;
import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.drp.DrpReleaseService;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * DRP 明细行 BizModel。薄委派层：{@link #releaseLine}/{@link #releaseApproved} 委派给 {@link DrpReleaseService}。
 */
@BizModel("ErpDrpLine")
public class ErpDrpLineBizModel extends CrudBizModel<ErpDrpLine> implements IErpDrpLineBiz {

    @Inject
    DrpReleaseService drpReleaseService;

    public ErpDrpLineBizModel() {
        setEntityName(ErpDrpLine.class.getName());
    }

    public void setDrpReleaseService(DrpReleaseService drpReleaseService) {
        this.drpReleaseService = drpReleaseService;
    }

    @Override
    @BizMutation
    public ErpDrpLine releaseLine(@Name("lineId") Long lineId, IServiceContext context) {
        drpReleaseService.releaseLine(lineId);
        return get(String.valueOf(lineId), false, context);
    }

    @Override
    @BizMutation
    public ErpDrpPlan releaseApproved(@Name("planId") Long planId, IServiceContext context) {
        drpReleaseService.releaseApproved(planId);
        return null;
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpDrpLine.class)
    public List<String> planName(@ContextSource List<ErpDrpLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("plan"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpLine row : rows) {
            result.add(row.orm_attached() && row.getPlan() != null ? row.getPlan().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpDrpLine.class)
    public List<String> materialName(@ContextSource List<ErpDrpLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("material"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpLine row : rows) {
            result.add(row.orm_attached() && row.getMaterial() != null ? row.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpDrpLine.class)
    public List<String> warehouseName(@ContextSource List<ErpDrpLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpLine row : rows) {
            result.add(row.orm_attached() && row.getWarehouse() != null ? row.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpDrpLine.class)
    public List<String> sourceWarehouseName(@ContextSource List<ErpDrpLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("sourceWarehouse"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpLine row : rows) {
            result.add(row.orm_attached() && row.getSourceWarehouse() != null ? row.getSourceWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpDrpLine.class)
    public List<String> orgName(@ContextSource List<ErpDrpLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpDrpLine row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
