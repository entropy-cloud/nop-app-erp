
package app.erp.pur.service.entity;

import app.erp.pur.biz.ConvertToOrderRequest;
import app.erp.pur.biz.IErpPurRequisitionBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.service.processor.ErpPurRequisitionProcessor;
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
 * 采购请购单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由 xbiz 一行委托注入 Processor；非审批动作（cancel/convertToOrder）在本类完成
 * Long→String 转换后委托 Processor。
 */
@BizModel("ErpPurRequisition")
public class ErpPurRequisitionBizModel extends CrudBizModel<ErpPurRequisition> implements IErpPurRequisitionBiz {

    @Inject
    ErpPurRequisitionProcessor requisitionProcessor;

    public ErpPurRequisitionBizModel() {
        setEntityName(ErpPurRequisition.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurRequisition cancel(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        return requisitionProcessor.cancel(String.valueOf(requisitionId), context);
    }

    @Override
    @BizMutation
    public ErpPurOrder convertToOrder(@Name("requisitionId") Long requisitionId,
                                      @Name("request") ConvertToOrderRequest request, IServiceContext context) {
        return requisitionProcessor.convertToOrder(String.valueOf(requisitionId), request, context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------
    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

    @BizLoader(forType = ErpPurRequisition.class)
    public List<String> orgName(@ContextSource List<ErpPurRequisition> requisitions) {
        orm().batchLoadProps(requisitions, Collections.singleton("org"));
        List<String> result = new ArrayList<>(requisitions.size());
        for (ErpPurRequisition requisition : requisitions) {
            result.add(requisition.getOrg() != null ? requisition.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurRequisition.class)
    public List<String> requesterName(@ContextSource List<ErpPurRequisition> requisitions) {
        orm().batchLoadProps(requisitions, Collections.singleton("requester"));
        List<String> result = new ArrayList<>(requisitions.size());
        for (ErpPurRequisition requisition : requisitions) {
            result.add(requisition.getRequester() != null ? requisition.getRequester().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurRequisition.class)
    public List<String> departmentName(@ContextSource List<ErpPurRequisition> requisitions) {
        orm().batchLoadProps(requisitions, Collections.singleton("department"));
        List<String> result = new ArrayList<>(requisitions.size());
        for (ErpPurRequisition requisition : requisitions) {
            result.add(requisition.getDepartment() != null ? requisition.getDepartment().getName() : null);
        }
        return result;
    }
}
