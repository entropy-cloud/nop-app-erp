package app.erp.common.service;

import io.nop.api.core.annotations.core.Locale;
import io.nop.api.core.exceptions.ErrorCode;

/**
 * 跨域共享 Processor 基类的通用错误码（plan 2026-07-24-2200-1 Phase 1）。
 *
 * <p>具体域应优先使用各自 {@code *Errors.java} 中定义的域特有错误码（更精准的描述）；
 * 本接口仅提供抽象基类默认实现使用的兜底错误码。
 */
@Locale("zh-CN")
public interface ErpCommonErrors {
    String ARG_BIZ_OBJ_NAME = "bizObjName";
    String ARG_BIZ_OBJ_ID = "bizObjId";
    String ARG_CURRENT_STATUS = "currentStatus";
    String ARG_EXPECTED_STATUS = "expectedStatus";

    ErrorCode ERR_ENTITY_NOT_FOUND = ErrorCode.define(
            "nop.err.erp.common.entity-not-found",
            "实体不存在：{bizObjName}#{bizObjId}",
            ARG_BIZ_OBJ_NAME, ARG_BIZ_OBJ_ID
    );

    /**
     * @deprecated "先抛通用码再外层转码"为被迫转码反模式（lesson 19：全仓核实该码无与实体无关的通用消费方，
     * 唯一消费方是转码层自己；未建转码层的实体则 common 码裸奔）。全仓 StateMachine/Processor 自
     * plan {@code 2026-09-07-2200-1} 起直抛领域错误码，本码零生产引用；定义保留防外部断裂，
     * 新代码禁用——领域组件应直抛 {@code erp.err.<domain>.illegal-status-transition} 或实体专属码。
     */
    @Deprecated
    ErrorCode ERR_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "nop.err.erp.common.illegal-status-transition",
            "非法状态转换：当前={currentStatus}，期望={expectedStatus}",
            ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS
    );

    // --- F1.3（ai-check P1-CK-pur-003 族）：通用 CRUD 状态锁 ---
    String ARG_ENTITY_NAME = "entityName";
    String ARG_ENTITY_KEY = "entityKey";
    String ARG_ACTION = "action";

    ErrorCode ERR_CRUD_STATUS_LOCKED = ErrorCode.define(
            "nop.err.erp.common.crud-status-locked",
            "单据已审核/已过账锁定，禁止通用{action}：{entityName}#{entityKey}（合法编辑请经状态机动作）",
            ARG_ACTION, ARG_ENTITY_NAME, ARG_ENTITY_KEY
    );

    ErrorCode ERR_CRUD_IMMUTABLE_ENTITY = ErrorCode.define(
            "nop.err.erp.common.crud-immutable-entity",
            "不可变台账实体，禁止通用{action}：{entityName}#{entityKey}（台账由域内编排驱动）",
            ARG_ACTION, ARG_ENTITY_NAME, ARG_ENTITY_KEY
    );

    // --- P2-CK-sal-013：withdrawApproval 仅提交人可操作（跨域共用骨架一点修，C8.2 全域盘点归后） ---
    ErrorCode ERR_WITHDRAW_NOT_SUBMITTER = ErrorCode.define(
            "erp.err.common.withdraw-not-submitter",
            "仅提交人可撤回提交：单据创建人 {createdBy} 与当前操作人 {userId} 不一致",
            "createdBy", "userId"
    );

    // --- P3-CK-common-012-r3：org 隔离解析失败 fail-closed ---
    ErrorCode ERR_ORG_ISOLATION_RESOLVE_FAILED = ErrorCode.define(
            "erp.err.common.org-isolation-resolve-failed",
            "实体 {entityName} 组织隔离元数据解析失败，fail-closed 模式下拒绝查询（拒答优于未隔离放行）",
            ARG_ENTITY_NAME
    );
}
