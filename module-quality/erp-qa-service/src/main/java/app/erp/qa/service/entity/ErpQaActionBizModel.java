
package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaActionBiz;
import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CAPA BizModel。实现纠正预防措施生命周期（{@code docs/design/quality/state-machine.md §NCR 与 CAPA 的关系`}）。
 *
 * <p>迁移：startAction（PENDING→IN_PROGRESS）、completeAction（IN_PROGRESS→COMPLETED + completedBy/completedAt）、
 * verifyAction（COMPLETED + verificationPerson/verificationDate 填写，效果验证）。
 * 非法迁移抛 {@link ErpQaErrors#ERR_INVALID_ACTION_STATUS_TRANSITION}。
 */
@BizModel("ErpQaAction")
public class ErpQaActionBizModel extends CrudBizModel<ErpQaAction> implements IErpQaActionBiz {

    public ErpQaActionBizModel() {
        setEntityName(ErpQaAction.class.getName());
    }

    @Override
    @BizMutation
    public ErpQaAction startAction(@Name("actionId") Long actionId, IServiceContext context) {
        ErpQaAction action = requireAction(actionId, context);
        requireActionStatus(action, ErpQaConstants.ACTION_STATUS_PENDING, "PENDING");
        action.setStatus(ErpQaConstants.ACTION_STATUS_IN_PROGRESS);
        dao().updateEntity(action);
        return action;
    }

    @Override
    @BizMutation
    public ErpQaAction completeAction(@Name("actionId") Long actionId, IServiceContext context) {
        ErpQaAction action = requireAction(actionId, context);
        requireActionStatus(action, ErpQaConstants.ACTION_STATUS_IN_PROGRESS, "IN_PROGRESS");
        action.setStatus(ErpQaConstants.ACTION_STATUS_COMPLETED);
        action.setCompletedAt(LocalDateTime.now());
        // completedBy 为 Long（职员 ID），IUserContext userId 为 String；此处留空，由前端按职员录入
        dao().updateEntity(action);
        return action;
    }

    @Override
    @BizMutation
    public ErpQaAction verifyAction(@Name("actionId") Long actionId,
                                    @Name("verificationPerson") Long verificationPerson,
                                    @Name("verificationDate") LocalDate verificationDate,
                                    IServiceContext context) {
        ErpQaAction action = requireAction(actionId, context);
        Integer current = action.getStatus();
        // 验证须在 COMPLETED（或已含验证的 COMPLETED）上进行
        if (current == null || current != ErpQaConstants.ACTION_STATUS_COMPLETED) {
            throw new NopException(ErpQaErrors.ERR_ACTION_VERIFY_REQUIRES_COMPLETED)
                    .param(ErpQaErrors.ARG_ACTION_ID, actionId);
        }
        if (verificationPerson == null || verificationDate == null) {
            throw new NopException(ErpQaErrors.ERR_ACTION_VERIFY_MISSING_FIELDS)
                    .param(ErpQaErrors.ARG_ACTION_ID, actionId);
        }
        action.setVerificationPerson(verificationPerson);
        action.setVerificationDate(verificationDate);
        dao().updateEntity(action);
        return action;
    }

    // ---------- helpers ----------

    private ErpQaAction requireAction(Long actionId, IServiceContext context) {
        if (actionId == null) {
            throw new NopException(ErpQaErrors.ERR_ACTION_NOT_FOUND).param(ErpQaErrors.ARG_ACTION_ID, actionId);
        }
        return requireEntity(String.valueOf(actionId), null, context);
    }

    private void requireActionStatus(ErpQaAction action, int expected, String expectedLabel) {
        Integer current = action.getStatus();
        if (current == null || current != expected) {
            throw illegalActionTransition(action, current, expectedLabel);
        }
    }

    private NopException illegalActionTransition(ErpQaAction action, Integer current, String expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_ACTION_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_ACTION_ID, action.getId())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, expected);
    }
}
