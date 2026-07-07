
package app.erp.cs.biz;

import app.erp.cs.dao.entity.ErpCsCatalogFulfillment;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

public interface IErpCsCatalogFulfillmentBiz extends ICrudBiz<ErpCsCatalogFulfillment>{

    /**
     * 执行目录项的履行流程映射（service-catalog.md §三）。
     *
     * <p>本期范围：
     * <ul>
     *   <li>CREATE_TICKET —— 登记为 DONE（工单已由调用方创建，写入参 ticketId）。</li>
     *   <li>ASSIGN_TEAM / NOTIFY_CUSTOMER / UPDATE_STATUS —— 登记执行结果（写 TicketAction 审计）。</li>
     *   <li>INVOKE_WORKFLOW / CREATE_CHILD_TICKET —— 标记 SKIPPED（归 Non-Goal successor）。</li>
     * </ul>
     * 完整多步履行编排（技能匹配分派 / 跨域调用）归 successor。
     *
     * @param catalogItemId 目录项 ID
     * @param ticketId      已创建的工单 ID（CREATE_TICKET 步骤的产物）
     * @return 已处理的履行步骤列表
     */
    @BizMutation
    List<ErpCsCatalogFulfillment> executeFulfillmentSteps(@Name("catalogItemId") Long catalogItemId,
                                                          @Name("ticketId") Long ticketId,
                                                          IServiceContext context);
}
