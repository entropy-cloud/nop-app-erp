package app.erp.inv.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.PostingEvent;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import jakarta.inject.Inject;

/**
 * 存货过账执行器：跨域经凭证聚合根 Facade {@link IErpFinVoucherBiz} 调用财务过账引擎
 * （{@code processor-extension-pattern.md} 硬规则 2：跨域注入 IErpXxxBiz，不注入 Processor 具体类）。
 *
 * <p>跨域失败隔离的事务边界（过账失败回滚独立事务，不污染移动单主事务）由 Facade
 * {@code IErpFinVoucherBiz.post()} 的 {@code @Transactional(REQUIRES_NEW)} 承接（硬规则 1：事务边界钉 Facade，
 * 不下放编排层）。本执行器不再自带 {@code @Transactional}。调用方 {@link InvPostingDispatcher} 以
 * try/catch 包裹本方法返回值/异常。
 */
public class InvPostingExecutor {

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
