package app.erp.pur.service.entity;

import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 入库移动单请求构造器：将 {@link ErpPurReceive}+{@link ErpPurReceiveLine} 映射为库存域
 * {@link StockMoveRequest}（INCOMING），供 {@link ErpPurReceiveBizModel#approve} 调
 * {@code IErpInvStockMoveBiz.generateMove}。
 *
 * <p>权威：{@code docs/design/inventory/cross-domain.md} 的 {@code generateMove} 调用方契约。
 *
 * <p>关键映射（{@code docs/design/inventory/cross-domain.md} + Phase 2 Decision）：
 * <ul>
 *   <li>{@code moveType}=INCOMING(10)；{@code relatedBillType}={@link ErpPurConstants#RELATED_BILL_TYPE_PUR_RECEIVE}（幂等键）；
 *       {@code relatedBillCode}={@code receive.code}。</li>
 *   <li>{@code destWarehouseId}={@code receive.warehouseId}（入库目的仓），{@code sourceWarehouseId}=null（外部供应商无源仓）。</li>
 *   <li>{@code acctSchemaId}=按 {@code receive.orgId} 解析的核算账套（存货估值凭证行 acctSchemaId 非空约束所需）；
 *       未配置账套时为 null（过账失败由库存域吞异常、置 {@code posted=false}，不阻塞入库终态）。</li>
 *   <li>行：{@code quantity}=BigDecimal 直读（DECIMAL 列），{@code unitCost}={@code new BigDecimal(line.unitPrice)}
 *       解析 VARCHAR 单价（空则 null），{@code destLocationId}=null 走整仓余额（locationId=null 合法）。</li>
 * </ul>
 */
public class ReceiveStockMoveBuilder {

    @Inject
    IDaoProvider daoProvider;

    public StockMoveRequest build(ErpPurReceive receive, List<ErpPurReceiveLine> lines) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpPurConstants.MOVE_TYPE_INCOMING);
        request.setOrgId(receive.getOrgId());
        request.setBusinessDate(receive.getBusinessDate());
        request.setDestWarehouseId(receive.getWarehouseId());
        request.setSourceWarehouseId(null);
        request.setAcctSchemaId(resolveAcctSchemaId(receive.getOrgId()));
        request.setCurrencyId(receive.getCurrencyId());
        request.setRelatedBillType(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE);
        request.setRelatedBillCode(receive.getCode());
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

    private List<StockMoveLineRequest> buildLines(List<ErpPurReceiveLine> lines) {
        List<StockMoveLineRequest> result = new ArrayList<>(lines.size());
        for (ErpPurReceiveLine line : lines) {
            StockMoveLineRequest req = new StockMoveLineRequest();
            req.setMaterialId(line.getMaterialId());
            req.setSkuId(line.getSkuId());
            req.setUoMId(line.getUoMId());
            req.setQuantity(line.getQuantity());
            req.setUnitCost(parseUnitPrice(line.getUnitPrice()));
            req.setBatchNo(line.getBatchNo());
            result.add(req);
        }
        return result;
    }

    private BigDecimal parseUnitPrice(String unitPrice) {
        if (StringHelper.isBlank(unitPrice)) {
            return null;
        }
        try {
            return new BigDecimal(unitPrice.trim());
        } catch (NumberFormatException e) {
            throw new NopException(ErpPurErrors.ERR_INVALID_UNIT_PRICE)
                    .param(ErpPurErrors.ARG_PRICE_TEXT, unitPrice);
        }
    }
}
