
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrAssessmentDetailBiz;
import app.erp.hr.dao.entity.ErpHrAssessmentDetail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrAssessmentDetail")
public class ErpHrAssessmentDetailBizModel extends CrudBizModel<ErpHrAssessmentDetail> implements IErpHrAssessmentDetailBiz{
    public ErpHrAssessmentDetailBizModel(){
        setEntityName(ErpHrAssessmentDetail.class.getName());
    }

    @BizLoader(forType = ErpHrAssessmentDetail.class)
    public List<String> competencyName(@ContextSource List<ErpHrAssessmentDetail> rows) {
        orm().batchLoadProps(rows, Collections.singleton("competency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrAssessmentDetail row : rows) {
            result.add(row.orm_attached() && row.getCompetency() != null ? row.getCompetency().getName() : null);
        }
        return result;
    }
}
