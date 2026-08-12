package app.erp.crm.service.processor;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.service.ErpCrmConfigs;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.statemachine.ErpCrmLeadStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 线索/商机状态机 + 漏斗阶段流转编排 Processor（{@code processor-extension-pattern.md} Facade + Processor）。
 *
 * <p>R6.6 slim-to-S-delegation：{@code qualify}/{@code lose}/{@code moveStage} 三个 D-mutation 已拆为独立
 * {@link ErpCrmLeadQualifyProcessor}/{@link ErpCrmLeadLoseProcessor}/{@link ErpCrmLeadMoveStageProcessor}。
 * 本类保留为共享 protected helper 单一真相源 + {@code cancel} S-mutation 单行委托。
 *
 * <p>状态机（docStatus）：{@code NEW → QUALIFIED}（入漏斗）、{@code NEW/QUALIFIED → LOST}（丢单原因必填）、
 * {@code NEW/QUALIFIED → CANCELLED}、{@code → CONVERTED}（终态，由转化 Processor 置位）。
 * 非法迁移被拒。每个 step 方法 protected，下游可逐 step 覆盖。
 *
 * <p>阶段流转（moveStage）：stageId 沿 {@link ErpCrmStage#getSequence()} 单向递增（owner doc §stageId 迁移规则），
 * 回退经 {@code erp-crm.allow-stage-backward}=true 放行（{@link ErpCrmConfigs#allowStageBackward()}），
 * 全量写 {@link ErpCrmLeadConvLog} 留痕（审计不丢）；probability 为空时取目标阶段 defaultProbability。
 */
public class ErpCrmLeadProcessor {

    static final Logger LOG = LoggerFactory.getLogger(ErpCrmLeadProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpCrmLeadStateMachine stateMachine;

    @Inject
    ErpCrmLeadCancelProcessor cancelProcessor;

    public ErpCrmLead cancel(Long leadId, IServiceContext context) {
        return cancelProcessor.cancel(String.valueOf(leadId), context);
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForQualify(ErpCrmLead lead, IServiceContext context) {
        String status = currentStatus(lead);
        try {
            stateMachine.assertCanQualify(status);
        } catch (NopException e) {
            throw illegalTransition(lead, status, ErpCrmConstants.DOC_STATUS_NEW, e);
        }
    }

    protected void validateTransitionForLose(ErpCrmLead lead, IServiceContext context) {
        String status = currentStatus(lead);
        try {
            stateMachine.assertCanLose(status);
        } catch (NopException e) {
            throw illegalTransition(lead, status,
                    ErpCrmConstants.DOC_STATUS_NEW + "/" + ErpCrmConstants.DOC_STATUS_QUALIFIED, e);
        }
    }

    protected void validateTransitionForCancel(ErpCrmLead lead, IServiceContext context) {
        String status = currentStatus(lead);
        try {
            stateMachine.assertCanCancel(status);
        } catch (NopException e) {
            throw illegalTransition(lead, status,
                    ErpCrmConstants.DOC_STATUS_NEW + "/" + ErpCrmConstants.DOC_STATUS_QUALIFIED, e);
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

    /**
     * stageId 单向递增守卫（owner doc §stageId 迁移规则 + §审查提示「sequence 单向递增约束」）。
     * 当 fromStageId 非 null（已入漏斗）时比较 from/to 的 sequence：回退（toSeq {@literal <} fromSeq）在 STRICT
     * 模式（{@link ErpCrmConfigs#allowStageBackward()}=false，默认）抛 {@link ErpCrmErrors#ERR_STAGE_BACKWARD_MOVE}；
     * allow-backward=true 时 LOG.warn 放行（保留 convLog 审计）。fromStageId 为 null（首次入漏斗）跳过方向校验。
     */
    protected void validateStageDirection(ErpCrmLead lead, Long fromStageId, ErpCrmStage toStage,
                                          IServiceContext context) {
        if (fromStageId == null) {
            return;
        }
        ErpCrmStage fromStage = stageDao().getEntityById(fromStageId);
        Integer fromSeq = fromStage != null ? fromStage.getSequence() : null;
        Integer toSeq = toStage.getSequence();
        if (fromSeq != null && toSeq != null && toSeq < fromSeq) {
            if (ErpCrmConfigs.allowStageBackward()) {
                LOG.warn("erp-crm-lead: 允许阶段回退（allow-stage-backward=true）：leadCode={}, fromSeq={}, toSeq={}",
                        lead.getCode(), fromSeq, toSeq);
                return;
            }
            throw new NopException(ErpCrmErrors.ERR_STAGE_BACKWARD_MOVE)
                    .param(ErpCrmErrors.ARG_LEAD_CODE, lead.getCode())
                    .param(ErpCrmErrors.ARG_FROM_SEQUENCE, fromSeq)
                    .param(ErpCrmErrors.ARG_TO_SEQUENCE, toSeq);
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
        lead.setDocStatus(stateMachine.qualifyTargetStatus());
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
        lead.setDocStatus(stateMachine.loseTargetStatus());
        lead.setLostReasonId(lostReasonId);
        if (lostReasonDesc != null) {
            lead.setLostReasonDesc(lostReasonDesc);
        }
        leadDao().updateEntity(lead);
    }

    protected void doCancel(ErpCrmLead lead, IServiceContext context) {
        lead.setDocStatus(stateMachine.cancelTargetStatus());
        leadDao().updateEntity(lead);
    }

    /**
     * 阶段流转：stageId 沿 sequence 单向递增（owner doc §stageId 迁移规则），回退经
     * {@code erp-crm.allow-stage-backward}=true 放行（见 {@link #validateStageDirection}）；写 convLog 全量留痕；
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
        log.setChangedAt(CoreMetrics.currentTimestamp());
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

    /** 领域非法迁移异常构造；{@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7）。 */
    protected NopException illegalTransition(ErpCrmLead lead, String current, String expected, Throwable cause) {
        return new NopException(ErpCrmErrors.ERR_LEAD_ILLEGAL_STATUS_TRANSITION, cause)
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
