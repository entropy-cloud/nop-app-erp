package app.erp.mfg.service.processor;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.posting.MfgPostingExecutor;
import app.erp.mfg.service.posting.SubcontractPostingDispatcher;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 委外单状态机编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 * 镜像 {@link ErpMfgWorkOrderProcessor}：审批迁移 + 业务动作均由 protected step 方法编排，下游派生 Processor
 * 可逐 step 覆盖。事务边界跟随 xbiz mutation（{@code @BizMutation} 自动事务），本类不带 {@code @Transactional}。
 *
 * <p>状态机（{@code docs/design/manufacturing/subcontracting.md §状态机设计}，8 态核心可执行子集）：
 * <pre>
 * DRAFT → SUBMITTED → APPROVED → ISSUED → RECEIVED → COMPLETED
 *                 ↘ REJECTED        ↘ CANCELLED（APPROVED 亦可取消）
 * </pre>
 *
 * <p>三段业务动作：
 * <ul>
 *   <li>{@link #issueMaterials}：APPROVED→ISSUED，发料出库（OUTGOING 移动单，材料成本出库）。</li>
 *   <li>{@link #receiveFinished}：ISSUED→RECEIVED，成品入库（INCOMING 移动单，含加工费成本）。</li>
 *   <li>{@link #postProcessingFee}：RECEIVED→COMPLETED，加工费过账（SUBCONTRACT_FEE 凭证，posted=true）。</li>
 *   <li>{@link #reverseCompletion}：COMPLETED→CANCELLED，全量红冲（SI/SR/SF 凭证 + 两段库存移动 + posted=false）。</li>
 * </ul>
 *
 * <p>config-gated {@code erp-mfg.subcontract-posting-enabled}（默认 false 向后兼容）。
 */
public class ErpMfgSubcontractOrderProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpMfgSubcontractOrderProcessor.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;
    @Inject
    SubcontractPostingDispatcher subcontractPostingDispatcher;
    @Inject
    MfgPostingExecutor mfgPostingExecutor;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setStockMoveBiz(IErpInvStockMoveBiz stockMoveBiz) {
        this.stockMoveBiz = stockMoveBiz;
    }

    public void setSubcontractPostingDispatcher(SubcontractPostingDispatcher subcontractPostingDispatcher) {
        this.subcontractPostingDispatcher = subcontractPostingDispatcher;
    }

    public void setMfgPostingExecutor(MfgPostingExecutor mfgPostingExecutor) {
        this.mfgPostingExecutor = mfgPostingExecutor;
    }

    // ---------- 审批轴 ----------

    public ErpMfgSubcontractOrder submitForApproval(String id, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(id, context);
        validateTransitionForSubmit(order, context);
        doSubmit(order, context);
        return order;
    }

    public ErpMfgSubcontractOrder withdrawApproval(String id, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(id, context);
        validateTransitionForWithdraw(order, context);
        doWithdrawSubmit(order, context);
        return order;
    }

    public ErpMfgSubcontractOrder approve(String id, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(id, context);
        validateTransitionForApprove(order, context);
        doApprove(order, context);
        return order;
    }

    public ErpMfgSubcontractOrder reject(String id, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(id, context);
        validateTransitionForReject(order, context);
        doReject(order, context);
        return order;
    }

    public ErpMfgSubcontractOrder reverseApprove(String id, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(id, context);
        validateTransitionForReverseApprove(order, context);
        doReverseApprove(order, context);
        return order;
    }

    public ErpMfgSubcontractOrder cancel(Long subcontractOrderId, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(String.valueOf(subcontractOrderId), context);
        String status = order.getDocStatus();
        if (status == null || (!Objects.equals(status, ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT)
                && !Objects.equals(status, ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED)
                && !Objects.equals(status, ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED))) {
            throw illegalTransition(order, status, "DRAFT、SUBMITTED 或 APPROVED");
        }
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED);
        orderDao().updateEntity(order);
        return order;
    }

    // ---------- 三段业务动作 ----------

    /**
     * 发料给供应商：APPROVED→ISSUED。按委外行逐行生成库存 OUTGOING 移动单（材料成本出库）。
     */
    public ErpMfgSubcontractOrder issueMaterials(Long subcontractOrderId, IServiceContext context) {
        return issueMaterials(subcontractOrderId, null, context);
    }

    /**
     * 发料给供应商：APPROVED→ISSUED。按委外行逐行生成库存 OUTGOING 移动单（材料成本出库）。
     *
     * @param sourceWarehouseId 发料源仓库（null 时由库存域默认仓库处理）
     */
    public ErpMfgSubcontractOrder issueMaterials(Long subcontractOrderId, Long sourceWarehouseId, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(String.valueOf(subcontractOrderId), context);
        requireStatus(order, ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED, "APPROVED");

        List<ErpMfgSubcontractOrderLine> lines = loadLines(subcontractOrderId);
        if (lines.isEmpty()) {
            throw new NopException(ErpMfgErrors.ERR_SUBCONTRACT_LINES_EMPTY)
                    .param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode());
        }

        generateIssueMove(order, lines, sourceWarehouseId, context);

        if (isSubcontractPostingEnabled()) {
            subcontractPostingDispatcher.dispatchIssuePosting(subcontractOrderId);
        }

        order = orderDao().getEntityById(subcontractOrderId);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED);
        orderDao().updateEntity(order);
        return order;
    }

    /**
     * 收回加工品入库：ISSUED→RECEIVED。生成库存 INCOMING 移动单（成品入库，含加工费成本归集）。
     */
    public ErpMfgSubcontractOrder receiveFinished(Long subcontractOrderId, BigDecimal receivedQty, IServiceContext context) {
        return receiveFinished(subcontractOrderId, receivedQty, null, context);
    }

    /**
     * 收回加工品入库：ISSUED→RECEIVED。生成库存 INCOMING 移动单（成品入库，含加工费成本归集）。
     *
     * @param destWarehouseId 收货目标仓库（null 时由库存域默认仓库处理）
     */
    public ErpMfgSubcontractOrder receiveFinished(Long subcontractOrderId, BigDecimal receivedQty,
                                                   Long destWarehouseId, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(String.valueOf(subcontractOrderId), context);
        requireStatus(order, ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED, "ISSUED");

        if (receivedQty == null || receivedQty.signum() <= 0) {
            BigDecimal lineQty = sumLineQuantity(subcontractOrderId);
            receivedQty = lineQty.signum() > 0 ? lineQty : BigDecimal.ONE;
        }

        generateReceiptMove(order, receivedQty, destWarehouseId, context);

        if (isSubcontractPostingEnabled()) {
            subcontractPostingDispatcher.dispatchReceiptPosting(subcontractOrderId);
        }

        order = orderDao().getEntityById(subcontractOrderId);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED);
        orderDao().updateEntity(order);
        return order;
    }

    /**
     * 加工费过账：RECEIVED→COMPLETED。构造 SUBCONTRACT_FEE PostingEvent 经 Dispatcher 过账，
     * 成功后 posted=true、docStatus→COMPLETED。config-gated {@code erp-mfg.subcontract-posting-enabled}。
     */
    public ErpMfgSubcontractOrder postProcessingFee(Long subcontractOrderId, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(String.valueOf(subcontractOrderId), context);
        requireStatus(order, ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED, "RECEIVED");

        if (isSubcontractPostingEnabled()) {
            subcontractPostingDispatcher.dispatchFeePosting(subcontractOrderId);
        }

        order = orderDao().getEntityById(subcontractOrderId);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED);
        orderDao().updateEntity(order);
        return order;
    }

    /**
     * 红冲完工：COMPLETED→CANCELLED。全量撤销已落地的 GL 凭证（SI/SR/SF）+ 反向两段库存移动
     * （issue/receipt）+ posted=false。镜像 {@code postProcessingFee} Facade→Processor 范式，
     * protected step 方法拆分供下游派生覆盖。
     *
     * <p>config-gate 裁决：红冲 GL 以 {@code posted==true} 为前置（非 config flag），避免正向过账开启→
     * 红冲前关闭 config→GL 红冲被跳过致孤儿凭证（设计 subcontracting.md §实现偏离补注 §红冲）。
     */
    public ErpMfgSubcontractOrder reverseCompletion(Long subcontractOrderId, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(String.valueOf(subcontractOrderId), context);
        validateCanReverse(order, context);
        reverseGlPostings(order, context);
        reverseInventoryMoves(order, context);
        doReverseCompletion(order, context);
        return order;
    }

    // ---------- step：红冲完工（protected，下游可逐个覆盖） ----------

    /**
     * 红冲前置守卫：仅 COMPLETED 且 posted==true 的委外单可红冲。
     */
    protected void validateCanReverse(ErpMfgSubcontractOrder order, IServiceContext context) {
        String status = order.getDocStatus();
        if (!Objects.equals(status, ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED)
                || !Boolean.TRUE.equals(order.getPosted())) {
            throw new NopException(ErpMfgErrors.ERR_SUBCONTRACT_CANNOT_REVERSE)
                    .param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode())
                    .param(ErpMfgErrors.ARG_CURRENT_STATUS, status);
        }
    }

    /**
     * 红冲三段 GL 凭证（-SF/-SR/-SI）。posted==true 即有凭证须红冲（非 config flag），避免孤儿凭证；
     * 逐段 try/catch 吞异常告警保持幂等（对齐 {@link SubcontractPostingDispatcher} 正向过账范式）。
     */
    protected void reverseGlPostings(ErpMfgSubcontractOrder order, IServiceContext context) {
        String code = order.getCode();
        reverseOneVoucher(code + "-SF", ErpFinBusinessType.SUBCONTRACT_FEE, order);
        reverseOneVoucher(code + "-SR", ErpFinBusinessType.SUBCONTRACT_RECEIPT, order);
        reverseOneVoucher(code + "-SI", ErpFinBusinessType.SUBCONTRACT_ISSUE, order);
    }

    protected void reverseOneVoucher(String billHeadCode, ErpFinBusinessType businessType, ErpMfgSubcontractOrder order) {
        try {
            mfgPostingExecutor.reverse(billHeadCode, businessType);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("委外红冲 GL 凭证失败（吞异常保持幂等），委外单 {} billHeadCode={}: {}",
                        order.getCode(), billHeadCode, e.getMessage());
            } else {
                LOG.error("委外红冲 GL 凭证异常（吞异常保持幂等），委外单 {} billHeadCode={}",
                        order.getCode(), billHeadCode, e);
            }
        }
    }

    /**
     * 反向两段库存移动（issue OUTGOING + receipt MANUFACTURING）。经 {@code relatedBillType}+{@code relatedBillCode}
     * 反查原移动单 → {@code IErpInvStockMoveBiz.reverse} 生成反向冲销移动单（余额自动回滚）。
     * 仅反向仓库前置满足的移动单（{@link #canSafelyReverse}），找不到原移动单或反向失败时吞异常告警，
     * 不阻断 GL 红冲与状态回退。
     */
    protected void reverseInventoryMoves(ErpMfgSubcontractOrder order, IServiceContext context) {
        reverseOneMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_SUBCONTRACT_RECEIPT, order.getCode(), order, context);
        reverseOneMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_SUBCONTRACT_ISSUE, order.getCode(), order, context);
    }

    protected void reverseOneMove(String relatedBillType, String relatedBillCode, ErpMfgSubcontractOrder order,
                                  IServiceContext context) {
        try {
            ErpInvStockMove original = stockMoveBiz.findByRelatedBill(relatedBillType, relatedBillCode, context);
            if (original == null) {
                return;
            }
            if (!canSafelyReverse(original)) {
                LOG.warn("委外红冲跳过库存移动反向（移动单仓库不满足反向前置，MANUFACTURE 移动单 sourceWarehouseId 为空时"
                                + "其反向冲销的 bookkeeper destWarehouseId 为空），委外单 {} relatedBillType={} moveCode={}",
                        order.getCode(), relatedBillType, original.getCode());
                return;
            }
            stockMoveBiz.reverse(original.getId(), context);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("委外红冲反向库存移动失败（吞异常保持幂等），委外单 {} relatedBillType={}: {}",
                        order.getCode(), relatedBillType, e.getMessage());
            } else {
                LOG.error("委外红冲反向库存移动异常（吞异常保持幂等），委外单 {} relatedBillType={}",
                        order.getCode(), relatedBillType, e);
            }
        }
    }

    /**
     * 判断移动单是否可安全反向。库存域 {@code inverseMoveType} 仅反转 INCOMING↔OUTGOING，
     * MANUFACTURE 等类型保持不变；bookkeeper 对非 OUTGOING 类型走 onIncoming（用 destWarehouseId）。
     * 故反向冲销的可用仓库取决于原单的 sourceWarehouseId（OUTGOING/MANUFACTURE 反向）或 destWarehouseId（INCOMING 反向）。
     */
    protected boolean canSafelyReverse(ErpInvStockMove original) {
        String moveType = original.getMoveType();
        if (Objects.equals(moveType, "INCOMING")) {
            return original.getDestWarehouseId() != null;
        }
        return original.getSourceWarehouseId() != null;
    }

    protected void doReverseCompletion(ErpMfgSubcontractOrder order, IServiceContext context) {
        order.setPosted(false);
        order.setPostedAt(null);
        order.setPostedBy(null);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED);
        orderDao().updateEntity(order);
    }

    // ---------- step：审批迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpMfgSubcontractOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null) {
            status = ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(order, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpMfgSubcontractOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpMfgSubcontractOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpMfgSubcontractOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpMfgSubcontractOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(order, status, "APPROVED");
        }
    }

    // ---------- step：审批执行 ----------

    protected void doSubmit(ErpMfgSubcontractOrder order, IServiceContext context) {
        order.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_SUBMITTED);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED);
        orderDao().updateEntity(order);
    }

    protected void doWithdrawSubmit(ErpMfgSubcontractOrder order, IServiceContext context) {
        order.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
        orderDao().updateEntity(order);
    }

    protected void doApprove(ErpMfgSubcontractOrder order, IServiceContext context) {
        order.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_APPROVED);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED);
        order.setApprovedBy(currentUserId());
        order.setApprovedAt(CoreMetrics.currentTimestamp());
        orderDao().updateEntity(order);
    }

    protected void doReject(ErpMfgSubcontractOrder order, IServiceContext context) {
        order.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_REJECTED);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED);
        orderDao().updateEntity(order);
    }

    protected void doReverseApprove(ErpMfgSubcontractOrder order, IServiceContext context) {
        order.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_REJECTED);
        order.setApprovedBy(null);
        order.setApprovedAt(null);
        orderDao().updateEntity(order);
    }

    // ---------- step：库存移动单生成（protected，下游可覆盖） ----------

    /**
     * 生成委外发料出库移动单（OUTGOING，按委外行逐行）。镜像 {@code ErpMfgWorkOrderProcessor.generateCompletionMove}
     * 的 StockMoveRequest 装配范式，moveType 用 OUTGOING（库存域 bookCompletion 据此扣减余额）。
     */
    protected void generateIssueMove(ErpMfgSubcontractOrder order,
                                     List<ErpMfgSubcontractOrderLine> lines, Long sourceWarehouseId,
                                     IServiceContext context) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpMfgConstants.MOVE_TYPE_OUTGOING_ISSUE);
        request.setOrgId(order.getOrgId());
        request.setBusinessDate(order.getBusinessDate() != null ? order.getBusinessDate() : CoreMetrics.today());
        request.setSourceWarehouseId(sourceWarehouseId);
        request.setCurrencyId(order.getCurrencyId());
        request.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));
        request.setRelatedBillType(ErpMfgConstants.RELATED_BILL_TYPE_MFG_SUBCONTRACT_ISSUE);
        request.setRelatedBillCode(order.getCode());

        List<StockMoveLineRequest> moveLines = new ArrayList<>();
        for (ErpMfgSubcontractOrderLine line : lines) {
            StockMoveLineRequest ml = new StockMoveLineRequest();
            ml.setMaterialId(line.getMaterialId());
            ml.setUoMId(line.getUoMId());
            ml.setQuantity(line.getQuantity());
            ml.setCurrencyId(order.getCurrencyId());
            moveLines.add(ml);
        }
        request.setLines(moveLines);
        stockMoveBiz.generateMove(request, context);
    }

    /**
     * 生成委外成品入库移动单（INCOMING，产成品 + 加工费成本归集）。镜像
     * {@code ErpMfgWorkOrderProcessor.generateCompletionMove} 的 MANUFACTURING 入库范式。
     */
    protected void generateReceiptMove(ErpMfgSubcontractOrder order, BigDecimal receivedQty,
                                       Long destWarehouseId, IServiceContext context) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpMfgConstants.MOVE_TYPE_MANUFACTURING);
        request.setOrgId(order.getOrgId());
        request.setBusinessDate(order.getBusinessDate() != null ? order.getBusinessDate() : CoreMetrics.today());
        request.setDestWarehouseId(destWarehouseId);
        request.setCurrencyId(order.getCurrencyId());
        request.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));
        request.setRelatedBillType(ErpMfgConstants.RELATED_BILL_TYPE_MFG_SUBCONTRACT_RECEIPT);
        request.setRelatedBillCode(order.getCode());

        BigDecimal unitCost = computeReceiptUnitCost(order, receivedQty);

        List<StockMoveLineRequest> moveLines = new ArrayList<>();
        StockMoveLineRequest ml = new StockMoveLineRequest();
        ml.setMaterialId(order.getProductId());
        Long uomId = resolveProductUomId(order.getProductId());
        ml.setUoMId(uomId);
        ml.setQuantity(receivedQty);
        ml.setUnitCost(unitCost);
        ml.setCurrencyId(order.getCurrencyId());
        moveLines.add(ml);
        request.setLines(moveLines);
        stockMoveBiz.generateMove(request, context);
    }

    /**
     * 委外成品单位成本 =（材料成本 + 加工费）/ 收货数量。材料成本取委外行加工费汇总近似（本期简化，
     * 精确材料成本归集归 N=2 计划 2026-07-13-0455-2 成本要素拆分）。加工费取订单头 processingFee。
     */
    protected BigDecimal computeReceiptUnitCost(ErpMfgSubcontractOrder order, BigDecimal receivedQty) {
        if (receivedQty == null || receivedQty.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee = nz(order.getProcessingFee());
        BigDecimal total = fee;
        return total.divide(receivedQty, 4, RoundingMode.HALF_UP);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpMfgSubcontractOrder requireOrder(String id, IServiceContext context) {
        ErpMfgSubcontractOrder order = orderDao().getEntityById(id);
        if (order == null) {
            throw new NopException(ErpMfgErrors.ERR_SUBCONTRACT_ORDER_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_ID, id);
        }
        return order;
    }

    protected void requireStatus(ErpMfgSubcontractOrder order, String expected, String expectedLabel) {
        String current = order.getDocStatus();
        if (current == null || !Objects.equals(current, expected)) {
            throw illegalTransition(order, current, expectedLabel);
        }
    }

    protected List<ErpMfgSubcontractOrderLine> loadLines(Long subcontractOrderId) {
        IEntityDao<ErpMfgSubcontractOrderLine> dao = daoProvider.daoFor(ErpMfgSubcontractOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("subcontractOrderId", subcontractOrderId));
        return dao.findAllByQuery(q);
    }

    protected BigDecimal sumLineQuantity(Long subcontractOrderId) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpMfgSubcontractOrderLine line : loadLines(subcontractOrderId)) {
            total = total.add(nz(line.getQuantity()));
        }
        return total;
    }

    protected Long resolveProductUomId(Long productId) {
        if (productId == null) {
            return null;
        }
        ErpMdMaterial product = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(productId);
        return product != null ? product.getUoMId() : null;
    }

    protected Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    protected boolean isSubcontractPostingEnabled() {
        return readBoolConfig(ErpMfgConstants.CONFIG_SUBCONTRACT_POSTING_ENABLED, false);
    }

    protected boolean readBoolConfig(String key, boolean defaultValue) {
        try {
            String value = AppConfig.var(key, String.valueOf(defaultValue));
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    protected IEntityDao<ErpMfgSubcontractOrder> orderDao() {
        return daoProvider.daoFor(ErpMfgSubcontractOrder.class);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            if (ctx == null) {
                return null;
            }
            return ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected NopException illegalTransition(ErpMfgSubcontractOrder order, String current, String expected) {
        return new NopException(ErpMfgErrors.ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, expected);
    }
}
