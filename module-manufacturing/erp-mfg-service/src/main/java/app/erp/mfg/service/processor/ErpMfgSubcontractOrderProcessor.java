package app.erp.mfg.service.processor;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.inv.biz.IErpInvStockLedgerBiz;
import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.posting.MfgPostingExecutor;
import app.erp.mfg.service.posting.SubcontractPostingDispatcher;
import app.erp.mfg.service.statemachine.ErpMfgSubcontractOrderApprovalStateMachine;
import app.erp.mfg.service.statemachine.ErpMfgSubcontractOrderDocumentStateMachine;
import app.erp.common.service.SoDGuard;
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
import io.nop.orm.IOrmTemplate;
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
    IOrmTemplate ormTemplate;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;
    @Inject
    IErpInvStockLedgerBiz stockLedgerBiz;
    @Inject
    SubcontractPostingDispatcher subcontractPostingDispatcher;
    @Inject
    MfgPostingExecutor mfgPostingExecutor;
    @Inject
    ErpMfgSubcontractOrderSubmitForApprovalProcessor submitForApprovalProcessor;
    @Inject
    ErpMfgSubcontractOrderApproveProcessor approveProcessor;
    @Inject
    ErpMfgSubcontractOrderRejectProcessor rejectProcessor;
    @Inject
    ErpMfgSubcontractOrderReverseApproveProcessor reverseApproveProcessor;
    @Inject
    ErpMfgSubcontractOrderWithdrawApprovalProcessor withdrawApprovalProcessor;
    @Inject
    ErpMfgSubcontractOrderApprovalStateMachine approvalStateMachine;
    @Inject
    ErpMfgSubcontractOrderDocumentStateMachine documentStateMachine;

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
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpMfgSubcontractOrder withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpMfgSubcontractOrder approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    public ErpMfgSubcontractOrder reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpMfgSubcontractOrder reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    public ErpMfgSubcontractOrder cancel(String subcontractOrderId, IServiceContext context) {
        ErpMfgSubcontractOrder order = requireOrder(subcontractOrderId, context);
        validateTransitionForCancel(order, context);
        order.setDocStatus(documentStateMachine.cancelTargetStatus());
        orderDao().updateEntity(order);
        return order;
    }

    // ---------- 三段业务动作（R6.2 per-mutation 拆分：public 入口已迁入 ----------
    // ----------   ErpMfgSubcontractOrder<Method>Processor，本类保留 protected step helper） ----------

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
        // P2-CK-mfg3-006（F2.5 范式）：红冲失败不再吞异常——中止 @BizMutation 保持 DONE+posted=true 可重试
        //（原「swallowed to keep idempotency」语义已显式重裁决：静默缺凭证无补偿面不可接受）
        mfgPostingExecutor.reverse(billHeadCode, businessType);
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
                LOG.warn("Subcontract reversal skipped inventory move reverse (move warehouse does not satisfy reversal precondition, when MANUFACTURE move sourceWarehouseId is empty"
                                + " its reversed bookkeeper destWarehouseId is also empty), subcontract order {} relatedBillType={} moveCode={}",
                        order.getCode(), relatedBillType, original.getCode());
                return;
            }
            stockMoveBiz.reverse(original.getId(), context);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("Subcontract reversal failed to reverse inventory move (exception swallowed to keep idempotency), subcontract order {} relatedBillType={}: {}",
                        order.getCode(), relatedBillType, e.getMessage());
            } else {
                LOG.error("Subcontract reversal error reversing inventory move (exception swallowed to keep idempotency), subcontract order {} relatedBillType={}",
                        order.getCode(), relatedBillType, e);
            }
        }
    }

    /**
     * 判断移动单是否可安全反向。库存域 {@code inverseMoveType} 反转 INCOMING/MANUFACTURE → OUTGOING、
     * OUTGOING → INCOMING（P1-CK-mfg3-002 修复后 MANUFACTURE 与 INCOMING 同语义：反向 OUTGOING
     * 从原 destWarehouseId 冲减）；其他类型保持不变（bookkeeper 非 OUTGOING 走 onIncoming 用 destWarehouseId）。
     * 故反向冲销的可用仓库取决于原单的 sourceWarehouseId（OUTGOING 反向）或 destWarehouseId（INCOMING/MANUFACTURE 反向）。
     */
    protected boolean canSafelyReverse(ErpInvStockMove original) {
        String moveType = original.getMoveType();
        if (Objects.equals(moveType, "INCOMING") || Objects.equals(moveType, "MANUFACTURE")) {
            return original.getDestWarehouseId() != null;
        }
        return original.getSourceWarehouseId() != null;
    }

    protected ErpMfgSubcontractOrder doReverseCompletion(ErpMfgSubcontractOrder order, IServiceContext context) {
        // F1.2 同型残余（委外红冲链）：reverseGlPostings/reverseInventoryMoves 内含 REQUIRES_NEW 红冲凭证
        // 与跨域反向移动，返回后当前 session 持有的 order 可能被 evict——按 ID 重载后再回退，
        // 避免 updateEntity 报 save-entity-not-transient（镜像 WorkOrder 报工链既有 reload 范式）。
        ErpMfgSubcontractOrder managed = orderDao().getEntityById(order.getId());
        if (managed == null) {
            managed = order;
        }
        managed.setPosted(false);
        managed.setPostedAt(null);
        managed.setPostedBy(null);
        managed.setDocStatus(documentStateMachine.reverseCompletionTargetStatus());
        orderDao().updateEntity(managed);
        return managed;
    }

    // ---------- step：审批迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpMfgSubcontractOrder order, IServiceContext context) {
        // P1-CK-mfg3-003：Pattern B override 绕过骨架 validateNotCancelled——补 docStatus 终态守卫
        // （cancel 只翻 docStatus=CANCELLED、approveStatus 不动，无此守卫可经 submit/approve 复活）。
        assertNotCancelled(order);
        String status = order.getApproveStatus();
        try {
            approvalStateMachine.assertCanSubmit(status);
        } catch (NopException e) {
            throw e.param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode());
        }
    }

    protected void validateTransitionForWithdraw(ErpMfgSubcontractOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        try {
            approvalStateMachine.assertCanWithdraw(status);
        } catch (NopException e) {
            throw e.param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode());
        }
    }

    protected void validateTransitionForApprove(ErpMfgSubcontractOrder order, IServiceContext context) {
        assertNotCancelled(order);
        String status = order.getApproveStatus();
        try {
            approvalStateMachine.assertCanApprove(status);
        } catch (NopException e) {
            throw e.param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode());
        }
    }

    protected void validateTransitionForReject(ErpMfgSubcontractOrder order, IServiceContext context) {
        assertNotCancelled(order);
        String status = order.getApproveStatus();
        try {
            approvalStateMachine.assertCanReject(status);
        } catch (NopException e) {
            throw e.param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode());
        }
    }

    /** P1-CK-mfg3-003：CANCELLED 为终态，任何审批迁移不得复活（骨架 validateNotCancelled 的 Pattern B 等价守卫）。 */
    protected void assertNotCancelled(ErpMfgSubcontractOrder order) {
        if (Objects.equals(order.getDocStatus(), ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED)) {
            throw new NopException(ErpMfgErrors.ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode())
                    .param(ErpMfgErrors.ARG_CURRENT_STATUS, order.getDocStatus())
                    .param(ErpMfgErrors.ARG_EXPECTED_STATUS, "!CANCELLED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpMfgSubcontractOrder order, IServiceContext context) {
        // P2-CK-mfg3-012：docStatus 守卫——仅 APPROVED（未发料）可反审核
        String _ds = order.getDocStatus();
        if (_ds != null && !"APPROVED".equals(_ds)) {
            throw new IllegalStateException("Subcontract order docStatus=" + _ds + ", only APPROVED allows reverseApprove (P2-CK-mfg3-012)");
        }
        String status = order.getApproveStatus();
        try {
            approvalStateMachine.assertCanReverseApprove(status);
        } catch (NopException e) {
            throw e.param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode());
        }
    }

    /** cancel 守卫：来源 {DRAFT, SUBMITTED, APPROVED}（固定来源态委托 Document Bean）。 */
    protected void validateTransitionForCancel(ErpMfgSubcontractOrder order, IServiceContext context) {
        String status = order.getDocStatus();
        try {
            documentStateMachine.assertCanCancel(status);
        } catch (NopException e) {
            throw e.param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode());
        }
    }

    // ---------- step：审批执行 ----------

    protected void doSubmit(ErpMfgSubcontractOrder order, IServiceContext context) {
        order.setApproveStatus(approvalStateMachine.submitTargetStatus());
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED);
        orderDao().updateEntity(order);
    }

    protected void doWithdrawSubmit(ErpMfgSubcontractOrder order, IServiceContext context) {
        order.setApproveStatus(approvalStateMachine.withdrawTargetStatus());
        orderDao().updateEntity(order);
    }

    protected void doApprove(ErpMfgSubcontractOrder order, IServiceContext context) {
        SoDGuard.assertApproverNotCreator(order.getCreatedBy(), currentUserId(), ErpMfgErrors.ERR_MFG_APPROVER_IS_CREATOR);
        order.setApproveStatus(approvalStateMachine.approveTargetStatus());
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED);
        order.setApprovedBy(currentUserId());
        order.setApprovedAt(CoreMetrics.currentTimestamp());
        orderDao().updateEntity(order);
    }

    protected void doReject(ErpMfgSubcontractOrder order, IServiceContext context) {
        order.setApproveStatus(approvalStateMachine.rejectTargetStatus());
        // Subcontract 独有：reject 联动写 docStatus=REJECTED（与 WorkOrder 不同；联动写入保留原位，契约 §9.2 选项 c）
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_REJECTED);
        orderDao().updateEntity(order);
    }

    protected void doReverseApprove(ErpMfgSubcontractOrder order, IServiceContext context) {
        order.setApproveStatus(approvalStateMachine.reverseApproveTargetStatus());
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
                                     List<ErpMfgSubcontractOrderLine> lines, String sourceWarehouseId,
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
                                        String destWarehouseId, IServiceContext context) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpMfgConstants.MOVE_TYPE_MANUFACTURING);
        request.setOrgId(order.getOrgId());
        request.setBusinessDate(order.getBusinessDate() != null ? order.getBusinessDate() : CoreMetrics.today());
        request.setDestWarehouseId(destWarehouseId);
        request.setCurrencyId(order.getCurrencyId());
        request.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));
        request.setRelatedBillType(ErpMfgConstants.RELATED_BILL_TYPE_MFG_SUBCONTRACT_RECEIPT);
        request.setRelatedBillCode(order.getCode());

        BigDecimal unitCost = computeReceiptUnitCost(order, receivedQty, context);

        List<StockMoveLineRequest> moveLines = new ArrayList<>();
        StockMoveLineRequest ml = new StockMoveLineRequest();
        ml.setMaterialId(order.getProductId());
        String uomId = resolveProductUomId(order.getProductId());
        ml.setUoMId(uomId);
        ml.setQuantity(receivedQty);
        ml.setUnitCost(unitCost);
        ml.setCurrencyId(order.getCurrencyId());
        moveLines.add(ml);
        request.setLines(moveLines);
        stockMoveBiz.generateMove(request, context);
    }

    /**
     * 委外成品单位成本 =（发料材料成本 + 加工费）/ 收货数量（P1-CK-mfg3-001 修复）。
     * 修复前分子仅含头 processingFee——产成品存货低估（缺材料成本）+ 1408 委外物资科目
     * 每单沉淀一笔永不结清的材料成本净额。材料成本 = 发料移动单（MFG_SUBCONTRACT_ISSUE）
     * 流水 totalCost 绝对值合计（与 {@code SubcontractPostingDispatcher} SI 段口径一致）。
     */
    protected BigDecimal computeReceiptUnitCost(ErpMfgSubcontractOrder order, BigDecimal receivedQty,
                                                IServiceContext context) {
        if (receivedQty == null || receivedQty.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee = nz(order.getProcessingFee());
        BigDecimal materialCost = aggregateIssueMaterialCost(order, context);
        BigDecimal total = fee.add(materialCost);
        return total.divide(receivedQty, 4, RoundingMode.HALF_UP);
    }

    /**
     * 聚合委外发料移动单流水材料成本（|totalCost| 合计）。经 relatedBillType+relatedBillCode 反查
     * 发料 OUTGOING 移动单 → 流水 totalCost 为负（出库扣减）取绝对值。无移动单/无流水 → 0。
     * 跨域读经 I*Biz（对齐跨实体访问纪律，避免新增 daoFor 站点）。
     */
    protected BigDecimal aggregateIssueMaterialCost(ErpMfgSubcontractOrder order, IServiceContext context) {
        if (order == null || order.getCode() == null) {
            return BigDecimal.ZERO;
        }
        ErpInvStockMove issueMove = stockMoveBiz.findByRelatedBill(
                ErpMfgConstants.RELATED_BILL_TYPE_MFG_SUBCONTRACT_ISSUE, order.getCode(), context);
        if (issueMove == null) {
            return BigDecimal.ZERO;
        }
        ormTemplate.flushSession();
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("moveId", issueMove.getId()));
        List<ErpInvStockLedger> ledgers = stockLedgerBiz.findList(lq, null, context);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpInvStockLedger l : ledgers) {
            sum = sum.add(nz(l.getTotalCost()).abs());
        }
        return sum;
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
        try {
            if (ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED.equals(expected)) {
                // issueMaterials 固定守卫：仅 APPROVED
                documentStateMachine.assertCanIssueMaterials(current);
            } else if (ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED.equals(expected)) {
                // receiveFinished 固定守卫：仅 ISSUED
                documentStateMachine.assertCanReceiveFinished(current);
            } else if (ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED.equals(expected)) {
                // postProcessingFee 固定守卫：仅 RECEIVED
                documentStateMachine.assertCanPostProcessingFee(current);
            } else {
                // 防御性兜底（当前调用方仅传 APPROVED/ISSUED/RECEIVED）：直抛实体终码
                // （plan 2026-09-07-2200-1），subcontractOrderCode 由下方 catch 同码补参
                throw new NopException(ErpMfgErrors.ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION)
                        .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                        .param(ErpMfgErrors.ARG_EXPECTED_STATUS, expectedLabel);
            }
        } catch (NopException e) {
            throw e.param(ErpMfgErrors.ARG_SUBCONTRACT_ORDER_CODE, order.getCode());
        }
    }

    protected List<ErpMfgSubcontractOrderLine> loadLines(String subcontractOrderId) {
        IEntityDao<ErpMfgSubcontractOrderLine> dao = daoProvider.daoFor(ErpMfgSubcontractOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("subcontractOrderId", subcontractOrderId));
        return dao.findAllByQuery(q);
    }

    protected BigDecimal sumLineQuantity(String subcontractOrderId) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpMfgSubcontractOrderLine line : loadLines(subcontractOrderId)) {
            total = total.add(nz(line.getQuantity()));
        }
        return total;
    }

    protected String resolveProductUomId(String productId) {
        if (productId == null) {
            return null;
        }
        ErpMdMaterial product = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(productId);
        return product != null ? product.getUoMId() : null;
    }

    protected String resolveAcctSchemaId(String orgId) {
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
            // P2-CK-mfg3-006：F2.5 范式——红冲/反向移动失败不再吞异常，中止保持可重试
            throw e;
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
            LOG.warn("currentUserId resolution failed (degraded): {}", e.getMessage());            return null;
        }
    }
}
