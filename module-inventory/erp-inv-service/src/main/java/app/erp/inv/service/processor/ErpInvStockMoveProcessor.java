package app.erp.inv.service.processor;

import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.biz.TraceChainResult;
import app.erp.inv.dao.ErpInvDaoConstants;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.posting.InvPostingDispatcher;
import app.erp.inv.service.stock.StockMoveBookkeeper;
import app.erp.inv.service.trace.TraceChainQuery;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 库存移动单状态机 + 跨域契约编排 Processor（{@code processor-extension-pattern.md} Facade + Processor）。
 * Facade {@code ErpInvStockMoveBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>配置余地：每个动作只编排步骤顺序（加载→校验迁移→校验业务规则→执行→后置回写），各步骤为 {@code protected}
 * 方法、以 {@link IServiceContext} 为末参。
 *
 * <p>跨实体：DONE 写流水/更新余额经 {@link StockMoveBookkeeper}；存货过账事件经 {@link InvPostingDispatcher}；
 * 追溯链经 {@link TraceChainQuery}。
 */
public class ErpInvStockMoveProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    StockMoveBookkeeper bookkeeper;

    @Inject
    InvPostingDispatcher postingDispatcher;

    @Inject
    TraceChainQuery traceChainQuery;

    public ErpInvStockMove generateMove(StockMoveRequest request, IServiceContext context) {
        if (request.isBusinessLinked()) {
            ErpInvStockMove existing = findExisting(request.getRelatedBillType(), request.getRelatedBillCode(), context);
            if (existing != null) {
                return existing;
            }
        }

        ErpInvStockMove move = newMove(request);
        moveDao().saveEntity(move);
        List<ErpInvStockMoveLine> lines = newLines(move, request);
        IEntityDao<ErpInvStockMoveLine> lineDao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        for (ErpInvStockMoveLine line : lines) {
            line.setMoveId(move.getId());
            lineDao.saveEntity(line);
        }

        doConfirm(move, lines, context);
        if (request.isBusinessLinked()) {
            doComplete(move, lines, request.getAcctSchemaId(), context);
            move = requireMove(move.getId(), context);
        }
        return move;
    }

    public ErpInvStockMove confirm(Long moveId, IServiceContext context) {
        ErpInvStockMove move = requireMove(moveId, context);
        List<ErpInvStockMoveLine> lines = loadLines(move.getId());
        doConfirm(move, lines, context);
        return move;
    }

    public ErpInvStockMove complete(Long moveId, IServiceContext context) {
        ErpInvStockMove move = requireMove(moveId, context);
        List<ErpInvStockMoveLine> lines = loadLines(move.getId());
        doComplete(move, lines, null, context);
        return requireMove(move.getId(), context);
    }

    public ErpInvStockMove cancel(Long moveId, IServiceContext context) {
        ErpInvStockMove move = requireMove(moveId, context);
        String status = move.getDocStatus();
        if (status == null
                || (!Objects.equals(status, ErpInvConstants.DOC_STATUS_DRAFT) && !Objects.equals(status, ErpInvConstants.DOC_STATUS_CONFIRMED))) {
            throw new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpInvErrors.ARG_MOVE_CODE, move.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpInvErrors.ARG_EXPECTED_STATUS,
                            "DRAFT或CONFIRMED");
        }
        if (Objects.equals(status, ErpInvConstants.DOC_STATUS_CONFIRMED)) {
            releaseReservation(move, loadLines(move.getId()), context);
        }
        move.setDocStatus(ErpInvConstants.DOC_STATUS_CANCELLED);
        moveDao().saveOrUpdateEntity(move);
        return move;
    }

    public ErpInvStockMove reverse(Long moveId, IServiceContext context) {
        ErpInvStockMove original = requireMove(moveId, context);
        if (original.getDocStatus() == null || !Objects.equals(original.getDocStatus(), ErpInvConstants.DOC_STATUS_DONE)) {
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

    public ErpInvStockMove findByRelatedBill(String relatedBillType, String relatedBillCode, IServiceContext context) {
        if (relatedBillType == null || relatedBillCode == null) {
            return null;
        }
        // O-5：改 findFirstByExample 为 findFirstByQuery + id DESC，确保确定性（findFirstByExample 无 ORDER BY 支持）
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("relatedBillType", relatedBillType), eq("relatedBillCode", relatedBillCode)));
        q.addOrderField("id", true);
        return moveDao().findFirstByQuery(q);
    }

    public TraceChainResult forwardTrace(Long moveId, IServiceContext context) {
        return traceChainQuery.forwardTrace(moveId, isTraceChainEnabled(), traceChainMaxDepth());
    }

    public TraceChainResult backwardTrace(Long moveId, IServiceContext context) {
        return traceChainQuery.backwardTrace(moveId, isTraceChainEnabled(), traceChainMaxDepth());
    }

    public TraceChainResult returnTrace(Long moveId, IServiceContext context) {
        return traceChainQuery.returnTrace(moveId, isTraceChainEnabled());
    }

    public TraceChainResult batchTrace(String batchNo, IServiceContext context) {
        return traceChainQuery.batchTrace(batchNo, isTraceChainEnabled());
    }

    // ---------- step：状态机迁移 ----------

    protected void doConfirm(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, IServiceContext context) {
        String status = move.getDocStatus();
        if (status == null || !Objects.equals(status, ErpInvConstants.DOC_STATUS_DRAFT)) {
            throw new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpInvErrors.ARG_MOVE_CODE, move.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpInvErrors.ARG_EXPECTED_STATUS, "DRAFT");
        }
        validateAvailable(move, lines, context);
        applyReservation(move, lines, true, context);
        move.setDocStatus(ErpInvConstants.DOC_STATUS_CONFIRMED);
        moveDao().saveOrUpdateEntity(move);
    }

    protected void doComplete(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, Long acctSchemaId,
                              IServiceContext context) {
        String status = move.getDocStatus();
        if (status == null || !Objects.equals(status, ErpInvConstants.DOC_STATUS_CONFIRMED)) {
            throw new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpInvErrors.ARG_MOVE_CODE, move.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpInvErrors.ARG_EXPECTED_STATUS, "CONFIRMED");
        }
        releaseReservation(move, lines, context);
        bookkeeper.bookCompletion(move, lines, acctSchemaId);
        move.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
        moveDao().saveOrUpdateEntity(move);
        postingDispatcher.dispatchIfApplicable(move, lines);
    }

    protected void validateAvailable(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, IServiceContext context) {
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

    protected void applyReservation(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, boolean reserve,
                                    IServiceContext context) {
        if (!reservesOnConfirm(move.getMoveType())) {
            return;
        }
        BigDecimal sign = reserve ? BigDecimal.ONE : BigDecimal.ONE.negate();
        IEntityDao<ErpInvStockBalance> balanceDao = daoProvider.daoFor(ErpInvStockBalance.class);
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

    protected void releaseReservation(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, IServiceContext context) {
        applyReservation(move, lines, false, context);
    }

    // ---------- helpers: entity construction ----------

    protected ErpInvStockMove newMove(StockMoveRequest request) {
        ErpInvStockMove move = moveDao().newEntity();
        move.setCode(StringHelper.isBlank(request.getCode()) ? newMoveCode() : request.getCode());
        move.setMoveType(request.getMoveType());
        move.setOrgId(request.getOrgId());
        move.setBusinessDate(request.getBusinessDate() != null ? request.getBusinessDate() : CoreMetrics.today());
        move.setSourceWarehouseId(request.getSourceWarehouseId());
        move.setSourceLocationId(request.getSourceLocationId());
        move.setDestWarehouseId(request.getDestWarehouseId());
        move.setDestLocationId(request.getDestLocationId());
        move.setDocStatus(ErpInvConstants.DOC_STATUS_DRAFT);
        move.setApproveStatus(ErpInvDaoConstants.APPROVE_STATUS_UNSUBMITTED);
        move.setPosted(false);
        move.setRelatedBillType(request.getRelatedBillType());
        move.setRelatedBillCode(request.getRelatedBillCode());
        move.setRemark(request.getRemark());
        move.setOriginMoveId(request.getOriginMoveId());
        move.setOriginReturnedMoveId(request.getOriginReturnedMoveId());
        return move;
    }

    protected List<ErpInvStockMoveLine> newLines(ErpInvStockMove move, StockMoveRequest request) {
        List<ErpInvStockMoveLine> lines = new ArrayList<>();
        if (request.getLines() == null) {
            return lines;
        }
        int lineNo = 1;
        for (StockMoveLineRequest req : request.getLines()) {
            ErpInvStockMoveLine line = daoProvider.daoFor(ErpInvStockMoveLine.class).newEntity();
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

    protected ErpInvStockMove requireMove(Long moveId, IServiceContext context) {
        ErpInvStockMove move = moveDao().getEntityById(moveId);
        if (move == null) {
            throw new NopException(ErpInvErrors.ERR_MOVE_NOT_FOUND).param(ErpInvErrors.ARG_MOVE_ID, moveId);
        }
        return move;
    }

    protected ErpInvStockMove findExisting(String relatedBillType, String relatedBillCode, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("relatedBillType", relatedBillType), eq("relatedBillCode", relatedBillCode)));
        // O-5：追加 id DESC 确保确定性结果
        q.addOrderField("id", true);
        return moveDao().findFirstByQuery(q);
    }

    protected List<ErpInvStockMoveLine> loadLines(Long moveId) {
        // D2 边界场景：同聚合子表加载，父实体已由 requireEntity/get 经数据权限/Meta 管道授权，子行无独立权限规则。
        IEntityDao<ErpInvStockMoveLine> dao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- helpers: semantics ----------

    protected boolean reservesOnConfirm(String moveType) {
        if (moveType == null) {
            return false;
        }
        return Objects.equals(moveType, ErpInvConstants.MOVE_TYPE_OUTGOING)
                || Objects.equals(moveType, ErpInvConstants.MOVE_TYPE_INTERNAL_TRANSFER);
    }

    protected Long resolveReservationWarehouseId(ErpInvStockMove move) {
        return move.getSourceWarehouseId();
    }

    protected Long resolveReservationLocationId(ErpInvStockMove move, ErpInvStockMoveLine line) {
        return line.getSourceLocationId() != null ? line.getSourceLocationId() : move.getSourceLocationId();
    }

    protected String inverseMoveType(String moveType) {
        if (moveType == null) {
            return null;
        }
        if (Objects.equals(moveType, ErpInvConstants.MOVE_TYPE_INCOMING)) {
            return ErpInvConstants.MOVE_TYPE_OUTGOING;
        }
        if (Objects.equals(moveType, ErpInvConstants.MOVE_TYPE_OUTGOING)) {
            return ErpInvConstants.MOVE_TYPE_INCOMING;
        }
        return moveType;
    }

    protected BigDecimal negateOrSame(BigDecimal qty, String moveType) {
        if (qty == null) {
            return BigDecimal.ZERO;
        }
        return qty;
    }

    protected void recomputeAvailable(ErpInvStockBalance balance) {
        BigDecimal total = nz(balance.getTotalQuantity());
        BigDecimal reserved = nz(balance.getReservedQuantity());
        BigDecimal locked = nz(balance.getLockedQuantity());
        balance.setAvailableQuantity(total.subtract(reserved).subtract(locked));
    }

    protected boolean isNegativeStockAllowed() {
        Boolean flag = AppConfig.var(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    protected boolean isTraceChainEnabled() {
        Boolean flag = AppConfig.var(ErpInvConstants.CONFIG_TRACE_CHAIN_ENABLED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    protected int traceChainMaxDepth() {
        Integer depth = AppConfig.var(ErpInvConstants.CONFIG_TRACE_CHAIN_MAX_DEPTH,
                ErpInvConstants.TRACE_CHAIN_MAX_DEPTH_DEFAULT);
        if (depth == null || depth <= 0) {
            return ErpInvConstants.TRACE_CHAIN_MAX_DEPTH_DEFAULT;
        }
        return depth;
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpInvStockMove> moveDao() {
        return daoProvider.daoFor(ErpInvStockMove.class);
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected static String newMoveCode() {
        return "MV-" + StringHelper.generateUUID();
    }
}
