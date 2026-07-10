package app.erp.mnt.service.entity;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mnt.biz.IErpMntSparePartUsageBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntSparePartUsageLine;
import app.erp.mnt.service.ErpMntConstants;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.posting.MaintenanceIssuePostingDispatcher;
import app.erp.mnt.service.support.SparePartIssueService;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.util.ArrayList;

import static io.nop.api.core.beans.FilterBeans.eq;

@BizModel("ErpMntSparePartUsage")
public class ErpMntSparePartUsageBizModel extends CrudBizModel<ErpMntSparePartUsage>
        implements IErpMntSparePartUsageBiz {

    @Inject
    SparePartIssueService sparePartIssueService;

    @Inject
    MaintenanceIssuePostingDispatcher issuePostingDispatcher;

    public ErpMntSparePartUsageBizModel() {
        setEntityName(ErpMntSparePartUsage.class.getName());
    }

    @Override
    @BizMutation
    public ErpMntSparePartUsage confirm(@Name("usageId") Long usageId, IServiceContext context) {
        ErpMntSparePartUsage usage = requireUsage(usageId, context);
        validateNotConfirmed(usage, context);
        java.util.List<ErpMntSparePartUsageLine> lines = loadLines(usageId);
        validateLinesNonEmpty(usage, lines, context);

        ErpInvStockMove move = sparePartIssueService.issue(usage, lines, context);

        // 跨域 generateMove 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化。
        usage = daoProvider().daoFor(ErpMntSparePartUsage.class).getEntityById(usageId);
        applyIssueResult(usage, lines, move, context);
        updateEntity(usage, null, context);

        // 备件消耗 GL 过账（Dr: 维修费用 / Cr: 存货），config-gated 默认关（dispatchIfApplicable 内部门控，
        // erp-mnt.spare-part-posting-enabled=false 时不生成凭证，向后兼容）。maintenance 域独占，InvPostingDispatcher
        // 对 ERP_MNT_SPARE_PART 显式跳过交由本域处理。镜像 ManufacturingIssuePostingDispatcher 显式调用范式。
        if (isStockIssued(move)) {
            issuePostingDispatcher.dispatchIfApplicable(usageId);
        }
        return usage;
    }

    // ---------- step：校验 ----------

    protected ErpMntSparePartUsage requireUsage(Long usageId, IServiceContext context) {
        ErpMntSparePartUsage usage = get(String.valueOf(usageId), false, context);
        if (usage == null) {
            throw new NopException(ErpMntErrors.ERR_USAGE_NOT_FOUND).param(ErpMntErrors.ARG_USAGE_ID, usageId);
        }
        return usage;
    }

    protected void validateNotConfirmed(ErpMntSparePartUsage usage, IServiceContext context) {
        if (Boolean.TRUE.equals(usage.getPosted())) {
            return; // 幂等：已确认出库
        }
        String docStatus = usage.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpMntDaoConstants.DOC_STATUS_ACTIVE)) {
            return;
        }
    }

    protected void validateLinesNonEmpty(ErpMntSparePartUsage usage,
                                         java.util.List<ErpMntSparePartUsageLine> lines, IServiceContext context) {
        if (lines.isEmpty()) {
            throw new NopException(ErpMntErrors.ERR_USAGE_LINES_EMPTY)
                    .param(ErpMntErrors.ARG_USAGE_CODE, usage.getCode());
        }
    }

    // ---------- step：执行 ----------

    protected void applyIssueResult(ErpMntSparePartUsage usage, java.util.List<ErpMntSparePartUsageLine> lines,
                                    ErpInvStockMove move, IServiceContext context) {
        usage.setDocStatus(ErpMntDaoConstants.DOC_STATUS_ACTIVE);
        usage.setApproveStatus(ErpMntConstants.APPROVE_STATUS_APPROVED);
        usage.setPosted(isStockIssued(move));
        if (Boolean.TRUE.equals(usage.getPosted())) {
            usage.setPostedAt(CoreMetrics.currentDateTime());
        }
        usage.setTotalAmount(aggregateAmount(lines));
    }

    protected boolean isStockIssued(ErpInvStockMove move) {
        String status = move.getDocStatus();
        return status != null && Objects.equals(status, ErpMntConstants.STOCK_MOVE_DOC_STATUS_DONE);
    }

    protected BigDecimal aggregateAmount(java.util.List<ErpMntSparePartUsageLine> lines) {
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

    protected java.util.List<ErpMntSparePartUsageLine> loadLines(Long usageId) {
        IEntityDao<ErpMntSparePartUsageLine> dao = daoProvider().daoFor(ErpMntSparePartUsageLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("sparePartUsageId", usageId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }
}
