
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstDisposalBiz;
import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.processor.ErpAstDisposalProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 资产处置 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpAstDisposalProcessor} 全权处理。
 */
@BizModel("ErpAstDisposal")
public class ErpAstDisposalBizModel extends CrudBizModel<ErpAstDisposal> implements IErpAstDisposalBiz {

    @Inject
    ErpAstDisposalProcessor disposalProcessor;

    public ErpAstDisposalBizModel() {
        setEntityName(ErpAstDisposal.class.getName());
    }

}
