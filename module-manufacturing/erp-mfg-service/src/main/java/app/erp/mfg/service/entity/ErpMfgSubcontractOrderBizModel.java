
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgSubcontractOrderBiz;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.processor.ErpMfgSubcontractOrderProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 委外加工单 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）由平台
 * {@code approval-support.xbiz} 标准 source 提供并委托 {@link ErpMfgSubcontractOrderProcessor}。
 * 委外生命周期三段（issueMaterials/receiveFinished/postProcessingFee）+ cancel 委托 Processor。
 *
 * <p>语义见 {@code docs/design/manufacturing/subcontracting.md}。
 */
@BizModel("ErpMfgSubcontractOrder")
public class ErpMfgSubcontractOrderBizModel extends CrudBizModel<ErpMfgSubcontractOrder> implements IErpMfgSubcontractOrderBiz {

    @Inject
    ErpMfgSubcontractOrderProcessor subcontractOrderProcessor;

    public ErpMfgSubcontractOrderBizModel() {
        setEntityName(ErpMfgSubcontractOrder.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder cancel(@Name("subcontractOrderId") Long subcontractOrderId, IServiceContext context) {
        return subcontractOrderProcessor.cancel(subcontractOrderId, context);
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder issueMaterials(@Name("subcontractOrderId") Long subcontractOrderId,
                                                  @io.nop.api.core.annotations.core.Optional @Name("sourceWarehouseId") Long sourceWarehouseId,
                                                  IServiceContext context) {
        return subcontractOrderProcessor.issueMaterials(subcontractOrderId, sourceWarehouseId, context);
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder receiveFinished(@Name("subcontractOrderId") Long subcontractOrderId,
                                                   @Name("receivedQty") BigDecimal receivedQty,
                                                   @io.nop.api.core.annotations.core.Optional @Name("destWarehouseId") Long destWarehouseId,
                                                   IServiceContext context) {
        return subcontractOrderProcessor.receiveFinished(subcontractOrderId, receivedQty, destWarehouseId, context);
    }

    @Override
    @BizMutation
    public ErpMfgSubcontractOrder postProcessingFee(@Name("subcontractOrderId") Long subcontractOrderId, IServiceContext context) {
        return subcontractOrderProcessor.postProcessingFee(subcontractOrderId, context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------

    @BizLoader(forType = ErpMfgSubcontractOrder.class)
    public List<String> orgName(@ContextSource List<ErpMfgSubcontractOrder> orders) {
        orm().batchLoadProps(orders, Collections.singleton("org"));
        List<String> result = new ArrayList<>(orders.size());
        for (ErpMfgSubcontractOrder order : orders) {
            result.add(order.getOrg() != null ? order.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgSubcontractOrder.class)
    public List<String> workOrderNo(@ContextSource List<ErpMfgSubcontractOrder> orders) {
        orm().batchLoadProps(orders, Collections.singleton("workOrder"));
        List<String> result = new ArrayList<>(orders.size());
        for (ErpMfgSubcontractOrder order : orders) {
            result.add(order.getWorkOrder() != null ? order.getWorkOrder().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgSubcontractOrder.class)
    public List<String> supplierName(@ContextSource List<ErpMfgSubcontractOrder> orders) {
        orm().batchLoadProps(orders, Collections.singleton("supplier"));
        List<String> result = new ArrayList<>(orders.size());
        for (ErpMfgSubcontractOrder order : orders) {
            result.add(order.getSupplier() != null ? order.getSupplier().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgSubcontractOrder.class)
    public List<String> workcenterName(@ContextSource List<ErpMfgSubcontractOrder> orders) {
        orm().batchLoadProps(orders, Collections.singleton("workcenter"));
        List<String> result = new ArrayList<>(orders.size());
        for (ErpMfgSubcontractOrder order : orders) {
            result.add(order.getWorkcenter() != null ? order.getWorkcenter().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgSubcontractOrder.class)
    public List<String> productName(@ContextSource List<ErpMfgSubcontractOrder> orders) {
        orm().batchLoadProps(orders, Collections.singleton("product"));
        List<String> result = new ArrayList<>(orders.size());
        for (ErpMfgSubcontractOrder order : orders) {
            result.add(order.getProduct() != null ? order.getProduct().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgSubcontractOrder.class)
    public List<String> currencyName(@ContextSource List<ErpMfgSubcontractOrder> orders) {
        orm().batchLoadProps(orders, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(orders.size());
        for (ErpMfgSubcontractOrder order : orders) {
            result.add(order.getCurrency() != null ? order.getCurrency().getName() : null);
        }
        return result;
    }
}
