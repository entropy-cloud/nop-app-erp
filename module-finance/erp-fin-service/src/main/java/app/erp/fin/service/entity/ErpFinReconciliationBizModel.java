
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.dto.AutoReconResult;
import app.erp.fin.dao.dto.DualSideDiffReport;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.dto.ReconciliationReversePreview;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.processor.ErpFinReconciliationCreateProcessor;
import app.erp.fin.service.processor.ErpFinReconciliationPostProcessor;
import app.erp.fin.service.processor.ErpFinReconciliationReverseProcessor;
import app.erp.fin.service.processor.ErpFinReconciliationRunAutoReconciliationProcessor;
import app.erp.fin.service.reconciliation.DualSideConsistencyChecker;
import app.erp.fin.service.statemachine.ErpFinReconciliationDocumentStateMachine;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 核销单聚合根 Biz（{@code ar-ap-reconciliation.md}）。CRUD 之外承载 create/post/reverse/runAutoReconciliation
 * 四个 @BizMutation，分别委派对应 per-mutation Processor；previewReverse/checkDualSideConsistency 为只读 @BizQuery
 * （保留 BizModel）。
 *
 * <p>核销单是 finance 域 GL/账龄视角的正式核销（period-end 正式核销），独立作用于辅助账
 * {@link ErpFinArApItem}；purchase/sales 域级核销作为运营核销权威并行。
 *
 * <p>核销不直接生成 GL 凭证（凭证由收付款审核时生成，{@code ar-ap-reconciliation.md §核销流程} 步骤5）。
 * 事务入口钉在 {@code @BizMutation}。
 */
@BizModel("ErpFinReconciliation")
public class ErpFinReconciliationBizModel extends CrudBizModel<ErpFinReconciliation> implements IErpFinReconciliationBiz {

    @Inject
    DualSideConsistencyChecker dualSideConsistencyChecker;
    @Inject
    ErpFinReconciliationCreateProcessor createProcessor;
    @Inject
    ErpFinReconciliationPostProcessor postProcessor;
    @Inject
    ErpFinReconciliationReverseProcessor reverseProcessor;
    @Inject
    ErpFinReconciliationRunAutoReconciliationProcessor runAutoReconciliationProcessor;
    @Inject
    ErpFinReconciliationDocumentStateMachine stateMachine;

