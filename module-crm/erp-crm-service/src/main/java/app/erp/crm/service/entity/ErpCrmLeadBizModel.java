package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmLeadBiz;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.processor.ErpCrmConversionProcessor;
import app.erp.crm.service.processor.ErpCrmLeadProcessor;
import app.erp.crm.service.support.LeadDuplicateChecker;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

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
    }
}
