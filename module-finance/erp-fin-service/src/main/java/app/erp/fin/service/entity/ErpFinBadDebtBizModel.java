
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBadDebtBiz;
import app.erp.fin.dao.dto.BadDebtProvisionResult;
import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.service.baddebt.BadDebtProvisionService;
import app.erp.fin.service.processor.ErpFinBadDebtProcessor;
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
 * 坏账单聚合根 Biz（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 三轴审批状态机 + ArApItem 变异 + 坏账凭证生成委托 {@link ErpFinBadDebtProcessor}；
 * 期末计提/释放委托 {@link BadDebtProvisionService}。
 *
 * <p>语义与配置门控见 {@code bad-debt.md}；{@code @BizMutation} 钉事务/会话边界。
 */
@BizModel("ErpFinBadDebt")
public class ErpFinBadDebtBizModel extends CrudBizModel<ErpFinBadDebt> implements IErpFinBadDebtBiz {

    @Inject
    ErpFinBadDebtProcessor badDebtProcessor;
    @Inject
    BadDebtProvisionService badDebtProvisionService;

    public ErpFinBadDebtBizModel() {
        setEntityName(ErpFinBadDebt.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinBadDebt writeOff(@Name("arApItemId") Long arApItemId,
                                  @Name("reason") String reason,
                                  IServiceContext context) {
        return badDebtProcessor.writeOff(arApItemId, reason, context);
    }

    @Override
    @BizMutation
    public ErpFinBadDebt recover(@Name("arApItemId") Long arApItemId,
                                 @Name("reason") String reason,
                                 IServiceContext context) {
        return badDebtProcessor.recover(arApItemId, reason, context);
    }

    @Override
    @BizMutation
    public ErpFinBadDebt submit(@Name("id") Long id, IServiceContext context) {
        return badDebtProcessor.submit(id, context);
    }

    @Override
    @BizMutation
    public ErpFinBadDebt approve(@Name("id") Long id, IServiceContext context) {
        return badDebtProcessor.approve(id, context);
    }

    @Override
    @BizMutation
    public ErpFinBadDebt reject(@Name("id") Long id, IServiceContext context) {
        return badDebtProcessor.reject(id, context);
    }

    @Override
    @BizMutation
    public BadDebtProvisionResult runBadDebtProvision(@Name("periodId") Long periodId, IServiceContext context) {
        return badDebtProvisionService.runBadDebtProvision(periodId, context);
    }

    // ---------- 高价值外键名称解析（机制 D）----------
    // sourceArApItemId 为内部溯源链路，保留原始 ID。

    @BizLoader(forType = ErpFinBadDebt.class)
    public List<String> orgName(@ContextSource List<ErpFinBadDebt> debts) {
        orm().batchLoadProps(debts, Collections.singleton("org"));
        List<String> result = new ArrayList<>(debts.size());
        for (ErpFinBadDebt debt : debts) {
            result.add(debt.getOrg() != null ? debt.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBadDebt.class)
    public List<String> acctSchemaCode(@ContextSource List<ErpFinBadDebt> debts) {
        orm().batchLoadProps(debts, Collections.singleton("acctSchema"));
        List<String> result = new ArrayList<>(debts.size());
        for (ErpFinBadDebt debt : debts) {
            result.add(debt.getAcctSchema() != null ? debt.getAcctSchema().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBadDebt.class)
    public List<String> partnerName(@ContextSource List<ErpFinBadDebt> debts) {
        orm().batchLoadProps(debts, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(debts.size());
        for (ErpFinBadDebt debt : debts) {
            result.add(debt.getPartner() != null ? debt.getPartner().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBadDebt.class)
    public List<String> currencyName(@ContextSource List<ErpFinBadDebt> debts) {
        orm().batchLoadProps(debts, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(debts.size());
        for (ErpFinBadDebt debt : debts) {
            result.add(debt.getCurrency() != null ? debt.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBadDebt.class)
    public List<String> periodCode(@ContextSource List<ErpFinBadDebt> debts) {
        orm().batchLoadProps(debts, Collections.singleton("period"));
        List<String> result = new ArrayList<>(debts.size());
        for (ErpFinBadDebt debt : debts) {
            result.add(debt.getPeriod() != null ? debt.getPeriod().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBadDebt.class)
    public List<String> voucherCode(@ContextSource List<ErpFinBadDebt> debts) {
        orm().batchLoadProps(debts, Collections.singleton("voucher"));
        List<String> result = new ArrayList<>(debts.size());
        for (ErpFinBadDebt debt : debts) {
            result.add(debt.getVoucher() != null ? debt.getVoucher().getCode() : null);
        }
        return result;
    }
}
