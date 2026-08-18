
package app.erp.cs.biz;

import io.nop.orm.biz.ICrudBiz;

import app.erp.cs.dao.entity.ErpCsTimeEntry;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;

public interface IErpCsTimeEntryBiz extends ICrudBiz<ErpCsTimeEntry> {

    /**
     * 提交条目进入审批（RC-R1.66，UC-CS-11 ⑤⑥；plan D4）：DRAFT(NULL)/REJECTED →
     * require-description 门控 → isBillable 或 duration≥阈值 → PENDING；否则直通 APPROVED。
     */
    @BizMutation
    ErpCsTimeEntry submit(@Name("timeEntryId") Long timeEntryId,
                          IServiceContext context);

    /** 审批通过（UC-CS-11 ⑦ 前置）：PENDING → APPROVED。 */
    @BizMutation
    ErpCsTimeEntry approve(@Name("timeEntryId") Long timeEntryId,
                           IServiceContext context);

    /** 审批驳回（plan D4 §3.2）：PENDING → REJECTED，可选原因追加 description 前缀；可修改后重新 submit。 */
    @BizMutation
    ErpCsTimeEntry reject(@Name("timeEntryId") Long timeEntryId,
                          @Optional @Name("rejectReason") String rejectReason,
                          IServiceContext context);
}
