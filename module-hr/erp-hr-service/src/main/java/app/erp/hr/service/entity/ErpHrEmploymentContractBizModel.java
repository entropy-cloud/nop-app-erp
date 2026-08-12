
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.common.service.MaskHelper;
import app.erp.hr.biz.IErpHrEmploymentContractBiz;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.processor.ErpHrEmploymentContractExpireOverdueContractsProcessor;
import app.erp.hr.service.statemachine.ErpHrEmploymentContractStateMachine;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * 劳动合同 BizModel（use-cases.md UC-HR-07）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展到期扫描（scanExpiringContracts/expireOverdueContracts）+ 续签（renew）。
 *
 * <p>到期扫描由 {@code ErpHrContractExpiryJob}（定时 Job）调用，通知派发经 {@code IErpSysNotificationBiz}。
 */
@BizModel("ErpHrEmploymentContract")
public class ErpHrEmploymentContractBizModel extends CrudBizModel<ErpHrEmploymentContract>
        implements IErpHrEmploymentContractBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpHrEmploymentContractBizModel.class);

    @Inject
    ErpHrEmploymentContractExpireOverdueContractsProcessor expireOverdueContractsProcessor;
    @Inject
    ErpHrEmploymentContractStateMachine stateMachine;

    public ErpHrEmploymentContractBizModel() {
        setEntityName(ErpHrEmploymentContract.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrEmploymentContract> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpHrEmploymentContract entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(CoreMetrics.today());
        }
    }

    @Override
    @BizQuery
    public List<ErpHrEmploymentContract> scanExpiringContracts(@Optional @Name("warningDays") Integer warningDays,
                                                                IServiceContext context) {
        int window = warningDays != null ? warningDays : ErpHrConfigs.contractExpiryWarningDays();
        LocalDate now = CoreMetrics.today();
        LocalDate windowEnd = now.plusDays(window);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpHrConstants.CONTRACT_STATUS_ACTIVE));
        q.addFilter(dateBetween("endDate", now, windowEnd));
        return findList(q, null, context);
    }

    @Override
    @BizMutation
    public List<ErpHrEmploymentContract> expireOverdueContracts(IServiceContext context) {
        return expireOverdueContractsProcessor.expireOverdueContracts(context);
    }

    @Override
    @BizMutation
    public ErpHrEmploymentContract renew(@Name("id") String id,
                                         @Name("newEndDate") LocalDate newEndDate,
                                         IServiceContext context) {
        ErpHrEmploymentContract contract = requireEntity(id, null, context);
        // 固定来源态/目标态判断委托 ErpHrEmploymentContractStateMachine（Bean 矩阵权威，契约 §4/§7）：
        // renew 接受 ACTIVE/EXPIRED 两类源（对齐原守卫）。非法边 Bean 抛 common 层码，此处映射领域
        // ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION（common 码作 cause）。
        try {
            stateMachine.assertCanRenew(contract.getStatus());
        } catch (NopException e) {
            throw new NopException(ErpHrErrors.ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION, e)
                    .param(ErpHrErrors.ARG_CONTRACT_ID, contract.getId())
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, contract.getStatus());
        }
        contract.setStatus(stateMachine.renewTargetStatus());
        contract.setEndDate(newEndDate);
        updateEntity(contract, null, context);
        return contract;
    }

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // socialInsuranceBase 授权 = 薪酬审批人；非授权 = null。委托 MaskHelper（fail-closed）。
    private static final Set<String> SALARY_MASK_ROLES = Set.of(MaskHelper.ROLE_SALARY_APPROVER);

    @BizLoader("socialInsuranceBase")
    public BigDecimal socialInsuranceBaseMask(@ContextSource ErpHrEmploymentContract entity) {
        return MaskHelper.maskDecimal(entity.getSocialInsuranceBase(), SALARY_MASK_ROLES, entity, "socialInsuranceBase");
    }

}
