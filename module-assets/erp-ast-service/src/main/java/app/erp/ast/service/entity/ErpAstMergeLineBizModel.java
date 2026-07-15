
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstMergeLineBiz;
import app.erp.ast.dao.entity.ErpAstMergeLine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import java.util.List;

@BizModel("ErpAstMergeLine")
public class ErpAstMergeLineBizModel extends CrudBizModel<ErpAstMergeLine> implements IErpAstMergeLineBiz {
    public ErpAstMergeLineBizModel() {
        setEntityName(ErpAstMergeLine.class.getName());
    }

}
