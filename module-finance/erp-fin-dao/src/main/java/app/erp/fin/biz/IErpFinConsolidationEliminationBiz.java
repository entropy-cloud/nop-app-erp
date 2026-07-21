
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.entity.ErpFinConsolidationElimination;


public interface IErpFinConsolidationEliminationBiz extends ICrudBiz<ErpFinConsolidationElimination>{

    /**
     * 期末抵消候选识别：按抵消 3 类（AR_AP/REVENUE_COST/INVENTORY_PROFIT）扫描配对候选 → 写候选记录（CANDIDATE）。
     *
     * <p>config-gated {@code erp-fin.consolidation-elimination-enabled} 默认 false（multi-company.md §Decision D）。
     *
     * @param periodId 会计期间
     * @param context  服务上下文
     * @return 本次识别写入的候选数
     */
    @BizMutation
    int generateEliminationCandidates(@Name("periodId") Long periodId, IServiceContext context);

    /**
     * 生成抵消分录草稿凭证：将候选（CANDIDATE）转为 DRAFT_VOUCHER（抵消分录凭证 docStatus=DRAFT）。
     *
     * @param candidateId 抵消候选 ID
     * @param context     服务上下文
     * @return 草稿抵消凭证 ID
     */
    @BizMutation
    Long postElimination(@Name("candidateId") Long candidateId, IServiceContext context);
}
