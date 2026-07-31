package app.erp.inv.service.processor;

import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ErpInvStockMove reverse per-mutation Processor（R6.4，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含冲销编排：require → DONE 守卫 → buildReverseRequest → 委托 {@link ErpInvStockMoveGenerateMoveProcessor} 生成反向移动。
 * 共享 protected helper（{@code requireMove}/{@code loadLines}/{@code inverseMoveType}/{@code negateOrSame}）单一真相源在
 * {@link ErpInvStockMoveProcessor}（delete-after-extract facade）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvStockMoveReverseProcessor {

    @Inject
    ErpInvStockMoveProcessor facade;

    @Inject
    ErpInvStockMoveGenerateMoveProcessor generateMoveProcessor;

    public ErpInvStockMove reverse(Long moveId, IServiceContext context) {
        ErpInvStockMove original = facade.requireMove(moveId, context);
        if (original.getDocStatus() == null
                || !Objects.equals(original.getDocStatus(), ErpInvConstants.DOC_STATUS_DONE)) {
            throw new NopException(ErpInvErrors.ERR_REVERSE_NOT_DONE)
                    .param(ErpInvErrors.ARG_MOVE_CODE, original.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, original.getDocStatus());
        }

        List<ErpInvStockMoveLine> originalLines = facade.loadLines(original.getId());
        StockMoveRequest reverseReq = buildReverseRequest(original, originalLines);
        return generateMoveProcessor.generateMove(reverseReq, context);
    }

    protected StockMoveRequest buildReverseRequest(ErpInvStockMove original, List<ErpInvStockMoveLine> originalLines) {
        StockMoveRequest reverseReq = new StockMoveRequest();
        reverseReq.setMoveType(facade.inverseMoveType(original.getMoveType()));
        reverseReq.setOrgId(original.getOrgId());
        reverseReq.setBusinessDate(CoreMetrics.today());
        reverseReq.setSourceWarehouseId(original.getDestWarehouseId());
        reverseReq.setSourceLocationId(original.getDestLocationId());
        reverseReq.setDestWarehouseId(original.getSourceWarehouseId());
        reverseReq.setDestLocationId(original.getSourceLocationId());
        reverseReq.setRelatedBillType("REVERSAL");
        reverseReq.setRelatedBillCode(original.getCode());
        reverseReq.setOriginReturnedMoveId(original.getId());
        reverseReq.setRemark("冲销");
        List<StockMoveLineRequest> reverseLines = new ArrayList<>(originalLines.size());
        for (ErpInvStockMoveLine ol : originalLines) {
            StockMoveLineRequest rl = new StockMoveLineRequest();
            rl.setMaterialId(ol.getMaterialId());
            rl.setSkuId(ol.getSkuId());
            rl.setUoMId(ol.getUoMId());
            rl.setQuantity(facade.negateOrSame(ErpInvStockMoveProcessor.nz(ol.getQuantity()), original.getMoveType()));
            rl.setUnitCost(ErpInvStockMoveProcessor.nz(ol.getUnitCost()));
            rl.setCurrencyId(ol.getCurrencyId());
            rl.setBatchNo(ol.getBatchNo());
            rl.setSerialNo(ol.getSerialNo());
            rl.setSourceLocationId(ol.getDestLocationId());
            rl.setDestLocationId(ol.getSourceLocationId());
            reverseLines.add(rl);
        }
        reverseReq.setLines(reverseLines);
        return reverseReq;
    }
}
