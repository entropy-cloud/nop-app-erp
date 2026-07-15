
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgForecastLineBiz;
import app.erp.mfg.dao.entity.ErpMfgForecastLine;

import java.util.List;

@BizModel("ErpMfgForecastLine")
public class ErpMfgForecastLineBizModel extends CrudBizModel<ErpMfgForecastLine> implements IErpMfgForecastLineBiz{
    public ErpMfgForecastLineBizModel(){
        setEntityName(ErpMfgForecastLine.class.getName());
    }

}
