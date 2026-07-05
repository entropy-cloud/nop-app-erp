package app.erp.mnt.service.support;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntSparePartUsageLine;
import app.erp.mnt.service.ErpMntConstants;
import app.erp.mnt.service.ErpMntErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import io.nop.api.core.time.CoreMetrics;

/**
 * 备件领料出库服务。按行构造 {@link StockMoveRequest}（OUTGOING，relatedBillType 非空自动 DONE 扣余额）
 * 调 {@link IErpInvStockMoveBiz#generateMove} 跨域出库，与 purchase/sales 跨域模式一致。
 */
public class SparePartIssueService {

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    public ErpInvStockMove issue(ErpMntSparePartUsage usage, List<ErpMntSparePartUsageLine> lines,
                                 IServiceContext context) {
        if (lines == null || lines.isEmpty()) {
            throw new NopException(ErpMntErrors.ERR_USAGE_LINES_EMPTY)
                    .param(ErpMntErrors.ARG_USAGE_CODE, usage.getCode());
        }

        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpMntConstants.MOVE_TYPE_OUTGOING);
        request.setOrgId(usage.getOrgId());
        request.setBusinessDate(usage.getBusinessDate() != null ? usage.getBusinessDate() : CoreMetrics.today());
        request.setSourceWarehouseId(usage.getWarehouseId());
        request.setRelatedBillType(ErpMntConstants.RELATED_BILL_TYPE_MNT_SPARE_PART);
        request.setRelatedBillCode(usage.getCode());

        List<StockMoveLineRequest> lineRequests = new ArrayList<>();
        for (ErpMntSparePartUsageLine line : lines) {
            StockMoveLineRequest lr = new StockMoveLineRequest();
            lr.setMaterialId(line.getMaterialId());
            lr.setUoMId(line.getUoMId());
            lr.setQuantity(line.getQuantity());
            lr.setUnitCost(null); // 出库不传 unitCost，inventory 按移动加权平均快照成本
            lr.setBatchNo(line.getBatchNo());
            lineRequests.add(lr);
        }
        request.setLines(lineRequests);

        return stockMoveBiz.generateMove(request, context);
    }
}
