package app.erp.common.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 跨域共享 Processor 基类的通用错误码（plan 2026-07-24-2200-1 Phase 1）。
 *
 * <p>具体域应优先使用各自 {@code *Errors.java} 中定义的域特有错误码（更精准的描述）；
 * 本接口仅提供抽象基类默认实现使用的兜底错误码。
 */
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

    ErrorCode ERR_ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "nop.err.erp.common.illegal-status-transition",
            "非法状态转换：当前={currentStatus}，期望={expectedStatus}",
            ARG_CURRENT_STATUS, ARG_EXPECTED_STATUS
    );
}
