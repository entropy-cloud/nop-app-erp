
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurReturnBiz;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.processor.ErpPurReturnProcessor;
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
 * 采购退货单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由 xbiz 一行委托注入 Processor；非审批动作（cancel）在本类完成
 * Long→String 转换后委托 Processor。
 */
@BizModel("ErpPurReturn")
public class ErpPurReturnBizModel extends CrudBizModel<ErpPurReturn> implements IErpPurReturnBiz {

    @Inject
    ErpPurReturnProcessor returnProcessor;

    public ErpPurReturnBizModel() {
        setEntityName(ErpPurReturn.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurReturn cancel(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.cancel(String.valueOf(returnId), context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------
    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

    @BizLoader(forType = ErpPurReturn.class)
    public List<String> supplierName(@ContextSource List<ErpPurReturn> returns) {
        orm().batchLoadProps(returns, Collections.singleton("supplier"));
        List<String> result = new ArrayList<>(returns.size());
        for (ErpPurReturn ret : returns) {
            result.add(ret.getSupplier() != null ? ret.getSupplier().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurReturn.class)
    public List<String> warehouseName(@ContextSource List<ErpPurReturn> returns) {
        orm().batchLoadProps(returns, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(returns.size());
        for (ErpPurReturn ret : returns) {
            result.add(ret.getWarehouse() != null ? ret.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurReturn.class)
    public List<String> currencyName(@ContextSource List<ErpPurReturn> returns) {
        orm().batchLoadProps(returns, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(returns.size());
        for (ErpPurReturn ret : returns) {
            result.add(ret.getCurrency() != null ? ret.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurReturn.class)
    public List<String> orgName(@ContextSource List<ErpPurReturn> returns) {
        orm().batchLoadProps(returns, Collections.singleton("org"));
        List<String> result = new ArrayList<>(returns.size());
        for (ErpPurReturn ret : returns) {
            result.add(ret.getOrg() != null ? ret.getOrg().getName() : null);
        }
        return result;
    }
}
