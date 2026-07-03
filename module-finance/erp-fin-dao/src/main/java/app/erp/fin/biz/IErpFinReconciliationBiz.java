
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinReconciliation;

import java.time.LocalDate;
import java.util.List;

public interface IErpFinReconciliationBiz extends ICrudBiz<ErpFinReconciliation> {

    /**
     * 创建草稿核销单（头 + 行）。维度（账套/组织/币种/汇率）从首条发票项继承；
     * 行仅记录引用与核销金额，实际回写辅助账在 {@link #post} 时发生。
     */
    @BizMutation
    ErpFinReconciliation create(@Name("direction") String direction,
                                @Name("partnerId") Long partnerId,
                                @Name("businessDate") LocalDate businessDate,
                                @Name("lines") List<ReconciliationLineInput> lines,
                                IServiceContext context);

    /**
     * 过账核销单：校验核销约束，回写双方辅助账 settled/open/status（OPEN→PARTIAL→SETTLED），
     * 置核销单 docStatus=POSTED，并重算往来单位余额。
     */
    @BizMutation
    ErpFinReconciliation post(@Name("reconciliationId") Long reconciliationId, IServiceContext context);

    /**
     * 红冲核销单：恢复双方辅助账 settled/open/status，置核销单 docStatus=REVERSED，重算往来单位余额。
     */
    @BizMutation
    ErpFinReconciliation reverse(@Name("reconciliationId") Long reconciliationId, IServiceContext context);
}
