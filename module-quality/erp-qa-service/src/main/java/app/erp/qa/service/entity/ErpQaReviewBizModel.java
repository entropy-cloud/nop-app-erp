
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaReviewBiz;
import app.erp.qa.dao.entity.ErpQaReview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpQaReview")
public class ErpQaReviewBizModel extends CrudBizModel<ErpQaReview> implements IErpQaReviewBiz{
    public ErpQaReviewBizModel(){
        setEntityName(ErpQaReview.class.getName());
    }

    @BizLoader(forType = ErpQaReview.class)
    public List<String> orgName(@ContextSource List<ErpQaReview> list) {
        orm().batchLoadProps(list, Collections.singleton("org"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaReview entity : list) {
            result.add(entity.getOrg() != null ? entity.getOrg().getName() : null);
        }
        return result;
    }
}
