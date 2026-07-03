package app.erp.crm.biz;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalQuotation;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

/**
 * 线索/商机业务接口。除标准 CRUD 外，定义 docStatus 状态机（qualify/lose/cancel）+ 漏斗阶段流转（moveStage）
 * + 线索查重，并继承 {@link IErpCrmConversionBiz} 的转化闭环（convertToCustomer/convertToQuotation）。
 *
 * <p>对齐 {@code docs/design/crm/README.md §业务规则1 转化流 / §衔接契约}。
 */
public interface IErpCrmLeadBiz extends ICrudBiz<ErpCrmLead>, IErpCrmConversionBiz {

    @BizMutation
    ErpCrmLead qualify(@Name("leadId") Long leadId, IServiceContext context);

    /**
     * 标记丢单。{@code lostReasonId} 标记为 {@link Optional}（GraphQL 层允许为空），
     * 由业务校验在缺失时抛 {@code ERR_LOST_REASON_REQUIRED}。
     */
    @BizMutation
    ErpCrmLead lose(@Name("leadId") Long leadId,
                    @Optional @Name("lostReasonId") Long lostReasonId,
                    @Optional @Name("lostReasonDesc") String lostReasonDesc,
                    IServiceContext context);

    @BizMutation
    ErpCrmLead cancel(@Name("leadId") Long leadId, IServiceContext context);

    @BizMutation
    ErpCrmLead moveStage(@Name("leadId") Long leadId,
                         @Name("toStageId") Long toStageId,
                         IServiceContext context);

    /**
     * 按 companyName/contactEmail/contactPhone 检测既有非终态重复线索。
     * 配置 {@code erp-crm.auto-convert-duplicate-lead=false}（默认）时仅返回候选，不阻断保存。
     */
    @BizQuery
    List<ErpCrmLead> findDuplicates(@Name("leadId") Long leadId, IServiceContext context);
}
