
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrGapAnalysisBiz;
import app.erp.hr.dao.entity.ErpHrGapAnalysis;

@BizModel("ErpHrGapAnalysis")
public class ErpHrGapAnalysisBizModel extends CrudBizModel<ErpHrGapAnalysis> implements IErpHrGapAnalysisBiz{
    public ErpHrGapAnalysisBizModel(){
        setEntityName(ErpHrGapAnalysis.class.getName());
    }
}
