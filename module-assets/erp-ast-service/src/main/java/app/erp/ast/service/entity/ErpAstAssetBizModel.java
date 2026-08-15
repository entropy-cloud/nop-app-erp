
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.service.processor.ErpAstAssetSuspendResumeProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 资产卡片 BizModel（Facade）。CRUD 走 CrudBizModel 默认；RC-R1.54 增 suspend/resume
 * 闲置状态机 mutation（L1 UC-AST-03），编排委托 {@link ErpAstAssetSuspendResumeProcessor}
 * （R6.3 per-mutation Processor，protected step 方法供下游覆盖）。
 */
@BizModel("ErpAstAsset")
public class ErpAstAssetBizModel extends CrudBizModel<ErpAstAsset> implements IErpAstAssetBiz {

    @Inject
    ErpAstAssetSuspendResumeProcessor suspendResumeProcessor;

    public ErpAstAssetBizModel() {
        setEntityName(ErpAstAsset.class.getName());
    }

    @Override
    @BizMutation
    public ErpAstAsset suspend(@Name("assetId") Long assetId, IServiceContext context) {
        return suspendResumeProcessor.suspend(assetId, context);
    }

    @Override
    @BizMutation
    public ErpAstAsset resume(@Name("assetId") Long assetId, IServiceContext context) {
        return suspendResumeProcessor.resume(assetId, context);
    }

}
