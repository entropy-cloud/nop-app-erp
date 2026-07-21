
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.dto.DualSideDiffReport;
import app.erp.fin.dao.entity.ErpFinIntercompanyMatch;


public interface IErpFinIntercompanyMatchBiz extends ICrudBiz<ErpFinIntercompanyMatch>{

    /**
     * 期末公司间自动配对：按 pairKey 扫描本期跨公司 INTERCOMPANY_SALE/PURCHASE 凭证对 → 写配对记录（MATCHED/DIFF）。
     *
     * <p>配对键 = (pairKey, periodId)，pairKey=min/max(fromOrgId,toOrgId)+materialId（multi-company.md §Decision C）。
     *
     * @param periodId 会计期间
     * @param context  服务上下文
     * @return 本次配对写入的记录数
     */
    @BizMutation
    int runMatching(@Name("periodId") Long periodId, IServiceContext context);

    /**
     * 双向一致性校验：返回指定配对键 + 期间的双面差异报告（复用 DualSideDiffReport 结构范式）。
     *
     * @param pairKey  配对键
     * @param periodId 会计期间
     * @param context  服务上下文
     * @return 双面差异报告（finance 侧 vs 域级侧聚合）
     */
    @BizQuery
    DualSideDiffReport checkDualSideConsistency(@Name("pairKey") String pairKey,
                                                @Name("periodId") Long periodId,
                                                IServiceContext context);
}
