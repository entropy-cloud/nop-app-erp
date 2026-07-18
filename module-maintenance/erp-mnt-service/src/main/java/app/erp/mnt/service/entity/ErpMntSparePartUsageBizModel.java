package app.erp.mnt.service.entity;

import app.erp.inv.biz.IErpInvStockMoveBiz;
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
import java.util.List;
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

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ErpMntSparePartUsageBizModel.class);

    @Inject
    SparePartIssueService sparePartIssueService;

    @Inject
    MaintenanceIssuePostingDispatcher issuePostingDispatcher;

    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

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

    @Override
    @BizMutation
    public ErpMntSparePartUsage reverseConfirm(@Name("usageId") Long usageId, IServiceContext context) {
        ErpMntSparePartUsage usage = requireUsage(usageId, context);
        validateCanReverse(usage, context);

        // 1. 红冲 MAINTENANCE_ISSUE 凭证（try/catch 吞异常告警保持幂等，对齐 dispatchIfApplicable 正向过账范式；
        //    IErpFinVoucherBiz.reverse platform 内置幂等守护，无凭证时安全 no-op）
        try {
            issuePostingDispatcher.reverseIssue(usage);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("备件消耗红冲 GL 凭证失败（吞异常保持幂等），消耗单 {} billHeadCode={}: {}",
                        usage.getCode(), usage.getCode() + "-MI", e.getMessage());
            } else {
                LOG.error("备件消耗红冲 GL 凭证异常（吞异常保持幂等），消耗单 {} billHeadCode={}",
                        usage.getCode(), usage.getCode() + "-MI", e);
            }
        }

        // 2. 反向 OUTGOING 库存移动单（经 relatedBillType+relatedBillCode 反查原移动单 → IErpInvStockMoveBiz.reverse
        //    生成 REVERSAL 反向移动单，余额自动回滚，对齐 1934-1 委外红冲范式）
        ErpInvStockMove originalMove = findIssueMove(usage.getCode());
        if (originalMove != null) {
            try {
                stockMoveBiz.reverse(originalMove.getId(), context);
            } catch (Exception e) {
                if (e instanceof NopException) {
                    LOG.warn("备件消耗红冲反向库存移动失败（吞异常保持幂等），消耗单 {} moveCode={}: {}",
                            usage.getCode(), originalMove.getCode(), e.getMessage());
                } else {
                    LOG.error("备件消耗红冲反向库存移动异常（吞异常保持幂等），消耗单 {} moveCode={}",
                            usage.getCode(), originalMove.getCode(), e);
                }
            }
        }

        // 3. 翻 posted=false + docStatus=CANCELLED（状态机终态）。
        //    跨域 reverse 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化（对齐 confirm 范式）。
        usage = daoProvider().daoFor(ErpMntSparePartUsage.class).getEntityById(usageId);
        doReverseConfirm(usage, context);
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

    /**
     * 红冲前置守卫：仅 posted=true 且 docStatus=ACTIVE（已 confirm 态）的消耗单可红冲。
     * 未过账或已 CANCELLED 抛 ERR_SPARE_PART_USAGE_NOT_POSTED（前置条件不满足，统一守卫）。
     */
    protected void validateCanReverse(ErpMntSparePartUsage usage, IServiceContext context) {
        String docStatus = usage.getDocStatus();
        if (!Boolean.TRUE.equals(usage.getPosted())
                || !Objects.equals(docStatus, ErpMntDaoConstants.DOC_STATUS_ACTIVE)) {
            throw new NopException(ErpMntErrors.ERR_SPARE_PART_USAGE_NOT_POSTED)
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
            usage.setPostedAt(CoreMetrics.currentTimestamp());
        }
        usage.setTotalAmount(aggregateAmount(lines));
    }

    protected boolean isStockIssued(ErpInvStockMove move) {
        String status = move.getDocStatus();
        return status != null && Objects.equals(status, ErpMntConstants.STOCK_MOVE_DOC_STATUS_DONE);
    }

    /**
     * 翻 posted=false + docStatus=CANCELLED（红冲终态）。对齐 {@link #applyIssueResult} 反向操作。
     */
    protected void doReverseConfirm(ErpMntSparePartUsage usage, IServiceContext context) {
        usage.setDocStatus(ErpMntDaoConstants.DOC_STATUS_CANCELLED);
        usage.setPosted(false);
        updateEntity(usage, null, context);
    }

    /**
     * 反查备件消耗单关联的 OUTGOING 移动单（按 {@code relatedBillType=ERP_MNT_SPARE_PART}+{@code relatedBillCode=usage.code}）。
     * 不存在返回 null（红冲步骤对此容忍）。
     */
    protected ErpInvStockMove findIssueMove(String usageCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider().daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpMntConstants.RELATED_BILL_TYPE_MNT_SPARE_PART));
        q.addFilter(eq("relatedBillCode", usageCode));
        q.setLimit(1);
        List<ErpInvStockMove> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
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
