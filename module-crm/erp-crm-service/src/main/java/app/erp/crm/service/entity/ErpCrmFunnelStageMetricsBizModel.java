
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmFunnelStageMetricsBiz;
import app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics;

@BizModel("ErpCrmFunnelStageMetrics")
public class ErpCrmFunnelStageMetricsBizModel extends CrudBizModel<ErpCrmFunnelStageMetrics> implements IErpCrmFunnelStageMetricsBiz{
    public ErpCrmFunnelStageMetricsBizModel(){
        setEntityName(ErpCrmFunnelStageMetrics.class.getName());
    }
}
