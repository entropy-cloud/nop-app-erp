
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurReceiveBiz;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.processor.ErpPurReceiveProcessor;
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
 * 采购入库单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由 xbiz 一行委托注入 Processor；非审批动作（cancel）在本类完成
 * Long→String 转换后委托 Processor。
 */
@BizModel("ErpPurReceive")
public class ErpPurReceiveBizModel extends CrudBizModel<ErpPurReceive> implements IErpPurReceiveBiz {

    @Inject
    ErpPurReceiveProcessor receiveProcessor;

    public ErpPurReceiveBizModel() {
        setEntityName(ErpPurReceive.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurReceive cancel(@Name("receiveId") Long receiveId, IServiceContext context) {
        return receiveProcessor.cancel(String.valueOf(receiveId), context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------
    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

    @BizLoader(forType = ErpPurReceive.class)
    public List<String> supplierName(@ContextSource List<ErpPurReceive> receives) {
        orm().batchLoadProps(receives, Collections.singleton("supplier"));
        List<String> result = new ArrayList<>(receives.size());
        for (ErpPurReceive receive : receives) {
            result.add(receive.getSupplier() != null ? receive.getSupplier().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurReceive.class)
    public List<String> warehouseName(@ContextSource List<ErpPurReceive> receives) {
        orm().batchLoadProps(receives, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(receives.size());
        for (ErpPurReceive receive : receives) {
            result.add(receive.getWarehouse() != null ? receive.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurReceive.class)
    public List<String> currencyName(@ContextSource List<ErpPurReceive> receives) {
        orm().batchLoadProps(receives, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(receives.size());
        for (ErpPurReceive receive : receives) {
            result.add(receive.getCurrency() != null ? receive.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurReceive.class)
    public List<String> orgName(@ContextSource List<ErpPurReceive> receives) {
        orm().batchLoadProps(receives, Collections.singleton("org"));
        List<String> result = new ArrayList<>(receives.size());
        for (ErpPurReceive receive : receives) {
            result.add(receive.getOrg() != null ? receive.getOrg().getName() : null);
        }
        return result;
    }
}
