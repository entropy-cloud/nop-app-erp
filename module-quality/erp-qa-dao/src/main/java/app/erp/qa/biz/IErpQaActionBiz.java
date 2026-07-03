package app.erp.qa.biz;

import app.erp.qa.dao.entity.ErpQaAction;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 纠正预防措施（CAPA）业务接口。除标准 CRUD 外，定义 CAPA 生命周期
 * （{@code docs/design/quality/state-machine.md §NCR 与 CAPA 的关系`}，效果验证闭环）。
 *
 * <p>方法（{@link BizMutation}）：
 * <ul>
 *   <li>{@link #startAction}：PENDING→IN_PROGRESS。</li>
 *   <li>{@link #completeAction}：IN_PROGRESS→COMPLETED + completedBy/completedAt。</li>
 *   <li>{@link #verifyAction}：COMPLETED + verificationPerson/verificationDate 填写（效果验证）。</li>
 * </ul>
 *
 * <p>非法迁移抛 {@code ErpQaErrors.ERR_INVALID_ACTION_STATUS_TRANSITION}。
 */
public interface IErpQaActionBiz extends ICrudBiz<ErpQaAction> {

    @BizMutation
    ErpQaAction startAction(@Name("actionId") Long actionId, IServiceContext context);

    @BizMutation
    ErpQaAction completeAction(@Name("actionId") Long actionId, IServiceContext context);

    @BizMutation
    ErpQaAction verifyAction(@Name("actionId") Long actionId,
                             @Name("verificationPerson") Long verificationPerson,
                             @Name("verificationDate") java.time.LocalDate verificationDate,
                             IServiceContext context);
}
