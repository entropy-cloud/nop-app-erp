package app.erp.sal.service.entity;

import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.md.biz.IErpMdAcctSchemaBiz;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.service.ErpSalConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * 出库移动单请求构造器：将 {@link ErpSalDelivery}+{@link ErpSalDeliveryLine} 映射为库存域
 * {@link StockMoveRequest}（OUTGOING），供 {@link ErpSalDeliveryBizModel#approve} 调
 * {@code IErpInvStockMoveBiz.generateMove}。
 *
 * <p>核算账套解析经 {@link IErpMdAcctSchemaBiz}（跨域只读经 I*Biz 管道，对齐 service-layer 跨实体访问规则）。
 */
public class DeliveryStockMoveBuilder {

    @Inject
    IErpMdAcctSchemaBiz mdAcctSchemaBiz;

    public StockMoveRequest build(ErpSalDelivery delivery, List<ErpSalDeliveryLine> lines, IServiceContext context) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpSalConstants.MOVE_TYPE_OUTGOING);
        request.setOrgId(delivery.getOrgId());
        request.setBusinessDate(delivery.getBusinessDate());
        request.setSourceWarehouseId(delivery.getWarehouseId());
        request.setDestWarehouseId(null);
        request.setAcctSchemaId(resolveAcctSchemaId(delivery.getOrgId(), context));
        request.setCurrencyId(delivery.getCurrencyId());
        request.setRelatedBillType(ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY);
        request.setRelatedBillCode(delivery.getCode());
        request.setLines(buildLines(lines));
        return request;
    }

    /**
     * 按组织解析核算账套 ID（账套为 master-data，orgId 匹配；经 {@link IErpMdAcctSchemaBiz} 只读查询）。
     */
    private Long resolveAcctSchemaId(Long orgId, IServiceContext context) {
        if (orgId == null) {
            return null;
        }
        ErpMdAcctSchema schema = mdAcctSchemaBiz.findFirstByOrg(orgId, context);
        return schema == null ? null : schema.getId();
    }

    private List<StockMoveLineRequest> buildLines(List<ErpSalDeliveryLine> lines) {
        List<StockMoveLineRequest> result = new ArrayList<>(lines.size());
        for (ErpSalDeliveryLine line : lines) {
            StockMoveLineRequest req = new StockMoveLineRequest();
            req.setMaterialId(line.getMaterialId());
            req.setSkuId(line.getSkuId());
            req.setUoMId(line.getUoMId());
            req.setQuantity(line.getQuantity());
            // 出库 unitCost 由库存域按移动加权平均 avgCost 快照（售价 unitPrice ≠ 存货成本，不得传入）。
            req.setBatchNo(line.getBatchNo());
            result.add(req);
        }
        return result;
    }
}
