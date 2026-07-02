package app.erp.inv.service.entity;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.biz.TraceChainResult;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.trace.TraceChainQuery;
import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 库存移动单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现：
 *
 * <ul>
 *   <li>跨域契约 {@link IErpInvStockMoveBiz#generateMove}（幂等 + 业务联动自动推进）。</li>
 *   <li>状态机 DRAFT→CONFIRMED→DONE/CANCELLED（对齐 {@code docs/design/inventory/state-machine.md}）。</li>
 *   <li>预留量管理（出库类/内部调拨 CONFIRMED 增预留，DONE/CANCELLED 释放）。</li>
 *   <li>DONE 写不可变流水 + 更新余额（移动加权平均成本）——见 {@link app.erp.inv.service.stock.StockMoveBookkeeper}。</li>
 *   <li>DONE 发存货过账事件（同法人内部调拨除外）——见 {@link app.erp.inv.service.posting.InvPostingDispatcher}。</li>
 * </ul>
 *
 * <p>状态机迁移校验前置 {@code docStatus}，违反抛 {@link NopException}。余额更新与流水写入在同一事务
 * （cross-domain §一致性规则）。冲销 {@link #reverse} 生成反向移动单走正常流程（非反审核）。
 */
@BizModel("ErpInvStockMove")
public class ErpInvStockMoveBizModel extends CrudBizModel<ErpInvStockMove> implements IErpInvStockMoveBiz {

    @Inject
    app.erp.inv.service.stock.StockMoveBookkeeper bookkeeper;

    @Inject
    app.erp.inv.service.posting.InvPostingDispatcher postingDispatcher;

    @Inject
    app.erp.inv.service.trace.TraceChainQuery traceChainQuery;

    public ErpInvStockMoveBizModel() {
        setEntityName(ErpInvStockMove.class.getName());
    }

    @Override
    @BizMutation
    public ErpInvStockMove generateMove(@Name("request") StockMoveRequest request, IServiceContext context) {
        if (request.isBusinessLinked()) {
            ErpInvStockMove existing = findExisting(request.getRelatedBillType(), request.getRelatedBillCode(), context);
            if (existing != null) {
                return existing;
            }
        }

        ErpInvStockMove move = newMove(request);
        dao().saveEntity(move);
        List<ErpInvStockMoveLine> lines = newLines(move, request);
        IEntityDao<ErpInvStockMoveLine> lineDao = daoFor(ErpInvStockMoveLine.class);
        for (ErpInvStockMoveLine line : lines) {
            line.setMoveId(move.getId());
            lineDao.saveEntity(line);
        }

        doConfirm(move, lines);
        if (request.isBusinessLinked()) {
            doComplete(move, lines, request.getAcctSchemaId());
        }
        return move;
    }

    @Override
    @BizMutation
    public ErpInvStockMove confirm(@Name("moveId") Long moveId, IServiceContext context) {
        ErpInvStockMove move = requireMove(moveId, context);
        List<ErpInvStockMoveLine> lines = loadLines(move.getId());
        doConfirm(move, lines);
        return move;
    }

    @Override
    @BizMutation
    public ErpInvStockMove complete(@Name("moveId") Long moveId, IServiceContext context) {
        ErpInvStockMove move = requireMove(moveId, context);
        List<ErpInvStockMoveLine> lines = loadLines(move.getId());
        doComplete(move, lines, null);
        return move;
    }

    @Override
    @BizMutation
    public ErpInvStockMove cancel(@Name("moveId") Long moveId, IServiceContext context) {
        ErpInvStockMove move = requireMove(moveId, context);
        Integer status = move.getDocStatus();
        if (status == null
                || (status != ErpInvConstants.DOC_STATUS_DRAFT && status != ErpInvConstants.DOC_STATUS_CONFIRMED)) {
            throw new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpInvErrors.ARG_MOVE_CODE, move.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpInvErrors.ARG_EXPECTED_STATUS,
                            "DRAFT或CONFIRMED");
        }
        if (status == ErpInvConstants.DOC_STATUS_CONFIRMED) {
            releaseReservation(move, loadLines(move.getId()));
        }
        move.setDocStatus(ErpInvConstants.DOC_STATUS_CANCELLED);
        dao().saveOrUpdateEntity(move);
        return move;
    }

    @Override
    @BizMutation
    public ErpInvStockMove reverse(@Name("moveId") Long moveId, IServiceContext context) {
        ErpInvStockMove original = requireMove(moveId, context);
        if (original.getDocStatus() == null || original.getDocStatus() != ErpInvConstants.DOC_STATUS_DONE) {
            throw new NopException(ErpInvErrors.ERR_REVERSE_NOT_DONE)
                    .param(ErpInvErrors.ARG_MOVE_CODE, original.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, original.getDocStatus());
        }

        List<ErpInvStockMoveLine> originalLines = loadLines(original.getId());

        StockMoveRequest reverseReq = new StockMoveRequest();
        reverseReq.setMoveType(inverseMoveType(original.getMoveType()));
        reverseReq.setOrgId(original.getOrgId());
        reverseReq.setBusinessDate(CoreMetrics.today());
        reverseReq.setSourceWarehouseId(original.getDestWarehouseId());
        reverseReq.setSourceLocationId(original.getDestLocationId());
        reverseReq.setDestWarehouseId(original.getSourceWarehouseId());
        reverseReq.setDestLocationId(original.getSourceLocationId());
        reverseReq.setRelatedBillType("REVERSAL");
        reverseReq.setRelatedBillCode(original.getCode());
        reverseReq.setOriginReturnedMoveId(original.getId());
        reverseReq.setRemark("冲销 " + original.getCode());
        List<StockMoveLineRequest> reverseLines = new ArrayList<>(originalLines.size());
        for (ErpInvStockMoveLine ol : originalLines) {
            StockMoveLineRequest rl = new StockMoveLineRequest();
            rl.setMaterialId(ol.getMaterialId());
            rl.setSkuId(ol.getSkuId());
            rl.setUoMId(ol.getUoMId());
            rl.setQuantity(negateOrSame(nz(ol.getQuantity()), original.getMoveType()));
            rl.setUnitCost(nz(ol.getUnitCost()));
            rl.setCurrencyId(ol.getCurrencyId());
            rl.setBatchNo(ol.getBatchNo());
            rl.setSerialNo(ol.getSerialNo());
            rl.setSourceLocationId(ol.getDestLocationId());
            rl.setDestLocationId(ol.getSourceLocationId());
            reverseLines.add(rl);
        }
        reverseReq.setLines(reverseLines);
        return generateMove(reverseReq, context);
    }

    @Override
    @BizAction
    public ErpInvStockMove findByRelatedBill(@Name("relatedBillType") String relatedBillType,
                                             @Name("relatedBillCode") String relatedBillCode,
                                             IServiceContext context) {
        if (relatedBillType == null || relatedBillCode == null) {
            return null;
        }
        ErpInvStockMove example = dao().newEntity();
        example.setRelatedBillType(relatedBillType);
        example.setRelatedBillCode(relatedBillCode);
        return dao().findFirstByExample(example);
    }

    @Override
    @BizQuery
    public TraceChainResult forwardTrace(@Name("moveId") Long moveId, IServiceContext context) {
        return traceChainQuery.forwardTrace(moveId, isTraceChainEnabled(), traceChainMaxDepth());
    }

    @Override
    @BizQuery
    public TraceChainResult backwardTrace(@Name("moveId") Long moveId, IServiceContext context) {
        return traceChainQuery.backwardTrace(moveId, isTraceChainEnabled(), traceChainMaxDepth());
    }

    @Override
    @BizQuery
    public TraceChainResult returnTrace(@Name("moveId") Long moveId, IServiceContext context) {
        return traceChainQuery.returnTrace(moveId, isTraceChainEnabled());
    }

    @Override
    @BizQuery
    public TraceChainResult batchTrace(@Name("batchNo") String batchNo, IServiceContext context) {
        return traceChainQuery.batchTrace(batchNo, isTraceChainEnabled());
    }

    // ---------- state machine internals ----------

    private void doConfirm(ErpInvStockMove move, List<ErpInvStockMoveLine> lines) {
        Integer status = move.getDocStatus();
        if (status == null || status != ErpInvConstants.DOC_STATUS_DRAFT) {
            throw new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpInvErrors.ARG_MOVE_CODE, move.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpInvErrors.ARG_EXPECTED_STATUS, "DRAFT");
        }
        validateAvailable(move, lines);
        applyReservation(move, lines, true);
        move.setDocStatus(ErpInvConstants.DOC_STATUS_CONFIRMED);
        dao().saveOrUpdateEntity(move);
    }

    private void doComplete(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, Long acctSchemaId) {
        Integer status = move.getDocStatus();
        if (status == null || status != ErpInvConstants.DOC_STATUS_CONFIRMED) {
            throw new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpInvErrors.ARG_MOVE_CODE, move.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpInvErrors.ARG_EXPECTED_STATUS, "CONFIRMED");
        }
        releaseReservation(move, lines);
        bookkeeper.bookCompletion(move, lines, acctSchemaId);
        move.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
        dao().saveOrUpdateEntity(move);
        postingDispatcher.dispatchIfApplicable(move, lines);
    }

    private void validateAvailable(ErpInvStockMove move, List<ErpInvStockMoveLine> lines) {
        if (isNegativeStockAllowed()) {
            return;
        }
        if (!reservesOnConfirm(move.getMoveType())) {
            return;
        }
        for (ErpInvStockMoveLine line : lines) {
            ErpInvStockBalance balance = bookkeeper.upsertBalance(move, line,
                    resolveReservationWarehouseId(move), resolveReservationLocationId(move, line));
            BigDecimal available = nz(balance.getAvailableQuantity());
            BigDecimal required = nz(line.getQuantity());
            if (available.compareTo(required) < 0) {
                throw new NopException(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT)
                        .param(ErpInvErrors.ARG_MATERIAL_ID, line.getMaterialId())
                        .param(ErpInvErrors.ARG_WAREHOUSE_ID, balance.getWarehouseId())
                        .param(ErpInvErrors.ARG_AVAILABLE, available.toPlainString())
                        .param(ErpInvErrors.ARG_REQUIRED, required.toPlainString());
            }
        }
    }

    private void applyReservation(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, boolean reserve) {
        if (!reservesOnConfirm(move.getMoveType())) {
            return;
        }
        BigDecimal sign = reserve ? BigDecimal.ONE : BigDecimal.ONE.negate();
        IEntityDao<ErpInvStockBalance> balanceDao = daoFor(ErpInvStockBalance.class);
        for (ErpInvStockMoveLine line : lines) {
            ErpInvStockBalance balance = bookkeeper.upsertBalance(move, line,
                    resolveReservationWarehouseId(move), resolveReservationLocationId(move, line));
            BigDecimal qty = nz(line.getQuantity());
            BigDecimal reserved = nz(balance.getReservedQuantity()).add(qty.multiply(sign));
            balance.setReservedQuantity(reserved);
            recomputeAvailable(balance);
            balanceDao.saveOrUpdateEntity(balance);
        }
    }

    private void releaseReservation(ErpInvStockMove move, List<ErpInvStockMoveLine> lines) {
        applyReservation(move, lines, false);
    }

    // ---------- helpers: entity construction ----------

    private ErpInvStockMove newMove(StockMoveRequest request) {
        ErpInvStockMove move = newEntity();
        move.setCode(StringHelper.isBlank(request.getCode()) ? newMoveCode() : request.getCode());
        move.setMoveType(request.getMoveType());
        move.setOrgId(request.getOrgId());
        move.setBusinessDate(request.getBusinessDate() != null ? request.getBusinessDate() : CoreMetrics.today());
        move.setSourceWarehouseId(request.getSourceWarehouseId());
        move.setSourceLocationId(request.getSourceLocationId());
        move.setDestWarehouseId(request.getDestWarehouseId());
        move.setDestLocationId(request.getDestLocationId());
        move.setDocStatus(ErpInvConstants.DOC_STATUS_DRAFT);
        move.setApproveStatus(10);
        move.setPosted(false);
        move.setRelatedBillType(request.getRelatedBillType());
        move.setRelatedBillCode(request.getRelatedBillCode());
        move.setRemark(request.getRemark());
        move.setOriginMoveId(request.getOriginMoveId());
        move.setOriginReturnedMoveId(request.getOriginReturnedMoveId());
        return move;
    }

    private List<ErpInvStockMoveLine> newLines(ErpInvStockMove move, StockMoveRequest request) {
        List<ErpInvStockMoveLine> lines = new ArrayList<>();
        if (request.getLines() == null) {
            return lines;
        }
        int lineNo = 1;
        for (StockMoveLineRequest req : request.getLines()) {
            ErpInvStockMoveLine line = daoFor(ErpInvStockMoveLine.class).newEntity();
            line.setMoveId(move.getId());
            line.setLineNo(lineNo++);
            line.setMaterialId(req.getMaterialId());
            line.setSkuId(req.getSkuId());
            line.setUoMId(req.getUoMId());
            BigDecimal qty = req.getQuantity() != null ? req.getQuantity() : BigDecimal.ZERO;
            line.setQuantity(qty);
            BigDecimal unitCost = req.getUnitCost() != null ? req.getUnitCost() : BigDecimal.ZERO;
            line.setUnitCost(unitCost);
            line.setTotalCost(unitCost.multiply(qty));
            line.setCurrencyId(req.getCurrencyId() != null ? req.getCurrencyId() : request.getCurrencyId());
            line.setBatchNo(req.getBatchNo());
            line.setSerialNo(req.getSerialNo());
            line.setSourceLocationId(req.getSourceLocationId() != null ? req.getSourceLocationId()
                    : move.getSourceLocationId());
            line.setDestLocationId(req.getDestLocationId() != null ? req.getDestLocationId()
                    : move.getDestLocationId());
            line.setRemark(req.getRemark());
            lines.add(line);
        }
        return lines;
    }

    // ---------- helpers: queries ----------

    private ErpInvStockMove requireMove(Long moveId, IServiceContext context) {
        ErpInvStockMove move = get(String.valueOf(moveId), true, context);
        if (move == null) {
            throw new NopException(ErpInvErrors.ERR_MOVE_NOT_FOUND).param(ErpInvErrors.ARG_MOVE_ID, moveId);
        }
        return move;
    }

    private ErpInvStockMove findExisting(String relatedBillType, String relatedBillCode, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("relatedBillType", relatedBillType), eq("relatedBillCode", relatedBillCode)));
        return findFirst(q, null, context);
    }

    private List<ErpInvStockMoveLine> loadLines(Long moveId) {
        // D2 边界场景：同聚合子表加载，父实体已由 requireEntity/get 经数据权限/Meta 管道授权，子行无独立权限规则。
        IEntityDao<ErpInvStockMoveLine> dao = daoFor(ErpInvStockMoveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- helpers: semantics ----------

    private boolean reservesOnConfirm(Integer moveType) {
        if (moveType == null) {
            return false;
        }
        return moveType == ErpInvConstants.MOVE_TYPE_OUTGOING
                || moveType == ErpInvConstants.MOVE_TYPE_INTERNAL_TRANSFER;
    }

    private Long resolveReservationWarehouseId(ErpInvStockMove move) {
        return move.getSourceWarehouseId();
    }

    private Long resolveReservationLocationId(ErpInvStockMove move, ErpInvStockMoveLine line) {
        return line.getSourceLocationId() != null ? line.getSourceLocationId() : move.getSourceLocationId();
    }

    private Integer inverseMoveType(Integer moveType) {
        if (moveType == null) {
            return null;
        }
        if (moveType == ErpInvConstants.MOVE_TYPE_INCOMING) {
            return ErpInvConstants.MOVE_TYPE_OUTGOING;
        }
        if (moveType == ErpInvConstants.MOVE_TYPE_OUTGOING) {
            return ErpInvConstants.MOVE_TYPE_INCOMING;
        }
        return moveType;
    }

    private BigDecimal negateOrSame(BigDecimal qty, Integer moveType) {
        if (qty == null) {
            return BigDecimal.ZERO;
        }
        return qty;
    }

    private void recomputeAvailable(ErpInvStockBalance balance) {
        BigDecimal total = nz(balance.getTotalQuantity());
        BigDecimal reserved = nz(balance.getReservedQuantity());
        BigDecimal locked = nz(balance.getLockedQuantity());
        balance.setAvailableQuantity(total.subtract(reserved).subtract(locked));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    boolean isNegativeStockAllowed() {
        Boolean flag = AppConfig.var(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    boolean isTraceChainEnabled() {
        Boolean flag = AppConfig.var(ErpInvConstants.CONFIG_TRACE_CHAIN_ENABLED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    int traceChainMaxDepth() {
        Integer depth = AppConfig.var(ErpInvConstants.CONFIG_TRACE_CHAIN_MAX_DEPTH,
                ErpInvConstants.TRACE_CHAIN_MAX_DEPTH_DEFAULT);
        if (depth == null || depth <= 0) {
            return ErpInvConstants.TRACE_CHAIN_MAX_DEPTH_DEFAULT;
        }
        return depth;
    }

    private String newMoveCode() {
        return "MV-" + StringHelper.generateUUID();
    }
}
