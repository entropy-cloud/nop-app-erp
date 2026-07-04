package app.erp.hr.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;

/**
 * 薪酬过账执行器：跨域经凭证聚合根 Facade {@link IErpFinVoucherBiz} 调用财务过账引擎
 * （processor-extension-pattern 硬规则 2：跨域注入 IErpXxxBiz，不注入 Processor 具体类）。
 *
 * <p>跨域失败隔离的事务边界由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code @Transactional(REQUIRES_NEW)}
 * 承接。本执行器不带 {@code @Transactional}，对齐 {@code ProjectPostingExecutor}/{@code AssetPostingExecutor}。
 * 调用方（{@link SalaryPostingDispatcher}）以 try/catch 包裹。
 */
public class SalaryPostingExecutor {

    @Inject
    IErpFinVoucherBiz voucherBiz;

    public Long postEvent(PostingEvent event) {
        IServiceContext context = IServiceContext.getCtx();
        if (context == null) {
            context = new ServiceContextImpl();
        }
        return voucherBiz.post(event, context);
    }

    public void reverse(String billHeadCode, ErpFinBusinessType businessType) {
        IServiceContext context = IServiceContext.getCtx();
        if (context == null) {
            context = new ServiceContextImpl();
        }
        voucherBiz.reverse(billHeadCode, businessType, context);
    }
}
