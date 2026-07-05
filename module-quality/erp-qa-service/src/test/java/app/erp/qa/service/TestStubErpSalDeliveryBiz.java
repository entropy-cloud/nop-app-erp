package app.erp.qa.service;

import app.erp.sal.biz.IErpSalDeliveryBiz;
import app.erp.sal.dao.entity.ErpSalDelivery;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.util.List;

/**
 * 测试桩：销售出库 BizModel。仅用于 quality-service 测试（避免引入 sales-service 形成 reactor 环）。
 *
 * <p>{@link #findFirst}/{@link #get} 直接经 {@code dao()} 反查（绕过 CrudBizModel 动作机制），
 * 召回目标定位/退货编排经此取得客户/仓库/币种/UoM。其余状态机方法为占位。
 */
@BizModel("ErpSalDelivery")
public class TestStubErpSalDeliveryBiz extends CrudBizModel<ErpSalDelivery> implements IErpSalDeliveryBiz {

    public TestStubErpSalDeliveryBiz() {
        setEntityName(ErpSalDelivery.class.getName());
    }

    @Override
    public ErpSalDelivery findFirst(QueryBean query, io.nop.api.core.beans.FieldSelectionBean selection, IServiceContext context) {
        List<ErpSalDelivery> list = dao().findAllByQuery(query);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public ErpSalDelivery get(String id, boolean ignoreUnknown, IServiceContext context) {
        return dao().getEntityById(Long.valueOf(id));
    }

    @Override
    public ErpSalDelivery submitForApproval(String id, IServiceContext context) {
        return get(id, false, context);
    }

    @Override
    public ErpSalDelivery withdrawApproval(String id, IServiceContext context) {
        return get(id, false, context);
    }

    @Override
    public ErpSalDelivery approve(String id, IServiceContext context) {
        return get(id, false, context);
    }

    @Override
    public ErpSalDelivery reject(String id, IServiceContext context) {
        return get(id, false, context);
    }

    @Override
    public ErpSalDelivery reverseApprove(String id, IServiceContext context) {
        return get(id, false, context);
    }

    @Override
    public ErpSalDelivery cancel(Long deliveryId, IServiceContext context) {
        return get(String.valueOf(deliveryId), false, context);
    }
}
