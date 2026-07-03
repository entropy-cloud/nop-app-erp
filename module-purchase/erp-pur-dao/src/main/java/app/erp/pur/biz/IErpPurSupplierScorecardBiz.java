
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurSupplierScorecard;

/**
 * 供应商评分卡周期评分引擎契约（{@code docs/design/purchase/supplier-evaluation.md §业务规则2/3}）。
 *
 * <p>{@link #finalizeScorecard} 按 criteria 取 variable.path 取值 → 公式（XLang 表达式）计算 score
 * → weightedScore=score×weight/100 → totalScore=Σ → 按 warn/hold 阈值落 standing；status DRAFT→FINALIZED。
 * 公式引擎为平台 XLang 表达式（非硬编码 Java），新增维度=配置 criteria+variable 不改代码。
 */
public interface IErpPurSupplierScorecardBiz extends ICrudBiz<ErpPurSupplierScorecard> {

    @BizMutation
    ErpPurSupplierScorecard finalizeScorecard(@Name("scorecardId") Long scorecardId, IServiceContext context);
}
