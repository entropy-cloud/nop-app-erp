
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinEmployeeAdvanceBiz;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.processor.ErpFinEmployeeAdvanceProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 员工借款单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpFinEmployeeAdvanceProcessor} 全权处理。
 */
@BizModel("ErpFinEmployeeAdvance")
public class ErpFinEmployeeAdvanceBizModel extends CrudBizModel<ErpFinEmployeeAdvance> implements IErpFinEmployeeAdvanceBiz {

    @Inject
    ErpFinEmployeeAdvanceProcessor advanceProcessor;

    public ErpFinEmployeeAdvanceBizModel() {
        setEntityName(ErpFinEmployeeAdvance.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinEmployeeAdvance cancel(@Name("advanceId") Long advanceId, IServiceContext context) {
        return advanceProcessor.cancel(advanceId, context);
    }

    // ---------- 高价值外键名称解析（机制 D）----------

    @BizLoader(forType = ErpFinEmployeeAdvance.class)
    public List<String> orgName(@ContextSource List<ErpFinEmployeeAdvance> advances) {
        orm().batchLoadProps(advances, Collections.singleton("org"));
        List<String> result = new ArrayList<>(advances.size());
        for (ErpFinEmployeeAdvance advance : advances) {
            result.add(advance.getOrg() != null ? advance.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinEmployeeAdvance.class)
    public List<String> employeeName(@ContextSource List<ErpFinEmployeeAdvance> advances) {
        orm().batchLoadProps(advances, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(advances.size());
        for (ErpFinEmployeeAdvance advance : advances) {
            result.add(advance.getEmployee() != null ? advance.getEmployee().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinEmployeeAdvance.class)
    public List<String> currencyName(@ContextSource List<ErpFinEmployeeAdvance> advances) {
        orm().batchLoadProps(advances, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(advances.size());
        for (ErpFinEmployeeAdvance advance : advances) {
            result.add(advance.getCurrency() != null ? advance.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinEmployeeAdvance.class)
    public List<String> projectName(@ContextSource List<ErpFinEmployeeAdvance> advances) {
        orm().batchLoadProps(advances, Collections.singleton("project"));
        List<String> result = new ArrayList<>(advances.size());
        for (ErpFinEmployeeAdvance advance : advances) {
            result.add(advance.getProject() != null ? advance.getProject().getName() : null);
        }
        return result;
    }
}
