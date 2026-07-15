package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmLeadBiz;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmTerritoryAssignmentRule;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.processor.ErpCrmConversionProcessor;
import app.erp.crm.service.processor.ErpCrmLeadProcessor;
import app.erp.crm.service.support.LeadDuplicateChecker;
import app.erp.crm.service.support.LeadScoringEngine;
import app.erp.crm.service.support.TerritoryAssignmentEngine;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 线索/商机 BizModel（Facade）。docStatus 状态机（qualify/lose/cancel）+ 漏斗阶段流转（moveStage）+ 线索查重
 * + 转化闭环（convertToCustomer/convertToQuotation）委托各自 Processor（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>对齐 {@code docs/design/crm/README.md §业务规则1 转化流 / §衔接契约 IErpCrmConversionBiz}。
 * 转化方法在 {@link IErpCrmLeadBiz}（继承 {@code IErpCrmConversionBiz}）上声明，本类实现。
 */
@BizModel("ErpCrmLead")
public class ErpCrmLeadBizModel extends CrudBizModel<ErpCrmLead> implements IErpCrmLeadBiz {

    @Inject
    ErpCrmLeadProcessor leadProcessor;

    @Inject
    ErpCrmConversionProcessor conversionProcessor;

    @Inject
    LeadDuplicateChecker duplicateChecker;

    @Inject
    LeadScoringEngine scoringEngine;

    @Inject
    TerritoryAssignmentEngine assignmentEngine;

    public ErpCrmLeadBizModel() {
        setEntityName(ErpCrmLead.class.getName());
    }

    @Override
    @BizMutation
    public ErpCrmLead qualify(@Name("leadId") Long leadId, IServiceContext context) {
        return leadProcessor.qualify(leadId, context);
    }

    @Override
    @BizMutation
    public ErpCrmLead lose(@Name("leadId") Long leadId,
                           @io.nop.api.core.annotations.core.Optional
                           @Name("lostReasonId") Long lostReasonId,
                           @io.nop.api.core.annotations.core.Optional
                           @Name("lostReasonDesc") String lostReasonDesc,
                           IServiceContext context) {
        return leadProcessor.lose(leadId, lostReasonId, lostReasonDesc, context);
    }

    @Override
    @BizMutation
    public ErpCrmLead cancel(@Name("leadId") Long leadId, IServiceContext context) {
        return leadProcessor.cancel(leadId, context);
    }

    @Override
    @BizMutation
    public ErpCrmLead moveStage(@Name("leadId") Long leadId,
                                @Name("toStageId") Long toStageId,
                                IServiceContext context) {
        return leadProcessor.moveStage(leadId, toStageId, context);
    }

    @Override
    @BizQuery
    public List<ErpCrmLead> findDuplicates(@Name("leadId") Long leadId, IServiceContext context) {
        ErpCrmLead lead = get(String.valueOf(leadId), true, context);
        return duplicateChecker.findDuplicates(lead, context);
    }

    // ---------- 区域分配（plan 2026-07-07-1100-1）----------

    @Override
    @BizMutation
    public ErpCrmLead assignLead(@Name("leadId") Long leadId, IServiceContext context) {
        ErpCrmLead lead = requireEntity(String.valueOf(leadId), null, context);
        List<ErpCrmTerritoryAssignmentRule> rules = loadActiveRules(lead.getOrgId());
        ErpCrmTerritoryAssignmentRule defaultRule = loadDefaultRule(lead.getOrgId());
        TerritoryAssignmentEngine.AssignmentResult result = assignmentEngine.assign(lead, rules, defaultRule);
        if (result == null) {
            return lead;
        }
        if (result.getTerritoryId() != null) {
            lead.setTerritoryId(result.getTerritoryId());
        }
        if (result.getTeamId() != null) {
            lead.setTeamId(result.getTeamId());
        }
        // ownerId 按分配方法范围 Decision：本期 MANUAL 降级 → ownerId 留空标记"待分配"，引擎不挑人。
        if (result.getOwnerId() != null) {
            lead.setOwnerId(result.getOwnerId());
        }
        updateEntity(lead, null, context);
        return lead;
    }

