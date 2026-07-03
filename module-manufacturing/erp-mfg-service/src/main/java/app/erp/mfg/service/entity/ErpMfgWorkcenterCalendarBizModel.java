
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkcenterCalendarBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCalendar;

@BizModel("ErpMfgWorkcenterCalendar")
public class ErpMfgWorkcenterCalendarBizModel extends CrudBizModel<ErpMfgWorkcenterCalendar> implements IErpMfgWorkcenterCalendarBiz{
    public ErpMfgWorkcenterCalendarBizModel(){
        setEntityName(ErpMfgWorkcenterCalendar.class.getName());
    }
}
