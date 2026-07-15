
package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaRecallTargetBiz;
import app.erp.qa.dao.entity.ErpQaRecallTarget;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import java.util.List;

/**
 * 召回目标 BizModel。仅承载标准 CRUD；目标的创建/通知/退货状态由
 * {@link ErpQaRecallBizModel} 的状态机编排驱动。
 */
@BizModel("ErpQaRecallTarget")
public class ErpQaRecallTargetBizModel extends CrudBizModel<ErpQaRecallTarget> implements IErpQaRecallTargetBiz {

    public ErpQaRecallTargetBizModel() {
        setEntityName(ErpQaRecallTarget.class.getName());
    }

}