    public ErpFinReconciliationBizModel() {
        setEntityName(ErpFinReconciliation.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinReconciliation create(@Name("direction") String direction,
                                       @Name("partnerId") String partnerId,
                                       @Name("businessDate") LocalDate businessDate,
                                       @Name("lines") List<ReconciliationLineInput> lines,
                                       IServiceContext context) {
        return createProcessor.create(direction, partnerId, businessDate, lines, context);
    }

    @Override
    @BizMutation
    public ErpFinReconciliation post(@Name("reconciliationId") String reconciliationId, IServiceContext context) {
        return postProcessor.post(reconciliationId, context);
    }

    @Override
    @BizMutation
    public ErpFinReconciliation reverse(@Name("reconciliationId") String reconciliationId, IServiceContext context) {
        return reverseProcessor.reverse(reconciliationId, context);
    }

    /**
     * F7 §3 核销单冲销预览。只读 @BizQuery，不执行实际冲销。镜像 reverse 的前置校验（须 POSTED），
     * 预览双方辅助账回退项 + partner 余额刷新影响，供前端 dialog 展示后再确认执行。
     */
    @Override
    @BizQuery
    public ReconciliationReversePreview previewReverse(@Name("reconciliationId") String reconciliationId,
                                                       IServiceContext context) {
        ErpFinReconciliation head = requireHead(reconciliationId, context);
        try {
            stateMachine.assertCanReverse(head.getDocStatus());
        } catch (NopException e) {
            if (ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode().equals(e.getErrorCode())) {
                throw statusError(head, e);
            }
            throw e;
        }
        List<ErpFinReconciliationLine> lines = loadLines(reconciliationId);

        ReconciliationReversePreview preview = new ReconciliationReversePreview();
        preview.setReconciliationId(head.getId());
        preview.setCode(head.getCode());
        preview.setDirection(head.getDirection());
        preview.setTotalAmountFunctional(nz(head.getTotalAmountFunctional()));
        preview.setPartnerId(head.getPartnerId());
        preview.setWillSetReversed(true);
        preview.setWillRefreshPartnerBalance(true);

        for (ErpFinReconciliationLine line : lines) {
            BigDecimal restore = nz(line.getSettledAmountFunctional());
            preview.getRevertedItems().add(toRevertedItem(line.getPaymentItemId(), "payment",
                    loadItem(line.getPaymentItemId()), restore));
            preview.getRevertedItems().add(toRevertedItem(line.getInvoiceItemId(), "invoice",
                    loadItem(line.getInvoiceItemId()), restore));
        }
        return preview;
    }

    // ---------- 自动核销 ----------

    @Override
    @BizMutation
    public AutoReconResult runAutoReconciliation(@Name("direction") String direction,
                                                  @Name("partnerId") String partnerId,
                                                  @Name("strategy") String strategy,
                                                  IServiceContext context) {
        return runAutoReconciliationProcessor.runAutoReconciliation(direction, partnerId, strategy, context);
    }

    @Override
    @BizQuery
    public DualSideDiffReport checkDualSideConsistency(@Name("direction") String direction,
                                                       @Name("partnerId") String partnerId,
                                                       IServiceContext context) {
        IServiceContext ctx = context != null ? context : new ServiceContextImpl();
        return dualSideConsistencyChecker.check(direction, partnerId, ctx);
    }

    // ---------- previewReverse helpers ----------

    private ReconciliationReversePreview.RevertedItem toRevertedItem(String itemId, String side,
                                                                     ErpFinArApItem item, BigDecimal restoreAmount) {
        ReconciliationReversePreview.RevertedItem ri = new ReconciliationReversePreview.RevertedItem();
        ri.setArApItemId(itemId);
        ri.setSourceBillType(item.getSourceBillType());
        ri.setSourceBillCode(item.getSourceBillCode());
        ri.setSide(side);
        ri.setCurrentStatus(item.getStatus());
        ri.setRestoreAmountFunctional(restoreAmount);
        ri.setWillBecomeStatus(estimateStatusAfterRevert(item, restoreAmount));
        return ri;
    }

    /** 反推 reverseSettle 后的辅助账状态：回退后已核销额 ≤ 0 → OPEN，否则 PARTIAL。 */
    private static String estimateStatusAfterRevert(ErpFinArApItem item, BigDecimal restoreAmount) {
        BigDecimal remainingSettled = nz(item.getSettledAmountFunctional()).subtract(restoreAmount);
        if (remainingSettled.compareTo(BigDecimal.ZERO) <= 0) {
            return ErpFinConstants.AR_AP_STATUS_OPEN;
        }
        return ErpFinConstants.AR_AP_STATUS_PARTIAL;
    }

    protected ErpFinReconciliation requireHead(String id, IServiceContext context) {
        ErpFinReconciliation head = get(String.valueOf(id), true, context);
        if (head == null) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_NOT_FOUND)
                    .param(ErpFinErrors.ARG_RECONCILIATION_ID, id);
        }
        return head;
    }

    protected List<ErpFinReconciliationLine> loadLines(String reconciliationId) {
        IEntityDao<ErpFinReconciliationLine> dao = daoProvider().daoFor(ErpFinReconciliationLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("reconciliationId", reconciliationId));
        List<ErpFinReconciliationLine> lines = new ArrayList<>(dao.findAllByQuery(q));
        lines.sort((a, b) -> Integer.compare(
                a.getLineNo() == null ? Integer.MAX_VALUE : a.getLineNo(),
                b.getLineNo() == null ? Integer.MAX_VALUE : b.getLineNo()));
        return lines;
    }

    protected ErpFinArApItem loadItem(String id) {
        IEntityDao<ErpFinArApItem> dao = daoProvider().daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = dao.getEntityById(id);
        if (item == null) {
            throw new NopException(ErpFinErrors.ERR_AR_AP_ITEM_NOT_FOUND)
                    .param(ErpFinErrors.ARG_ID, id);
        }
        return item;
    }

    protected NopException statusError(ErpFinReconciliation head) {
        return statusError(head, null);
    }

    /**
     * 领域码 {@code ERR_RECONCILIATION_STATUS_INVALID}（Bean common 码作 cause 保留，契约 §7）。
     * 参数 reconciliationId/docStatus 由本层组装（唯一真相源在实体），cause 来自状态机 Bean 非法边。
     */
    protected NopException statusError(ErpFinReconciliation head, NopException cause) {
        return new NopException(ErpFinErrors.ERR_RECONCILIATION_STATUS_INVALID, cause)
                .param(ErpFinErrors.ARG_RECONCILIATION_ID, head.getId())
                .param(ErpFinErrors.ARG_DOC_STATUS, head.getDocStatus());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
