
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgRoutingOperationBiz;
import app.erp.mfg.dao.entity.ErpMfgRoutingOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpMfgRoutingOperation")
public class ErpMfgRoutingOperationBizModel extends CrudBizModel<ErpMfgRoutingOperation> implements IErpMfgRoutingOperationBiz{
    public ErpMfgRoutingOperationBizModel(){
        setEntityName(ErpMfgRoutingOperation.class.getName());
    }

    @BizLoader(forType = ErpMfgRoutingOperation.class)
    public List<String> routingCode(@ContextSource List<ErpMfgRoutingOperation> ops) {
        orm().batchLoadProps(ops, Collections.singleton("routing"));
        List<String> result = new ArrayList<>(ops.size());
        for (ErpMfgRoutingOperation op : ops) {
            result.add(op.getRouting() != null ? op.getRouting().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgRoutingOperation.class)
    public List<String> workcenterName(@ContextSource List<ErpMfgRoutingOperation> ops) {
        orm().batchLoadProps(ops, Collections.singleton("workcenter"));
        List<String> result = new ArrayList<>(ops.size());
        for (ErpMfgRoutingOperation op : ops) {
            result.add(op.getWorkcenter() != null ? op.getWorkcenter().getName() : null);
        }
        return result;
    }
}
