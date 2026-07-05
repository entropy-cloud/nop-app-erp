package app.erp.qa.biz;

import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaRecall;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 不符合项报告（NCR）业务接口。除标准 CRUD 外，定义 NCR 5 态状态机
 * （{@code docs/design/quality/state-machine.md §适用对象二`}）。
 *
 * <p>状态机方法（{@link BizMutation}）：
 * <ul>
 *   <li>{@link #submitReview}：OPEN→IN_REVIEW。</li>
 *   <li>{@link #resolve}：IN_REVIEW→RESOLVED（须全部关联 CAPA ErpQaAction.status=COMPLETED + 验证人/验证日期已填）。</li>
 *   <li>{@link #escalateToRecall}：IN_REVIEW→ESCALATED_TO_RECALL（终态，仅状态迁移占位，不建召回实体）。</li>
 *   <li>{@link #upgradeToRecall}：IN_REVIEW→ESCALATED_TO_RECALL 并生成 ErpQaRecall（triggerType=BATCH_NCR_UPGRADE +
 *       sourceNcrId 关联 + 继承 NCR 物料/严重程度）。召回 2.11 真正入口。</li>
 *   <li>{@link #cancel}：OPEN / IN_REVIEW→CANCELLED。</li>
 * </ul>
 *
 * <p>非法迁移抛 {@code ErpQaErrors.ERR_INVALID_NCR_STATUS_TRANSITION}。
 */
public interface IErpQaNonConformanceBiz extends ICrudBiz<ErpQaNonConformance> {

    @BizMutation
    ErpQaNonConformance submitReview(@Name("ncrId") Long ncrId, IServiceContext context);

    @BizMutation
    ErpQaNonConformance resolve(@Name("ncrId") Long ncrId,
                                @Name("resolution") String resolution,
                                IServiceContext context);

    @BizMutation
    ErpQaNonConformance escalateToRecall(@Name("ncrId") Long ncrId, IServiceContext context);

    @BizMutation
    ErpQaRecall upgradeToRecall(@Name("ncrId") Long ncrId, IServiceContext context);

    @BizMutation
    ErpQaNonConformance cancel(@Name("ncrId") Long ncrId, IServiceContext context);

    /**
     * 人工触发 NCR 过账（MANUAL_POST 模式 + 补过账）。前置：status=RESOLVED + posted=false。
     * SCRAP 处置 → 报废损失凭证；CONCESSION/DOWNGRADE → 拒（ERR_NCR_DISPOSITION_NOT_POSTABLE）；
     * RETURN 处置 → 编排退货域（已由 resolve 触发，postNcr 对 RETURN 拒）。
     */
    @BizMutation
    ErpQaNonConformance postNcr(@Name("ncrId") Long ncrId, IServiceContext context);

    /**
     * 红冲 NCR 已过账凭证（SCRAP 处置）。前置：posted=true。清除 posted 三件套 + 生成红字冲销凭证。
     */
    @BizMutation
    ErpQaNonConformance reverseNcr(@Name("ncrId") Long ncrId, IServiceContext context);
}
