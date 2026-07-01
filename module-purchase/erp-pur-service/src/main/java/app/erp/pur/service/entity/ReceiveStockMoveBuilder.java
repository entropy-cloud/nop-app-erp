package app.erp.pur.service.entity;

import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.md.biz.IErpMdAcctSchemaBiz;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.service.ErpPurConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * 入库移动单请求构造器：将 {@link ErpPurReceive}+{@link ErpPurReceiveLine} 映射为库存域
 * {@link StockMoveRequest}（INCOMING），供 {@link ErpPurReceiveBizModel#approve} 调
 * {@code IErpInvStockMoveBiz.generateMove}。
 *
 * <p>权威：{@code docs/design/inventory/cross-domain.md} 的 {@code generateMove} 调用方契约。
 *
 * <p>核算账套解析经 {@link IErpMdAcctSchemaBiz}（跨域只读经 I*Biz 管道，对齐 service-layer 跨实体访问规则）。
 */
public class ReceiveStockMoveBuilder {

    @Inject
    IErpMdAcctSchemaBiz mdAcctSchemaBiz;

    public StockMoveRequest build(ErpPurReceive receive, List<ErpPurReceiveLine> lines, IServiceContext context) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpPurConstants.MOVE_TYPE_INCOMING);
        request.setOrgId(receive.getOrgId());
        request.setBusinessDate(receive.getBusinessDate());
        request.setDestWarehouseId(receive.getWarehouseId());
        request.setSourceWarehouseId(null);
        request.setAcctSchemaId(resolveAcctSchemaId(receive.getOrgId(), context));
        request.setCurrencyId(receive.getCurrencyId());
        request.setRelatedBillType(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE);
        request.setRelatedBillCode(receive.getCode());
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

    private List<StockMoveLineRequest> buildLines(List<ErpPurReceiveLine> lines) {
        List<StockMoveLineRequest> result = new ArrayList<>(lines.size());
        for (ErpPurReceiveLine line : lines) {
            StockMoveLineRequest req = new StockMoveLineRequest();
            req.setMaterialId(line.getMaterialId());
            req.setSkuId(line.getSkuId());
            req.setUoMId(line.getUoMId());
            req.setQuantity(line.getQuantity());
            req.setUnitCost(line.getUnitPrice());
            req.setBatchNo(line.getBatchNo());
            result.add(req);
        }
        return result;
    }
}
