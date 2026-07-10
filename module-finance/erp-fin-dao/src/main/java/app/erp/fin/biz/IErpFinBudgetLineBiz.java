
package app.erp.fin.biz;

import app.erp.fin.dao.dto.BudgetVsActualRow;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

/**
 * 预算明细行 Biz。CRUD 之外，承载预算对比查询 {@link #getBudgetVsActual}（budget.md §业务规则5）：
 * 按 {@code (subjectId, periodId, costCenterId, projectId)} 维度从 {@code ErpFinVoucherLine}
 * 关联凭证 {@code postingType} 聚合，得 Budget/Actual/Available 三列。
 */
public interface IErpFinBudgetLineBiz extends ICrudBiz<ErpFinBudgetLine> {

    /**
     * 预算对比查询：按科目×期间×成本中心×项目聚合预算 vs 实际 vs 余量。
     *
     * @param acctSchemaId 账套（可空）
     * @param periodId     会计期间（可空）
     * @param subjectId    科目（可空）
     * @param context      服务上下文
     * @return 对比行列表
     */
    @BizQuery
    List<BudgetVsActualRow> getBudgetVsActual(@Name("acctSchemaId") Long acctSchemaId,
                                              @Name("periodId") Long periodId,
                                              @Name("subjectId") Long subjectId,
                                              IServiceContext context);
}