    @Override
    @BizMutation
    public ErpCrmLead reassignLead(@Name("leadId") Long leadId,
                                    @Optional @Name("territoryId") Long territoryId,
                                    @Optional @Name("teamId") Long teamId,
                                    @Optional @Name("ownerId") String ownerId,
                                    IServiceContext context) {
        ErpCrmLead lead = requireEntity(String.valueOf(leadId), null, context);
        if (territoryId != null) {
            lead.setTerritoryId(territoryId);
        }
        if (teamId != null) {
            lead.setTeamId(teamId);
        }
        if (ownerId != null) {
            lead.setOwnerId(ownerId);
        }
        updateEntity(lead, null, context);
        return lead;
    }

    // ---------- 转化闭环（IErpCrmConversionBiz）----------

    @Override
    @BizMutation
    public ErpMdPartner convertToCustomer(@Name("leadId") Long leadId, IServiceContext context) {
        return conversionProcessor.convertToCustomer(leadId, context);
    }

    @Override
    @BizMutation
    public ErpSalQuotation convertToQuotation(@Name("leadId") Long leadId,
                                              @io.nop.api.core.annotations.core.Optional
                                              @Name("quotationData") Map<String, Object> quotationData,
                                              IServiceContext context) {
        return conversionProcessor.convertToQuotation(leadId, quotationData, context);
    }

    @Override
    @BizMutation
    public ErpCrmLead getCreatedOpportunity(@Name("leadId") Long leadId, IServiceContext context) {
        return conversionProcessor.getCreatedOpportunity(leadId, context);
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCrmLead> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        // 查重默认仅提示不阻断（auto-convert-duplicate-lead=false）；候选结果可经 findDuplicates 查询。
        duplicateChecker.checkAndNotify(entityData.getEntity(), context);

        // config-gated：新建 Lead 且 ownerId/teamId 均空 → 调分配引擎匹配区域规则回写
        ErpCrmLead lead = entityData.getEntity();
        if (lead.getId() == null && lead.getTerritoryId() == null) {
            boolean autoAssign = io.nop.api.core.config.AppConfig.var(
                    ErpCrmConstants.CONFIG_TERRITORY_AUTO_ASSIGN_ON_CREATE, Boolean.TRUE);
            if (autoAssign) {
                List<ErpCrmTerritoryAssignmentRule> rules = loadActiveRules(lead.getOrgId());
                ErpCrmTerritoryAssignmentRule defaultRule = loadDefaultRule(lead.getOrgId());
                TerritoryAssignmentEngine.AssignmentResult result =
                        assignmentEngine.assign(lead, rules, defaultRule);
                if (result != null) {
                    if (result.getTerritoryId() != null) {
                        lead.setTerritoryId(result.getTerritoryId());
                    }
                    if (result.getTeamId() != null) {
                        lead.setTeamId(result.getTeamId());
                    }
                    if (result.getOwnerId() != null) {
                        lead.setOwnerId(result.getOwnerId());
                    }
                }
            }
        }
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpCrmLead> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        // config-gated：lead-scoring.recalc-on-lead-update=true 时线索字段变更触发重新评分。
        ErpCrmLead lead = entityData.getEntity();
        if (lead.getId() == null) {
            return;
        }
        boolean recalcEnabled = io.nop.api.core.config.AppConfig.var(
                app.erp.crm.service.ErpCrmConstants.CONFIG_LEAD_SCORING_RECALC_ON_LEAD_UPDATE, Boolean.TRUE);
        if (recalcEnabled) {
            scoringEngine.recalculateScore(lead.getId(),
                    app.erp.crm.service.ErpCrmConstants.TRIGGER_EVENT_LEAD_UPDATE, context);
        }
    }

    // ---------- 区域分配规则加载 ----------

    protected List<ErpCrmTerritoryAssignmentRule> loadActiveRules(Long orgId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        if (orgId != null) {
            q.addFilter(eq("orgId", orgId));
        }
        return assignmentRuleDao().findAllByQuery(q);
    }

    protected ErpCrmTerritoryAssignmentRule loadDefaultRule(Long orgId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        q.addFilter(eq("isDefault", Boolean.TRUE));
        if (orgId != null) {
            q.addFilter(eq("orgId", orgId));
        }
        q.setLimit(1);
        return assignmentRuleDao().findAllByQuery(q).stream().findFirst().orElse(null);
    }

    protected IEntityDao<ErpCrmTerritoryAssignmentRule> assignmentRuleDao() {
        return daoProvider().daoFor(ErpCrmTerritoryAssignmentRule.class);
    }

    

}
