package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * 线索/商机状态机 + 漏斗阶段流转编排 Processor（{@code processor-extension-pattern.md} Facade + Processor）。
 *
 * <p>状态机（docStatus）：{@code NEW → QUALIFIED}（入漏斗）、{@code NEW/QUALIFIED → LOST}（丢单原因必填）、
 * {@code NEW/QUALIFIED → CANCELLED}、{@code → CONVERTED}（终态，由转化 Processor 置位）。
 * 非法迁移被拒。每个 step 方法 protected，下游可逐 step 覆盖。
 *
 * <p>阶段流转（moveStage）：按 {@link ErpCrmStage#getSequence()} 允许前移/回退（销售流程中阶段可能反复），
 * 全量写 {@link ErpCrmLeadConvLog} 留痕（审计不丢）；probability 为空时取目标阶段 defaultProbability。
 */
public class ErpCrmLeadProcessor {

    @Inject
    IDaoProvider daoProvider;

    public ErpCrmLead qualify(Long leadId, IServiceContext context) {
        ErpCrmLead lead = requireLead(leadId, context);
        validateTransitionForQualify(lead, context);
        doQualify(lead, context);
        return lead;
    }

    public ErpCrmLead lose(Long leadId, Long lostReasonId, String lostReasonDesc, IServiceContext context) {
        ErpCrmLead lead = requireLead(leadId, context);
        validateTransitionForLose(lead, context);
        requireLostReason(lead, lostReasonId, context);
        doLose(lead, lostReasonId, lostReasonDesc, context);
        return lead;
    }

    public ErpCrmLead cancel(Long leadId, IServiceContext context) {
        ErpCrmLead lead = requireLead(leadId, context);
        validateTransitionForCancel(lead, context);
        doCancel(lead, context);
        return lead;
    }

    public ErpCrmLead moveStage(Long leadId, Long toStageId, IServiceContext context) {
        ErpCrmLead lead = requireLead(leadId, context);
        validateMovable(lead, context);
        ErpCrmStage toStage = requireStage(toStageId, context);
        Long fromStageId = lead.getStageId();
        doMoveStage(lead, toStage, fromStageId, context);
        return lead;
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForQualify(ErpCrmLead lead, IServiceContext context) {
        String status = currentStatus(lead);
        if (!Objects.equals(status, ErpCrmConstants.DOC_STATUS_NEW)) {
            throw illegalTransition(lead, status, "NEW");
        }
    }

    protected void validateTransitionForLose(ErpCrmLead lead, IServiceContext context) {
        String status = currentStatus(lead);
        if (!Objects.equals(status, ErpCrmConstants.DOC_STATUS_NEW)
                && !Objects.equals(status, ErpCrmConstants.DOC_STATUS_QUALIFIED)) {
            throw illegalTransition(lead, status, "NEW 或 QUALIFIED");
        }
    }

    protected void validateTransitionForCancel(ErpCrmLead lead, IServiceContext context) {
        String status = currentStatus(lead);
        if (!Objects.equals(status, ErpCrmConstants.DOC_STATUS_NEW)
                && !Objects.equals(status, ErpCrmConstants.DOC_STATUS_QUALIFIED)) {
            throw illegalTransition(lead, status, "NEW 或 QUALIFIED");
        }
    }

    /**
     * 仅非终态（NEW/QUALIFIED）线索可流转阶段。
     */
    protected void validateMovable(ErpCrmLead lead, IServiceContext context) {
        String status = currentStatus(lead);
        if (!Objects.equals(status, ErpCrmConstants.DOC_STATUS_NEW)
                && !Objects.equals(status, ErpCrmConstants.DOC_STATUS_QUALIFIED)) {
            throw illegalTransition(lead, status, "NEW 或 QUALIFIED");
        }
    }

    protected void requireLostReason(ErpCrmLead lead, Long lostReasonId, IServiceContext context) {
        if (lostReasonId == null) {
            throw new NopException(ErpCrmErrors.ERR_LOST_REASON_REQUIRED)
                    .param(ErpCrmErrors.ARG_LEAD_CODE, lead.getCode());
        }
    }

    // ---------- step：执行 ----------

    protected void doQualify(ErpCrmLead lead, IServiceContext context) {
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_QUALIFIED);
        if (lead.getStageId() == null) {
            ErpCrmStage first = findFirstStage(lead.getOrgId());
            if (first != null) {
                lead.setStageId(first.getId());
                applyDefaultProbability(lead, first);
            }
        }
        leadDao().updateEntity(lead);
    }

    protected void doLose(ErpCrmLead lead, Long lostReasonId, String lostReasonDesc, IServiceContext context) {
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_LOST);
        lead.setLostReasonId(lostReasonId);
        if (lostReasonDesc != null) {
            lead.setLostReasonDesc(lostReasonDesc);
        }
        leadDao().updateEntity(lead);
    }

    protected void doCancel(ErpCrmLead lead, IServiceContext context) {
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_CANCELLED);
        leadDao().updateEntity(lead);
    }

    /**
     * 阶段流转：允许前移/回退（销售流程阶段可能反复），写 convLog 全量留痕；
     * probability 为空时取目标阶段 defaultProbability。
     */
    protected void doMoveStage(ErpCrmLead lead, ErpCrmStage toStage, Long fromStageId, IServiceContext context) {
        lead.setStageId(toStage.getId());
        applyDefaultProbability(lead, toStage);
        leadDao().updateEntity(lead);
        writeConvLog(lead, fromStageId, toStage.getId(), context);
    }

    protected void writeConvLog(ErpCrmLead lead, Long fromStageId, Long toStageId, IServiceContext context) {
        ErpCrmLeadConvLog log = convLogDao().newEntity();
        log.setLeadId(lead.getId());
        log.setOrgId(lead.getOrgId());
        log.setFromStageId(fromStageId);
        log.setToStageId(toStageId);
        log.setChangedAt(CoreMetrics.currentDateTime());
        log.setChangedBy(currentUser(context));
        convLogDao().saveEntity(log);
    }

    protected void applyDefaultProbability(ErpCrmLead lead, ErpCrmStage stage) {
        if (lead.getProbability() == null && stage.getDefaultProbability() != null) {
            lead.setProbability(stage.getDefaultProbability());
        }
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpCrmLead requireLead(Long leadId, IServiceContext context) {
        ErpCrmLead lead = leadDao().getEntityById(leadId);
        if (lead == null) {
            throw new NopException(ErpCrmErrors.ERR_LEAD_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_LEAD_ID, leadId);
        }
        return lead;
    }

    protected ErpCrmStage requireStage(Long stageId, IServiceContext context) {
        if (stageId == null) {
            throw new NopException(ErpCrmErrors.ERR_STAGE_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_STAGE_ID, stageId);
        }
        ErpCrmStage stage = stageDao().getEntityById(stageId);
        if (stage == null) {
            throw new NopException(ErpCrmErrors.ERR_STAGE_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_STAGE_ID, stageId);
        }
        return stage;
    }

    protected ErpCrmStage findFirstStage(Long orgId) {
        // 无独立权限规则：漏斗阶段为全局配置记录，按 sequence 升序取首条作为默认入漏斗阶段。
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addOrderField("sequence", false);
        q.setLimit(1);
        return stageDao().findAllByQuery(q).stream().findFirst().orElse(null);
    }

    protected String currentStatus(ErpCrmLead lead) {
        String status = lead.getDocStatus();
        return status == null ? ErpCrmConstants.DOC_STATUS_NEW : status;
    }

    protected String currentUser(IServiceContext context) {
        return context != null && context.getUserId() != null ? context.getUserId() : null;
    }

    protected NopException illegalTransition(ErpCrmLead lead, String current, String expected) {
        return new NopException(ErpCrmErrors.ERR_LEAD_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCrmErrors.ARG_LEAD_CODE, lead.getCode())
                .param(ErpCrmErrors.ARG_CURRENT_STATUS, current)
                .param(ErpCrmErrors.ARG_EXPECTED_STATUS, expected);
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpCrmLead> leadDao() {
        return daoProvider.daoFor(ErpCrmLead.class);
    }

    protected IEntityDao<ErpCrmStage> stageDao() {
        return daoProvider.daoFor(ErpCrmStage.class);
    }

    protected IEntityDao<ErpCrmLeadConvLog> convLogDao() {
        return daoProvider.daoFor(ErpCrmLeadConvLog.class);
    }
}
