package app.erp.sal.service.entity;

import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.md.biz.IErpMdAcctSchemaBiz;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import app.erp.sal.service.ErpSalConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * 退货入库移动单请求构造器：将 {@link ErpSalReturn}+{@link ErpSalReturnLine} 映射为库存域
 * {@link StockMoveRequest}（INCOMING，反向入库），供 {@code ErpSalReturnBizModel.approve} 调
 * {@code IErpInvStockMoveBiz.generateMove}。
 *
 * <p>权威：{@code docs/design/inventory/cross-domain.md} 的 {@code generateMove} 调用方契约；
 * 退货入库方向见 {@code docs/design/sales/returns.md §与库存域协作}（方向：INCOMING，库存增加）。
 *
 * <p>幂等键 {@code (ERP_SAL_RETURN, return.code)} 由 {@code generateMove} 防重复触发。目的仓=退货仓库，
 * 源仓为空（入库）。单位成本取退货行单价（按原出库成本冲减存货估值口径）。
 *
 * <p>核算账套解析经 {@link IErpMdAcctSchemaBiz}（跨域只读经 I*Biz 管道）。
 */
public class ReturnStockMoveBuilder {

    @Inject
    IErpMdAcctSchemaBiz mdAcctSchemaBiz;

    public StockMoveRequest build(ErpSalReturn returnOrder, List<ErpSalReturnLine> lines, IServiceContext context) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpSalConstants.MOVE_TYPE_INCOMING);
        request.setOrgId(returnOrder.getOrgId());
        request.setBusinessDate(returnOrder.getBusinessDate());
        request.setSourceWarehouseId(null);
        request.setDestWarehouseId(returnOrder.getWarehouseId());
        request.setAcctSchemaId(resolveAcctSchemaId(returnOrder.getOrgId(), context));
        request.setCurrencyId(returnOrder.getCurrencyId());
        request.setRelatedBillType(ErpSalConstants.RELATED_BILL_TYPE_SAL_RETURN);
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

    private List<StockMoveLineRequest> buildLines(List<ErpSalReturnLine> lines) {
        List<StockMoveLineRequest> result = new ArrayList<>(lines.size());
        for (ErpSalReturnLine line : lines) {
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
