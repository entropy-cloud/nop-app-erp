package app.erp.common.service;

import io.nop.biz.api.IBizObject;
import io.nop.biz.api.IBizObjectManager;
import io.nop.core.context.IServiceContext;
import io.nop.orm.support.OrmEntity;
import io.nop.wf.core.IWorkflow;
import io.nop.wf.core.IWorkflowManager;
import io.nop.wf.core.support.ApprovalFlowHelper;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 提交审批（submitForApproval）编排骨架（plan 2026-07-24-2200-1 Phase 1）。
 *
 * <p>编排骨架：{@code requireEntity → validateTransitionForSubmit → validateBusinessRules
 * → beforeStateChange → doSubmit → afterStateChange → save}。
 *
 * <p>额外职责：**可选 wf 启动**——通过构造函数传入的 {@code bizObjName} 从 xmeta 读取
 * {@code wf:wfName} 属性，有则调 {@code IWorkflowManager.newWorkflow()} 启动工作流。
 * 无 wf:wfName 时仅做状态转换。
 *
 * <p>获取 xmeta 的方式：构造函数强制传入 {@code bizObjName}（如 {@code "ErpPurOrder"}），
 * 通过 {@code IBizObjectManager.getBizObject(bizObjName).getObjMeta().prop_get("wf:wfName")} 读取
 * （IObjMeta 实现 IPropGetMissingHook，XLang 的 {@code objMeta['wf:wfName']} 等价 Java 的 prop_get）。
 * 不使用 {@code entity.getClass().getSimpleName()}（AOP 代理类名不可靠，且方法执行时尚无 entity 实例）。
 */
public abstract class AbstractSubmitForApprovalProcessor<T extends OrmEntity> extends AbstractProcessor<T> {

    protected final String bizObjName;

    @Inject
    protected IBizObjectManager bizObjectManager;

    @Inject
    protected IWorkflowManager workflowManager;

    protected AbstractSubmitForApprovalProcessor(String bizObjName) {
        this.bizObjName = bizObjName;
    }

    public T submitForApproval(String id, IServiceContext context) {
        T entity = requireEntity(id);
        validateNotCancelled(entity, context);
        validateTransitionForSubmit(entity, context);
        validateBusinessRules(entity, context);
        beforeStateChange(entity, context);
        doSubmit(entity, context);
        afterStateChange(entity, context);
        dao().updateEntity(entity);
        maybeStartWorkflow(entity, context);
        return entity;
    }

    protected void validateTransitionForSubmit(T entity, IServiceContext context) {
        String status = getApproveStatus(entity);
        if (!Objects.equals(status, unsubmittedStatus()) && !Objects.equals(status, rejectedStatus())) {
            throw illegalStatusException(entity, status, unsubmittedStatus() + " / " + rejectedStatus());
        }
    }

    protected void doSubmit(T entity, IServiceContext context) {
        setApproveStatus(entity, submittedStatus());
    }

    protected void maybeStartWorkflow(T entity, IServiceContext context) {
        String wfName = resolveWorkflowName();
        if (wfName == null || wfName.isEmpty()) {
            return;
        }
        IWorkflow wf = workflowManager.newWorkflow(wfName, null);
        Map<String, Object> args = new HashMap<>();
        args.put("bizObjName", bizObjName);
        args.put("bizObjId", String.valueOf(entity.orm_id()));
        ApprovalFlowHelper.start(wf, args, context);
    }

    protected String resolveWorkflowName() {
        IBizObject bizObj = bizObjectManager.getBizObject(bizObjName);
        if (bizObj == null || bizObj.getObjMeta() == null) {
            return null;
        }
        Object value = bizObj.getObjMeta().prop_get("wf:wfName");
        return value == null ? null : value.toString();
    }

    protected void validateNotCancelled(T entity, IServiceContext context) {
        if (isCancelled(entity)) {
            throw illegalStatusException(entity, "CANCELLED", "非已作废");
        }
    }

    protected void validateBusinessRules(T entity, IServiceContext context) {
    }

    protected void beforeStateChange(T entity, IServiceContext context) {
    }

    protected void afterStateChange(T entity, IServiceContext context) {
    }

    protected abstract String getApproveStatus(T entity);

    protected abstract void setApproveStatus(T entity, String status);

    protected abstract boolean isCancelled(T entity);

    protected abstract String unsubmittedStatus();

    protected abstract String submittedStatus();

    protected abstract String rejectedStatus();
}
