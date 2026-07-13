
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrCompetencyLevelBiz;
import app.erp.hr.dao.entity.ErpHrCompetencyLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrCompetencyLevel")
public class ErpHrCompetencyLevelBizModel extends CrudBizModel<ErpHrCompetencyLevel> implements IErpHrCompetencyLevelBiz{
    public ErpHrCompetencyLevelBizModel(){
        setEntityName(ErpHrCompetencyLevel.class.getName());
    }

    @BizLoader(forType = ErpHrCompetencyLevel.class)
    public List<String> competencyName(@ContextSource List<ErpHrCompetencyLevel> rows) {
        orm().batchLoadProps(rows, Collections.singleton("competency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrCompetencyLevel row : rows) {
            result.add(row.orm_attached() && row.getCompetency() != null ? row.getCompetency().getName() : null);
        }
        return result;
    }
}
