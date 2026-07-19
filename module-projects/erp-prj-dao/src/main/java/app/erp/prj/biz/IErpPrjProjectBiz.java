package app.erp.prj.biz;

import app.erp.prj.dao.entity.ErpPrjProject;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;

/**
 * 项目 Biz 契约。CRUD 之上承载项目状态引用校验与成本归集回写：
 * <ul>
 *   <li>{@link #requireReferenceable(Long, IServiceContext)}：跨域调用方校验项目可被新单据引用
 *       （status=OPEN，对齐 {@code cost-collection.md §七 关键业务规则 1}）。</li>
 *   <li>{@link #refreshActualCost(Long, IServiceContext)}：聚合归集行金额回写 {@code actualCost}。</li>
 *   <li>{@link #closeProject(Long, IServiceContext)}：OPEN→COMPLETED 关闭冻结（对齐 §4.3）。</li>
 * </ul>
 */
public interface IErpPrjProjectBiz extends ICrudBiz<ErpPrjProject> {

    /**
     * 校验项目可被新单据引用。非 OPEN 状态抛 {@code ERR_PROJECT_NOT_REFERENCEABLE}。
     * 跨域（如费用报销归集）调用方在写归集前调用本方法。
     */
    @BizMutation
    ErpPrjProject requireReferenceable(@Name("projectId") Long projectId, IServiceContext context);

    /**
     * 聚合项目所有归集行金额 → 回写 {@code actualCost}。返回刷新后的实际成本合计。
     */
    @BizMutation
    BigDecimal refreshActualCost(@Name("projectId") Long projectId, IServiceContext context);

    /**
     * 关闭项目（OPEN→COMPLETED）。关闭后不可再被新单据引用（{@link #requireReferenceable} 拒绝）。
     */
    @BizMutation
    ErpPrjProject closeProject(@Name("projectId") Long projectId, IServiceContext context);

    /**
     * 启动项目（DRAFT→OPEN）。
     */
    @BizMutation
    ErpPrjProject startProject(@Name("projectId") Long projectId, IServiceContext context);

    /**
     * 暂停项目（OPEN→ON_HOLD）。
     */
    @BizMutation
    ErpPrjProject holdProject(@Name("projectId") Long projectId, IServiceContext context);

    /**
     * 恢复项目（ON_HOLD→OPEN）。
     */
    @BizMutation
    ErpPrjProject resumeProject(@Name("projectId") Long projectId, IServiceContext context);

    /**
     * 取消项目（DRAFT/OPEN/ON_HOLD→CANCELLED）。
     */
    @BizMutation
    ErpPrjProject cancelProject(@Name("projectId") Long projectId, IServiceContext context);
}
