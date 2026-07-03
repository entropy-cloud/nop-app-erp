
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetCapitalizationBiz;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.service.processor.ErpAstAssetCapitalizationProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 资产资本化（转固）BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 三轴审批状态机 + 建卡/折旧计划生成 + CAPITALIZATION 业财过账编排委托
 * {@link ErpAstAssetCapitalizationProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义见 {@code depreciation-and-posting.md} §2；{@code @BizMutation}+{@code @SingleSession} 钉事务/会话边界。
 */
@BizModel("ErpAstAssetCapitalization")
public class ErpAstAssetCapitalizationBizModel extends CrudBizModel<ErpAstAssetCapitalization>
        implements IErpAstAssetCapitalizationBiz {

    @Inject
    ErpAstAssetCapitalizationProcessor capitalizationProcessor;

    public ErpAstAssetCapitalizationBizModel() {
        setEntityName(ErpAstAssetCapitalization.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstAssetCapitalization submit(@Name("id") Long id, IServiceContext context) {
        return capitalizationProcessor.submit(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstAssetCapitalization approve(@Name("id") Long id, IServiceContext context) {
        return capitalizationProcessor.approve(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstAssetCapitalization reject(@Name("id") Long id, IServiceContext context) {
        return capitalizationProcessor.reject(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstAssetCapitalization reverseApprove(@Name("id") Long id, IServiceContext context) {
        return capitalizationProcessor.reverseApprove(id, context);
    }
}
