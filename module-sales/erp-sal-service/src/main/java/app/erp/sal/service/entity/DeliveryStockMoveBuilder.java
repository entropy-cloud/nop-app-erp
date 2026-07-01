package app.erp.sal.service.entity;

import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 出库移动单请求构造器：将 {@link ErpSalDelivery}+{@link ErpSalDeliveryLine} 映射为库存域
 * {@link StockMoveRequest}（OUTGOING），供 {@link ErpSalDeliveryBizModel#approve} 调
 * {@code IErpInvStockMoveBiz.generateMove}。
 *
 * <p>权威：{@code docs/design/inventory/cross-domain.md} 的 {@code generateMove} 调用方契约 +
 * 销售独有差异（出库类 CONFIRM 校验可用量）。
 *
 * <p>关键映射（{@code docs/design/inventory/cross-domain.md} + Phase 2 Decision）：
 * <ul>
 *   <li>{@code moveType}=OUTGOING(20)；{@code relatedBillType}={@link ErpSalConstants#RELATED_BILL_TYPE_SAL_DELIVERY}（幂等键）；
 *       {@code relatedBillCode}={@code delivery.code}。</li>
 *   <li>{@code sourceWarehouseId}={@code delivery.warehouseId}（出库源仓），{@code destWarehouseId}=null（发往客户无目的仓）。</li>
 *   <li>{@code acctSchemaId}=按 {@code delivery.orgId} 解析的核算账套（存货估值凭证行 acctSchemaId 非空约束所需）；
 *       未配置账套时为 null（过账失败由库存域吞异常、置 {@code posted=false}，不阻塞出库终态）。</li>
 *   <li>行：{@code quantity}=BigDecimal 直读（DECIMAL 列），**不传 {@code unitCost}**（出库由库存域按移动加权平均
 *       {@code avgCost} 快照，售价 {@code unitPrice} 不得作为成本传入），{@code sourceLocationId}=null 走整仓余额
 *       （{@code deliveryLine.warehouseId} displayName 为「出库库位」但无关系声明、语义未定，按整仓余额 locationId=null 为安全默认）。</li>
 * </ul>
 */
public class DeliveryStockMoveBuilder {

    @Inject
    IDaoProvider daoProvider;

    public StockMoveRequest build(ErpSalDelivery delivery, List<ErpSalDeliveryLine> lines) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpSalConstants.MOVE_TYPE_OUTGOING);
        request.setOrgId(delivery.getOrgId());
        request.setBusinessDate(delivery.getBusinessDate());
        request.setSourceWarehouseId(delivery.getWarehouseId());
        request.setDestWarehouseId(null);
        request.setAcctSchemaId(resolveAcctSchemaId(delivery.getOrgId()));
        request.setCurrencyId(delivery.getCurrencyId());
        request.setRelatedBillType(ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY);
        request.setRelatedBillCode(delivery.getCode());
        request.setLines(buildLines(lines));
        return request;
    }

    /**
     * 按组织解析核算账套 ID（账套为 master-data，orgId 匹配；纯实体读故用 daoFor 直接查询）。
     */
    private Long resolveAcctSchemaId(Long orgId) {
        if (orgId == null) {
            return null;
        }
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        List<ErpMdAcctSchema> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
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
