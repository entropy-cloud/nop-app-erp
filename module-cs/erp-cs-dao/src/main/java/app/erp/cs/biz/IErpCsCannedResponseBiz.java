package app.erp.cs.biz;

import app.erp.cs.dao.entity.ErpCsCannedResponse;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;
import java.util.Map;

public interface IErpCsCannedResponseBiz extends ICrudBiz<ErpCsCannedResponse> {

    /**
     * 渲染预设应答模板（{@code canned-response.md} §3.2）。
     *
     * <p>系统变量（customer_name/ticket_id/agent_name/today/now）自动解析，
     * customVariables 覆盖同名系统变量。必填变量缺失抛
     * {@code ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING}。
     *
     * @param cannedResponseId 预设应答 id
     * @param ticketId         关联工单 id（解析系统变量用）
     * @param customVariables  客服手动填入的自定义变量
     * @return 渲染后的正文
     */
    @BizQuery
    String renderTemplate(@Name("cannedResponseId") Long cannedResponseId,
                          @Name("ticketId") Long ticketId,
                          @Optional @Name("customVariables") Map<String, String> customVariables,
                          IServiceContext context);

    /**
     * 宏自动匹配（{@code canned-response.md} §二）。三级匹配：精确（type+priority）> 类型（type，priority 空）
     * > 全局兜底（type+priority 均空），按 sequence ASC 取前 {@code macro-count} 条。
     *
     * @param ticketId 工单 id（取 ticketTypeId + priority 作为匹配条件）
     * @return 匹配到的预设应答列表（≤ macro-count 条）
     */
    @BizQuery
    List<ErpCsCannedResponse> suggestForTicket(@Name("ticketId") Long ticketId,
                                                IServiceContext context);

    /**
     * 应用预设应答（{@code canned-response.md} §3.1/§3.2）。
     *
     * <p>校验 active → 渲染 → usageCount+1 持久化 → 经 {@code IErpCsTicketActionBiz.save}
     * 写 TicketAction(actionType=NOTE, content=渲染后正文) → 返回渲染后 content。
     *
     * @param cannedResponseId 预设应答 id
     * @param ticketId         关联工单 id
     * @param customVariables  客服手动填入的自定义变量
     * @return 渲染后的正文
     */
    @BizMutation
    String applyCannedResponse(@Name("cannedResponseId") Long cannedResponseId,
                               @Name("ticketId") Long ticketId,
                               @Optional @Name("customVariables") Map<String, String> customVariables,
                               IServiceContext context);
}
