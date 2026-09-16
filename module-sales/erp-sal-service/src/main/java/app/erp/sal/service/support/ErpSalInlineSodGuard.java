package app.erp.sal.service.support;

import app.erp.common.service.SoDGuard;
import app.erp.sal.service.ErpSalErrors;
import io.nop.api.core.auth.IUserContext;
import io.nop.core.context.IServiceContext;

/**
 * P2-CK-sal-014：INLINE xbiz 动作的 SoD 守卫桥（全仓首例 INLINE SoD，无脚本内读 AppConfig 先例）。
 * 委托 {@link SoDGuard#assertApproverNotCreator} 复用同一 {@code erp-common.sod-enabled} 总开关
 * （%test profile 关闭以容纳 admin 单账号 create+approve 的 E2E 范式）与 null 容忍语义。
 * 供 {@code ErpSalContract.xbiz} approve 等 INLINE source 经 {@code inject(...)} 调用。
 */
public class ErpSalInlineSodGuard {

    public void assertApproverNotCreator(String createdBy, IServiceContext context) {
        // 与 SoDGuard PROC 路径同源：读 IUserContext 线程局部（svcCtx.getUserId() 在 RPC 管道中
        // 不反映测试/拦截器内 IUserContext.set 的运行时身份）。
        String userId = null;
        try {
            IUserContext uc = IUserContext.get();
            userId = uc == null ? null : uc.getUserId();
        } catch (Exception e) {
            // null-user 语义放行（同 SoDGuard null 容忍）
        }
        SoDGuard.assertApproverNotCreator(createdBy, userId, ErpSalErrors.ERR_SAL_APPROVER_IS_CREATOR);
    }
}
