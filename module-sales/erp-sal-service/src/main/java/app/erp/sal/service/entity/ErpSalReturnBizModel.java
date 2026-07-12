
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalReturnBiz;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.processor.ErpSalReturnProcessor;
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
 * 销售退货单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@link ErpSalReturnProcessor#onSubmit}/{@link ErpSalReturnProcessor#onApproved}/{@link ErpSalReturnProcessor#onReverseApproved}。
 */
@BizModel("ErpSalReturn")
public class ErpSalReturnBizModel extends CrudBizModel<ErpSalReturn> implements IErpSalReturnBiz {

    @Inject
    ErpSalReturnProcessor returnProcessor;

    public ErpSalReturnBizModel() {
        setEntityName(ErpSalReturn.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalReturn cancel(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.cancel(String.valueOf(returnId), context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------
    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

    @BizLoader(forType = ErpSalReturn.class)
    public List<String> customerName(@ContextSource List<ErpSalReturn> returns) {
        orm().batchLoadProps(returns, Collections.singleton("customer"));
        List<String> result = new ArrayList<>(returns.size());
        for (ErpSalReturn ret : returns) {
            result.add(ret.getCustomer() != null ? ret.getCustomer().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpSalReturn.class)
    public List<String> warehouseName(@ContextSource List<ErpSalReturn> returns) {
        orm().batchLoadProps(returns, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(returns.size());
        for (ErpSalReturn ret : returns) {
            result.add(ret.getWarehouse() != null ? ret.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpSalReturn.class)
    public List<String> currencyName(@ContextSource List<ErpSalReturn> returns) {
        orm().batchLoadProps(returns, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(returns.size());
        for (ErpSalReturn ret : returns) {
            result.add(ret.getCurrency() != null ? ret.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpSalReturn.class)
    public List<String> orgName(@ContextSource List<ErpSalReturn> returns) {
        orm().batchLoadProps(returns, Collections.singleton("org"));
        List<String> result = new ArrayList<>(returns.size());
        for (ErpSalReturn ret : returns) {
            result.add(ret.getOrg() != null ? ret.getOrg().getName() : null);
        }
        return result;
    }
}
