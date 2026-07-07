package app.erp.inv.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
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
 *
 * <p>O-9：补充 {@link #reverse} 方法，对齐 {@code AssetPostingExecutor}/{@code ProjectPostingExecutor} 语义，
 * 使存货过账链同样支持红字冲销（原 reverse 在 inventory 域缺失，调用方无统一入口触发冲销）。
 * 原正常凭证 isReversed 补标由引擎公共流程（{@code ErpFinPostingProcessor.reverseProcess()}）统一处理。
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

    /**
     * O-9：存货过账红字冲销入口。透传至 Facade {@link IErpFinVoucherBiz#reverse}，由引擎按回链反查原已过账凭证
     * 生成红字冲销凭证，并经公共流程补标原凭证 isReversed=true。
     */
    public void reverse(String billHeadCode, ErpFinBusinessType businessType) {
        IServiceContext context = IServiceContext.getCtx();
        if (context == null) {
            context = new ServiceContextImpl();
        }
        voucherBiz.reverse(billHeadCode, businessType, context);
    }
}
