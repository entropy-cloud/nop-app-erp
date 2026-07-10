package app.erp.mnt.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.PostingEvent;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import jakarta.inject.Inject;

/**
 * 维护域过账执行器：跨域经凭证聚合根 Facade {@link IErpFinVoucherBiz} 调用财务过账引擎
 * （{@code processor-extension-pattern.md} 硬规则 2：跨域注入 IErpXxxBiz，不注入 Processor 具体类）。
 *
 * <p>跨域失败隔离的事务边界由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code @Transactional(REQUIRES_NEW)}
 * 承接（硬规则 1：事务边界钉 Facade，不下放编排层）。本执行器不带 {@code @Transactional}，对齐
 * {@code MfgPostingExecutor} / {@code InvPostingExecutor} 范式。
 *
 * <p>当前服务于备件消耗 GL 过账（{@link MaintenanceIssuePostingDispatcher}）；后续 maintenance 域其它
 * 业财过账（如维修工时费用化）可复用本执行器入口。
 */
public class MntPostingExecutor {

    @Inject
    IErpFinVoucherBiz voucherBiz;

    public Long postEvent(PostingEvent event) {
        IServiceContext context = IServiceContext.getCtx();
        if (context == null) {
            context = new ServiceContextImpl();
        }
        return voucherBiz.post(event, context);
    }
}
