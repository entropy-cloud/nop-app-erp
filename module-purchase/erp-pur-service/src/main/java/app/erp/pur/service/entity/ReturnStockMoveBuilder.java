package app.erp.pur.service.entity;

import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.md.biz.IErpMdAcctSchemaBiz;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.dao.entity.ErpPurReturnLine;
import app.erp.pur.service.ErpPurConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * 退货出库移动单请求构造器：将 {@link ErpPurReturn}+{@link ErpPurReturnLine} 映射为库存域
 * {@link StockMoveRequest}（OUTGOING，反向出库），供 {@code ErpPurReturnBizModel.approve} 调
 * {@code IErpInvStockMoveBiz.generateMove}。
 *
 * <p>权威：{@code docs/design/inventory/cross-domain.md} 的 {@code generateMove} 调用方契约；
 * 退货出库方向见 {@code docs/design/purchase/returns.md §与库存域协作}（方向：OUTGOING，库存减少）。
 *
 * <p>幂等键 {@code (ERP_PUR_RETURN, return.code)} 由 {@code generateMove} 防重复触发。源仓=退货仓库，
 * 目的仓为空（出库）。单位成本取退货行单价（冲减存货估值口径与入库一致）。
 *
 * <p>核算账套解析经 {@link IErpMdAcctSchemaBiz}（跨域只读经 I*Biz 管道）。
 */
public class ReturnStockMoveBuilder {

    @Inject
    IErpMdAcctSchemaBiz mdAcctSchemaBiz;

    public StockMoveRequest build(ErpPurReturn returnOrder, List<ErpPurReturnLine> lines, IServiceContext context) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpPurConstants.MOVE_TYPE_OUTGOING);
        request.setOrgId(returnOrder.getOrgId());
        request.setBusinessDate(returnOrder.getBusinessDate());
        request.setSourceWarehouseId(returnOrder.getWarehouseId());
        request.setDestWarehouseId(null);
        request.setAcctSchemaId(resolveAcctSchemaId(returnOrder.getOrgId(), context));
        request.setCurrencyId(returnOrder.getCurrencyId());
        request.setRelatedBillType(ErpPurConstants.RELATED_BILL_TYPE_PUR_RETURN);
        request.setRelatedBillCode(returnOrder.getCode());
        request.setLines(buildLines(lines));
        return request;
    }

    private Long resolveAcctSchemaId(Long orgId, IServiceContext context) {
        if (orgId == null) {
            return null;
        }
        ErpMdAcctSchema schema = mdAcctSchemaBiz.findFirstByOrg(orgId, context);
        return schema == null ? null : schema.getId();
    }

    private List<StockMoveLineRequest> buildLines(List<ErpPurReturnLine> lines) {
        List<StockMoveLineRequest> result = new ArrayList<>(lines.size());
        for (ErpPurReturnLine line : lines) {
            StockMoveLineRequest req = new StockMoveLineRequest();
            req.setMaterialId(line.getMaterialId());
            req.setSkuId(line.getSkuId());
            req.setUoMId(line.getUoMId());
            req.setQuantity(line.getQuantity());
            req.setUnitCost(line.getUnitPrice());
            result.add(req);
        }
        return result;
    }
}
