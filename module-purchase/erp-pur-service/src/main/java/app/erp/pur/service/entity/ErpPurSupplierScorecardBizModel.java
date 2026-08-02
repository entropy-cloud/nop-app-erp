
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurSupplierScorecardBiz;
import app.erp.pur.dao.entity.ErpPurSupplierScorecard;
import app.erp.pur.service.processor.ErpPurSupplierScorecardFinalizeScorecardProcessor;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 供应商评分卡 BizModel（Facade）。{@code finalizeScorecard} 委托
 * {@link ErpPurSupplierScorecardFinalizeScorecardProcessor} 完成「criteria×formula×weight→totalScore→standing」
 * + status DRAFT→FINALIZED + standing=RED 跨域 AVL 联动（{@code docs/design/purchase/supplier-evaluation.md §业务规则2/3/4}）。
 */
@BizModel("ErpPurSupplierScorecard")
public class ErpPurSupplierScorecardBizModel extends CrudBizModel<ErpPurSupplierScorecard> implements IErpPurSupplierScorecardBiz {

    @Inject
    ErpPurSupplierScorecardFinalizeScorecardProcessor finalizeScorecardProcessor;

    public ErpPurSupplierScorecardBizModel() {
        setEntityName(ErpPurSupplierScorecard.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurSupplierScorecard finalizeScorecard(@Name("scorecardId") Long scorecardId, IServiceContext context) {
        return finalizeScorecardProcessor.finalizeScorecard(scorecardId, context);
    }

    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

}
