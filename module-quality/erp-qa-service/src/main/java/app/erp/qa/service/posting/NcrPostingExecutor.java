package app.erp.qa.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import jakarta.inject.Inject;

/**
 * NCR 过账执行器：跨域经凭证聚合根 Facade {@link IErpFinVoucherBiz} 调用财务过账引擎
 * （{@code processor-extension-pattern.md} 硬规则 2：跨域注入 IErpXxxBiz，不注入 Processor 具体类）。
 *
 * <p>跨域失败隔离的事务边界由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code @Transactional(REQUIRES_NEW)}
 * 承接（硬规则 1：事务边界钉 Facade）。本执行器不再自带 {@code @Transactional}。
 *
 * <p>承接 {@code InvPostingExecutor} 范式（plan 2026-07-05-2352-2）。
 */
public class NcrPostingExecutor {

    @Inject
    IErpFinVoucherBiz voucherBiz;

    public Long postEvent(PostingEvent event) {
        IServiceContext context = IServiceContext.getCtx();
        if (context == null) {
            context = new ServiceContextImpl();
        }
        return voucherBiz.post(event, context);
    }

    /**
     * O-15：对齐其他域执行器（{@code AssetPostingExecutor}/{@code ProjectPostingExecutor}/{@code InvPostingExecutor}）
     * 的返回类型 {@code void}。原返回 {@code Long}（voucherId）与其他域不一致，且调用方不消费该返回值。
     * voucherId 已由引擎写入执行上下文（凭证回链表 + 红冲凭证实体），无需经返回值传递。
     */
    public void reverse(String billHeadCode, ErpFinBusinessType businessType) {
        IServiceContext context = IServiceContext.getCtx();
        if (context == null) {
            context = new ServiceContextImpl();
        }
        voucherBiz.reverse(billHeadCode, businessType, context);
    }
}
