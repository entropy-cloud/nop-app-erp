package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsTimeEntryBiz;
import app.erp.cs.dao.entity.ErpCsTeam;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTimeEntry;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.commons.util.StringHelper;
import jakarta.inject.Inject;

import java.sql.Timestamp;

/**
 * 工单计时条目 BizModel（RC-R1.66，UC-CS-11 ⑤⑥⑦；plan D4 审批链）。
 *
 * <p>审批状态机（owner doc time-tracking.md §3.2 经 NULL 承载 DRAFT）：
 * NULL(DRAFT)/REJECTED --submit--> PENDING --approve--> APPROVED / --reject--> REJECTED（可改后重新 submit）。
 * 触发判定：isBillable=true 或 duration ≥ approval-threshold → PENDING；否则（或 auto-approve=true）直通 APPROVED。
 * 审批人链（§3.3，advisory）：工单 assignedToId → SLA 策略团队 teamLeaderId → config 主管兜底，全空 WARN 跳过。
 */
@BizModel("ErpCsTimeEntry")
public class ErpCsTimeEntryBizModel extends CrudBizModel<ErpCsTimeEntry> implements IErpCsTimeEntryBiz {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ErpCsTimeEntryBizModel.class);

    public ErpCsTimeEntryBizModel() {
        setEntityName(ErpCsTimeEntry.class.getName());
    }

    @Override
    @BizMutation
    public ErpCsTimeEntry submit(@Name("timeEntryId") String timeEntryId, IServiceContext context) {
        ErpCsTimeEntry entry = requireEntry(timeEntryId, context);
        String current = entry.getApprovalStatus();
        if (current != null && !ErpCsConstants.TIME_ENTRY_APPROVE_REJECTED.equals(current)) {
            throw illegalApprovalStatus(entry, "NULL(DRAFT) 或 REJECTED");
        }
        if (ErpCsConfigs.isTimeEntryRequireDescription() && StringHelper.isBlank(entry.getDescription())) {
            throw new NopException(ErpCsErrors.ERR_CS_TIME_ENTRY_DESCRIPTION_REQUIRED)
                    .param(ErpCsErrors.ARG_TIME_ENTRY_ID, entry.getId());
        }
        logResolvedApprover(entry, context);

        boolean needApproval = Boolean.TRUE.equals(entry.getIsBillable())
                || (entry.getDuration() != null && entry.getDuration() >= ErpCsConfigs.getTimeEntryApprovalThresholdMinutes());
        if (needApproval && !ErpCsConfigs.isTimeEntryAutoApprove()) {
            entry.setApprovalStatus(ErpCsConstants.TIME_ENTRY_APPROVE_PENDING);
        } else {
            // 不触发审批（不可计费且未超阈值）或 auto-approve=true：直通 APPROVED（plan D4）
            entry.setApprovalStatus(ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED);
            entry.setApprovedById(context.getUserId());
            entry.setApprovedAt(Timestamp.valueOf(CoreMetrics.currentDateTime()));
        }
        updateEntity(entry, null, context);
        return entry;
    }

    @Override
    @BizMutation
    public ErpCsTimeEntry approve(@Name("timeEntryId") String timeEntryId, IServiceContext context) {
        ErpCsTimeEntry entry = requireEntry(timeEntryId, context);
        if (!ErpCsConstants.TIME_ENTRY_APPROVE_PENDING.equals(entry.getApprovalStatus())) {
            throw illegalApprovalStatus(entry, ErpCsConstants.TIME_ENTRY_APPROVE_PENDING);
        }
        entry.setApprovalStatus(ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED);
        entry.setApprovedById(context.getUserId());
        entry.setApprovedAt(Timestamp.valueOf(CoreMetrics.currentDateTime()));
        updateEntity(entry, null, context);
        return entry;
    }

    @Override
    @BizMutation
    public ErpCsTimeEntry reject(@Name("timeEntryId") String timeEntryId,
                                 @Optional @Name("rejectReason") String rejectReason,
                                 IServiceContext context) {
        ErpCsTimeEntry entry = requireEntry(timeEntryId, context);
        if (!ErpCsConstants.TIME_ENTRY_APPROVE_PENDING.equals(entry.getApprovalStatus())) {
            throw illegalApprovalStatus(entry, ErpCsConstants.TIME_ENTRY_APPROVE_PENDING);
        }
        entry.setApprovalStatus(ErpCsConstants.TIME_ENTRY_APPROVE_REJECTED);
        if (!StringHelper.isBlank(rejectReason)) {
            // 无独立驳回原因列：追加 description 前缀承载（plan D4，owner doc 回填注记）
            entry.setDescription("[驳回] " + rejectReason.trim() + "; " + (entry.getDescription() == null ? "" : entry.getDescription()));
        }
        entry.setApprovedById(context.getUserId());
        entry.setApprovedAt(Timestamp.valueOf(CoreMetrics.currentDateTime()));
        updateEntity(entry, null, context);
        return entry;
    }

    /**
     * 审批人链解析（§3.3，advisory 审计日志；resolved approver 不持久化——approvedById 记录实际决策人，
     * 身份强校验超 L1 验收标准归 successor，plan D4）。
     */
    private void logResolvedApprover(ErpCsTimeEntry entry, IServiceContext context) {
        try {
            String approver = resolveApprover(entry);
            if (approver == null) {
                String fallback = ErpCsConfigs.getTimeEntryApproverId();
                approver = StringHelper.isBlank(fallback) ? null : fallback;
            }
            if (approver == null) {
                LOG.warn("计时条目[{}]审批人链解析全空（工单未分派/团队无负责人/无 config 兜底），跳过审批人登记", entry.getId());
            } else {
                LOG.info("计时条目[{}]解析审批人: {}", entry.getId(), approver);
            }
        } catch (Exception e) {
            LOG.warn("计时条目[{}]审批人链解析失败（advisory，不阻断提交）：{}", entry.getId(), e.getMessage());
        }
    }

    /** 工单 assignedToId → SLA 策略团队 teamLeaderId（ORM 关系 getter，E2）。 */
    private String resolveApprover(ErpCsTimeEntry entry) {
        ErpCsTicket ticket = entry.getTicket();
        if (ticket == null) {
            return null;
        }
        if (!StringHelper.isBlank(ticket.getAssignedToId())) {
            return ticket.getAssignedToId();
        }
        if (ticket.getSlaPolicy() != null) {
            ErpCsTeam team = ticket.getSlaPolicy().getTeam();
            if (team != null && !StringHelper.isBlank(team.getTeamLeaderId())) {
                return team.getTeamLeaderId();
            }
        }
        return null;
    }

    /** 镜像 {@link ErpCsTicketBizModel#requireTicket} 范式：null id 领域错误码，存在性走 requireEntity 管道。 */
    private ErpCsTimeEntry requireEntry(String timeEntryId, IServiceContext context) {
        if (timeEntryId == null) {
            throw new NopException(ErpCsErrors.ERR_CS_TIME_ENTRY_NOT_FOUND)
                    .param(ErpCsErrors.ARG_TIME_ENTRY_ID, timeEntryId);
        }
        return requireEntity(String.valueOf(timeEntryId), null, context);
    }

    private NopException illegalApprovalStatus(ErpCsTimeEntry entry, String expected) {
        return new NopException(ErpCsErrors.ERR_CS_TIME_ENTRY_ILLEGAL_APPROVAL_STATUS)
                .param(ErpCsErrors.ARG_TIME_ENTRY_ID, entry.getId())
                .param(ErpCsErrors.ARG_CURRENT_STATUS, entry.getApprovalStatus())
                .param(ErpCsErrors.ARG_EXPECTED_STATUS, expected);
    }

}
