
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.hr.biz.IErpHrEmploymentContractBiz;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        LocalDate now = CoreMetrics.today();
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpHrConstants.CONTRACT_STATUS_ACTIVE));
        q.addFilter(lt("endDate", now));
        List<ErpHrEmploymentContract> overdue = doFindListByQueryDirectly(q, context);
        List<ErpHrEmploymentContract> expired = new ArrayList<>();
        for (ErpHrEmploymentContract c : overdue) {
            try {
                c.setStatus(ErpHrConstants.CONTRACT_STATUS_EXPIRED);
                updateEntity(c, null, context);
                expired.add(c);
            } catch (Exception ex) {
                LOG.warn("contract-expire-failed: id={}, reason={}", c.getId(), ex.getMessage());
            }
        }
        return expired;
    }

    @Override
    @BizMutation
    public ErpHrEmploymentContract renew(@Name("id") String id,
                                         @Name("newEndDate") LocalDate newEndDate,
                                         IServiceContext context) {
        ErpHrEmploymentContract contract = requireEntity(id, null, context);
        String status = contract.getStatus();
        if (!ErpHrConstants.CONTRACT_STATUS_ACTIVE.equals(status)
                && !ErpHrConstants.CONTRACT_STATUS_EXPIRED.equals(status)) {
            throw new NopException(ErpHrErrors.ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpHrErrors.ARG_CONTRACT_ID, contract.getId())
                    .param(ErpHrErrors.ARG_CURRENT_STATUS, status);
        }
        contract.setStatus(ErpHrConstants.CONTRACT_STATUS_ACTIVE);
        contract.setEndDate(newEndDate);
        updateEntity(contract, null, context);
        return contract;
    }

}
