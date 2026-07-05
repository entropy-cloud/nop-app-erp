
package app.erp.qa.biz;

import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.qa.dao.entity.ErpQaReview;

public interface IErpQaReviewBiz extends ICrudBiz<ErpQaReview>, IApprovableBiz<ErpQaReview>{

}
