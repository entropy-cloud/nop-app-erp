package app.erp.all.meta;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 业务模块元数据读取器错误码。供升级前依赖完整性检查（{@link ModuleMetaReader#checkDependencyIntegrity()}）
 * 的调用方在需要硬失败时抛出 {@link io.nop.api.core.exceptions.NopException}。
 *
 * <p>读取器本身的诊断查询返回结果对象，不直接抛异常（诊断场景允许返回不完整清单）。
 */
public interface ErpModuleMetaErrors {

    String ARG_MODULE_ID = "moduleId";
    String ARG_DEPENDENCY_ID = "dependencyId";
    String ARG_EXPECTED = "expected";
    String ARG_ACTUAL = "actual";

    ErrorCode ERR_MODULE_DEPENDENCY_MISSING = ErrorCode.define(
            "erp.err.module-meta.dependency-missing",
            "模块业务依赖缺失：模块 {moduleId} 声明依赖 {dependencyId}，但 classpath 中不存在该模块",
            ARG_MODULE_ID, ARG_DEPENDENCY_ID);

    ErrorCode ERR_MODULE_VERSION_MISMATCH = ErrorCode.define(
            "erp.err.module-meta.version-mismatch",
            "模块业务依赖版本不匹配：模块 {moduleId} 依赖 {dependencyId}，期望版本 {expected}，实际版本 {actual}",
            ARG_MODULE_ID, ARG_DEPENDENCY_ID, ARG_EXPECTED, ARG_ACTUAL);
}
