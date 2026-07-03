
package app.erp.crm.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.crm.dao.entity.ErpCrmLeadScore;

/**
 * 线索评分业务接口。除标准 CRUD 外，定义 config 驱动评分计算 {@link #recalculateScore}。
 *
 * <p>对齐 {@code docs/design/crm/lead-scoring.md}（评分引擎权威设计）。
 */
public interface IErpCrmLeadScoreBiz extends ICrudBiz<ErpCrmLeadScore> {

    /**
     * 重算指定线索的评分：加载 active config，按 configLine 的 scoringMethod（LOOKUP/FORMULA/BOOLEAN）
     * 逐准则计分，归一化 totalScore（0-100），写 append-only 评分记录 + 行级快照；
     * totalScore ≥ autoQualifyThreshold 且 leadType=LEAD 且 docStatus=NEW 且 config-gated 时复用 3.1 qualify 自动转商机。
     *
     * @param leadId       线索 ID
     * @param triggerEvent 触发事件（MANUAL/LEAD_UPDATE/SCHEDULED），为空按 MANUAL 处理
     * @return 评分记录；无 active config 时返回 null（triggeredAction=NONE，不阻断）
     */
    @BizMutation
    ErpCrmLeadScore recalculateScore(@Name("leadId") Long leadId,
                                     @Optional @Name("triggerEvent") String triggerEvent,
                                     IServiceContext context);
}
