package app.erp.mnt.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntSparePartUsageLine;
import app.erp.mnt.service.ErpMntConstants;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.posting.MaintenanceIssuePostingDispatcher;
import app.erp.mnt.service.statemachine.ErpMntSparePartUsageApprovalStateMachine;
import app.erp.mnt.service.statemachine.ErpMntSparePartUsageDocumentStateMachine;
import app.erp.mnt.service.support.SparePartIssueService;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 备件消耗单 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 confirm/reverseConfirm 两个 per-mutation Processor 共用的加载、状态守卫、行加载、金额聚合与移动单反查辅助（单一真相源）。
 * confirm/reverseConfirm 含备件消耗 GL 过账（会计保护区域），其 config-gating / try-catch 吞异常 / 跨域调用后 session-reload
 * 惯法逐字保留在各子类内（对照 TestErpMntSparePartPosting 校验语义不变）。
 */
public abstract class AbstractErpMntSparePartUsageProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    SparePartIssueService sparePartIssueService;

    @Inject
    MaintenanceIssuePostingDispatcher issuePostingDispatcher;

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Inject
    ErpMntSparePartUsageDocumentStateMachine documentStateMachine;

    @Inject
    ErpMntSparePartUsageApprovalStateMachine approvalStateMachine;

    protected IEntityDao<ErpMntSparePartUsage> usageDao() {
        return daoProvider.daoFor(ErpMntSparePartUsage.class);
    }

    protected ErpMntSparePartUsage requireUsage(Long usageId, IServiceContext context) {
        ErpMntSparePartUsage usage = usageDao().getEntityById(usageId);
        if (usage == null) {
            throw new NopException(ErpMntErrors.ERR_USAGE_NOT_FOUND).param(ErpMntErrors.ARG_USAGE_ID, usageId);
        }
        return usage;
    }

    protected void validateNotConfirmed(ErpMntSparePartUsage usage, IServiceContext context) {
        if (Boolean.TRUE.equals(usage.getPosted())) {
            return;
        }
        String docStatus = usage.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpMntDaoConstants.DOC_STATUS_ACTIVE)) {
            return;
        }
    }

    protected void validateLinesNonEmpty(ErpMntSparePartUsage usage,
                                          List<ErpMntSparePartUsageLine> lines, IServiceContext context) {
        if (lines.isEmpty()) {
            throw new NopException(ErpMntErrors.ERR_USAGE_LINES_EMPTY)
                    .param(ErpMntErrors.ARG_USAGE_CODE, usage.getCode());
        }
    }

    protected void validateCanReverse(ErpMntSparePartUsage usage, IServiceContext context) {
        if (!Boolean.TRUE.equals(usage.getPosted())) {
            throw new NopException(ErpMntErrors.ERR_SPARE_PART_USAGE_NOT_POSTED)
                    .param(ErpMntErrors.ARG_USAGE_CODE, usage.getCode());
        }
        try {
            documentStateMachine.assertCanReverseConfirm(usage.getDocStatus());
        } catch (NopException e) {
            throw new NopException(ErpMntErrors.ERR_SPARE_PART_USAGE_NOT_POSTED, e)
                    .param(ErpMntErrors.ARG_USAGE_CODE, usage.getCode());
        }
    }

    protected void applyIssueResult(ErpMntSparePartUsage usage, List<ErpMntSparePartUsageLine> lines,
                                     ErpInvStockMove move, IServiceContext context) {
        usage.setDocStatus(documentStateMachine.confirmTargetStatus());
        usage.setApproveStatus(approvalStateMachine.confirmApproveTargetStatus());
        usage.setPosted(isStockIssued(move));
        if (Boolean.TRUE.equals(usage.getPosted())) {
            usage.setPostedAt(CoreMetrics.currentTimestamp());
        }
        usage.setTotalAmount(aggregateAmount(lines));
    }

    protected boolean isStockIssued(ErpInvStockMove move) {
        String status = move.getDocStatus();
        return status != null && Objects.equals(status, ErpMntConstants.STOCK_MOVE_DOC_STATUS_DONE);
    }

    protected void doReverseConfirm(ErpMntSparePartUsage usage, IServiceContext context) {
        usage.setDocStatus(documentStateMachine.reverseConfirmTargetStatus());
        usage.setPosted(false);
        usageDao().updateEntity(usage);
    }

    protected ErpInvStockMove findIssueMove(String usageCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpMntConstants.RELATED_BILL_TYPE_MNT_SPARE_PART));
        q.addFilter(eq("relatedBillCode", usageCode));
        q.setLimit(1);
        List<ErpInvStockMove> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected BigDecimal aggregateAmount(List<ErpMntSparePartUsageLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpMntSparePartUsageLine line : lines) {
            BigDecimal amount = line.getAmount();
            if (amount == null && line.getQuantity() != null && line.getUnitCost() != null) {
                amount = line.getQuantity().multiply(line.getUnitCost());
            }
            if (amount != null) {
                total = total.add(amount);
            }
        }
        return total;
    }

    protected List<ErpMntSparePartUsageLine> loadLines(Long usageId) {
        IEntityDao<ErpMntSparePartUsageLine> dao = daoProvider.daoFor(ErpMntSparePartUsageLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("sparePartUsageId", usageId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }
}
