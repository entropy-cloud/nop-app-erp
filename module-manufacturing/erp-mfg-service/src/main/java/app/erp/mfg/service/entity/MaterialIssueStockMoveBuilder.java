package app.erp.mfg.service.entity;

import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.md.biz.IErpMdAcctSchemaBiz;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * 领料出库移动单请求构造器：将 {@link ErpMfgMaterialIssue}+{@link ErpMfgMaterialIssueLine} 映射为库存域
 * {@link StockMoveRequest}（MANUFACTURING 出库方向），供 {@link ErpMfgMaterialIssueBizModel#confirm} 调
 * {@code IErpInvStockMoveBiz.generateMove}。
 *
 * <p>权威：{@code docs/design/inventory/cross-domain.md} 的 {@code generateMove} 调用方契约。
 * 幂等键 {@code (ERP_MFG_ISSUE, issue.code)}。核算账套解析经 {@link IErpMdAcctSchemaBiz}（跨域只读经 I*Biz 管道）。
 */
public class MaterialIssueStockMoveBuilder {

    @Inject
    IErpMdAcctSchemaBiz mdAcctSchemaBiz;

    public StockMoveRequest build(ErpMfgMaterialIssue issue, List<ErpMfgMaterialIssueLine> lines,
                                  IServiceContext context) {
        StockMoveRequest request = new StockMoveRequest();
        // 领料为出库方向：库存域 bookCompletion 按 moveType 决定方向，OUTGOING(20) 扣减余额并写负号流水；
        // MANUFACTURING(40) 被库存域视为入库（仅完工入库用），故领料出库用 OUTGOING（偏离计划 moveType 字面，
        // 已记入 state-machine.md 补注；完工入库 Phase 4 用 MANUFACTURING 入库）。
        request.setMoveType(ErpMfgConstants.MOVE_TYPE_OUTGOING_ISSUE);
        request.setOrgId(issue.getOrgId());
        request.setBusinessDate(issue.getBusinessDate());
        // 领料为出库方向：源仓=发料仓库，目的仓为空（物料离开仓库进入工单消耗）
        request.setSourceWarehouseId(issue.getWarehouseId());
        request.setSourceLocationId(null);
        request.setAcctSchemaId(resolveAcctSchemaId(issue.getOrgId(), context));
        request.setCurrencyId(issue.getCurrencyId());
        request.setRelatedBillType(ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE);
        request.setRelatedBillCode(issue.getCode());
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

    private List<StockMoveLineRequest> buildLines(List<ErpMfgMaterialIssueLine> lines) {
        List<StockMoveLineRequest> result = new ArrayList<>(lines.size());
        for (ErpMfgMaterialIssueLine line : lines) {
            StockMoveLineRequest req = new StockMoveLineRequest();
            req.setMaterialId(line.getMaterialId());
            req.setSkuId(line.getSkuId());
            req.setUoMId(line.getUoMId());
            // 实领数量驱动出库（领料确认即出库）
            req.setQuantity(line.getIssuedQuantity() != null ? line.getIssuedQuantity() : line.getRequiredQuantity());
            req.setBatchNo(line.getBatchNo());
            req.setSourceLocationId(line.getLocationId());
            result.add(req);
        }
        return result;
    }
}
