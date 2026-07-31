package app.erp.common.service;

import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;

import java.util.Objects;

/**
 * 职责分离（Segregation of Duties）程序级守卫（plan 2026-07-31-1023-2 R3.3）。
 *
 * <p>阻断单据创建人审核自己创建的单据：当 {@code createdBy} 与当前审核人 userId 相等（且均非 null）时抛
 * {@code NopException(errorCode)}。null-user 语义为放行（保留 wf 回调路径既有行为，回调未填 IUserContext 线程局部）。
 *
 * <p>静态工具，供三类 approve 实现模式共享（Pattern A 基类 / Pattern B 独立 facade / Pattern C per-mutation inline），
 * 因 Pattern B facade 不继承 {@link AbstractProcessor}，无法用实例方法共享，故此处为静态入口。
 */
public final class SoDGuard {

    public static final String ARG_USER_ID = "userId";

    private SoDGuard() {
    }

    public static void assertApproverNotCreator(String createdBy, String approverUserId, ErrorCode errorCode) {
        if (createdBy == null || approverUserId == null) {
            return;
        }
        if (Objects.equals(createdBy, approverUserId)) {
            throw new NopException(errorCode).param(ARG_USER_ID, approverUserId);
        }
    }
}
