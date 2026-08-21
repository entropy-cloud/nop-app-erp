
package app.erp.cs.biz;

import app.erp.cs.dao.entity.ErpCsCatalogFulfillment;
import app.erp.cs.dao.entity.ErpCsTicketFulfillmentStep;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;
import java.util.Map;

public interface IErpCsCatalogFulfillmentBiz extends ICrudBiz<ErpCsCatalogFulfillment>{

    /**
     * 执行目录项的履行流程（RC-R1.71 实化，service-catalog.md §9.1）。
     *
     * <p>按模板物化 per-ticket 执行行（UK(ticketId, fulfillmentId) 幂等）→ sequence 升序推进：
     * 五动作 actionConfig 驱动实化（ASSIGN_TEAM/ASSIGN_AGENT/REQUEST_APPROVAL/CREATE_CHILD_TICKET/
     * NOTIFY_CUSTOMER/UPDATE_STATUS）；CREATE_TICKET 保留 DONE 审计；INVOKE_WORKFLOW 维持 SKIPPED。
     * 失败 → step FAILED + lastError + 中断 + 管理员通知（cs.fulfillment-step-failed）；
     * 全部完成 → 工单 IN_PROGRESS（末步前 ensureInProgress 铺底；尾部 UPDATE_STATUS(RESOLVED) 组合可达 RESOLVED）。
     *
     * @param catalogItemId 目录项 ID
     * @param ticketId      已创建的工单 ID（CREATE_TICKET 步骤的产物）
     * @return 物化的工单履行步骤执行行
     */
    @BizMutation
    List<ErpCsTicketFulfillmentStep> executeFulfillmentSteps(@Name("catalogItemId") String catalogItemId,
                                                             @Name("ticketId") String ticketId,
                                                             IServiceContext context);

    /**
     * 手动重试失败链（UC-CS-12 异常「支持重试，最多 3 次」）：仅 FAILED 步骤（IN_PROGRESS 待审批步骤不重执行），
     * retryCount+1 后刷新读取模板 actionConfig 再重执行；retryCount >= erp-cs.fulfillment-retry-max 拒绝
     * + 管理员通知人工介入。
     */
    @BizMutation
    List<ErpCsTicketFulfillmentStep> retryFulfillment(@Name("ticketId") String ticketId,
                                                      IServiceContext context);

    /**
     * cs-local 轻量审批（UC-CS-12 ② REQUEST_APPROVAL「发起审批链，超时自动审批」）：
     * IN_PROGRESS 守卫；approved=true → DONE + 链恢复推进；approved=false → FAILED + retryCount 置 max
     * （人工决定终局语义，阻断自动重试链）+ lastError=「审批驳回: {comment}」。
     */
    @BizMutation
    ErpCsTicketFulfillmentStep approveFulfillmentStep(@Name("stepId") String stepId,
                                                      @Name("approved") boolean approved,
                                                      @Optional @Name("comment") String comment,
                                                      IServiceContext context);

    /**
     * 履行流程状态跟踪查询（UC-CS-12 后置「履行流程状态可跟踪」）：投影步骤执行行
     * （sequence/actionType/status/retryCount/lastError/executedAt/executedBy）。
     */
    @BizQuery
    List<Map<String, Object>> findFulfillmentProgress(@Name("ticketId") String ticketId,
                                                      IServiceContext context);
}
