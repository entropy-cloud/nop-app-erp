
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinExpenseClaimBiz;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.service.processor.ErpFinExpenseClaimProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 费用报销单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpFinExpenseClaimProcessor} 全权处理。
 */
@BizModel("ErpFinExpenseClaim")
public class ErpFinExpenseClaimBizModel extends CrudBizModel<ErpFinExpenseClaim> implements IErpFinExpenseClaimBiz {

    @Inject
    ErpFinExpenseClaimProcessor claimProcessor;

    public ErpFinExpenseClaimBizModel() {
        setEntityName(ErpFinExpenseClaim.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim cancel(@Name("claimId") Long claimId, IServiceContext context) {
        return claimProcessor.cancel(claimId, context);
    }

    // ---------- 高价值外键名称解析（机制 D）----------

    @BizLoader(forType = ErpFinExpenseClaim.class)
    public List<String> orgName(@ContextSource List<ErpFinExpenseClaim> claims) {
        orm().batchLoadProps(claims, Collections.singleton("org"));
        List<String> result = new ArrayList<>(claims.size());
        for (ErpFinExpenseClaim claim : claims) {
            result.add(claim.getOrg() != null ? claim.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinExpenseClaim.class)
    public List<String> claimantName(@ContextSource List<ErpFinExpenseClaim> claims) {
        orm().batchLoadProps(claims, Collections.singleton("claimant"));
        List<String> result = new ArrayList<>(claims.size());
        for (ErpFinExpenseClaim claim : claims) {
            result.add(claim.getClaimant() != null ? claim.getClaimant().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinExpenseClaim.class)
    public List<String> departmentName(@ContextSource List<ErpFinExpenseClaim> claims) {
        orm().batchLoadProps(claims, Collections.singleton("department"));
        List<String> result = new ArrayList<>(claims.size());
        for (ErpFinExpenseClaim claim : claims) {
            result.add(claim.getDepartment() != null ? claim.getDepartment().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinExpenseClaim.class)
    public List<String> currencyName(@ContextSource List<ErpFinExpenseClaim> claims) {
        orm().batchLoadProps(claims, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(claims.size());
        for (ErpFinExpenseClaim claim : claims) {
            result.add(claim.getCurrency() != null ? claim.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinExpenseClaim.class)
    public List<String> employeeAdvanceCode(@ContextSource List<ErpFinExpenseClaim> claims) {
        orm().batchLoadProps(claims, Collections.singleton("settleAdvance"));
        List<String> result = new ArrayList<>(claims.size());
        for (ErpFinExpenseClaim claim : claims) {
            result.add(claim.getSettleAdvance() != null ? claim.getSettleAdvance().getCode() : null);
        }
        return result;
    }
}
