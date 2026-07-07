package app.erp.prj.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;

/**
 * 项目过账执行器：跨域经凭证聚合根 Facade {@link IErpFinVoucherBiz} 调用财务过账引擎
 * （processor-extension-pattern 硬规则 2：跨域注入 IErpXxxBiz，不注入 Processor 具体类）。
 *
 * <p>跨域失败隔离的事务边界由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code @Transactional(REQUIRES_NEW)}
 * 承接。本执行器不带 {@code @Transactional}，对齐 {@code AssetPostingExecutor}/{@code SalPostingExecutor}。
 * 调用方（各 Dispatcher）以 try/catch 包裹。
 *
 * <p>O-8：{@code markOriginalVoucherReversed} 已上提到引擎公共流程（{@code ErpFinPostingProcessor.reverseProcess()}），
 * 本执行器 {@code reverse()} 仅透传 Facade 调用，不再重复补标原凭证。
 */
public class ProjectPostingExecutor {

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
        // O-8：原正常凭证 isReversed 补标由引擎公共流程统一处理（ErpFinPostingProcessor.reverseProcess）
        voucherBiz.reverse(billHeadCode, businessType, context);
    }
}
