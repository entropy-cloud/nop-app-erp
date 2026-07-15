
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetCapitalizationBiz;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.service.processor.ErpAstAssetCapitalizationProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 资产资本化（转固）BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpAstAssetCapitalizationProcessor} 全权处理。
 */
@BizModel("ErpAstAssetCapitalization")
public class ErpAstAssetCapitalizationBizModel extends CrudBizModel<ErpAstAssetCapitalization>
        implements IErpAstAssetCapitalizationBiz {

    @Inject
    ErpAstAssetCapitalizationProcessor capitalizationProcessor;

    public ErpAstAssetCapitalizationBizModel() {
        setEntityName(ErpAstAssetCapitalization.class.getName());
    }

}
