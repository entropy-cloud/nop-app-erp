
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.service.posting.ErpFinPostingProcessor;
import jakarta.inject.Inject;

/**
 * 凭证聚合根 Biz（过账记录主实体）。CRUD 之外承载业财过账的两个动作入口（{@code post}/{@code reverse}），
 * 为过账引擎 Facade（{@code processor-extension-pattern.md} 两层结构）。Facade 只负责入口/事务/参数透传，
 * 编排委托 {@link ErpFinPostingProcessor}。
 *
 * <p>事务入口钉在 {@link BizMutation}：{@link #post} 叠加 {@link Transactional}(REQUIRES_NEW) 承接跨域失败隔离
 * （过账失败回滚独立事务，不污染源单据主事务；语义承接自原 {@code InvPostingExecutor}）——这是
 * {@code processor-extension-pattern.md} 硬规则 1 的显式独立事务边界声明，故此处特意叠加 @Transactional。
 * ORM Session 由编排层 {@link ErpFinPostingProcessor} 的 {@code @SingleSession} 承接（@SingleSession 原位于
 * 重构前的过账入口方法、现迁移至编排方法），使 Session 作用域精确覆盖 ORM 工作、在编排方法返回时刷新——
 * 这样跨域调用方（{@code InvPostingDispatcher}）的 try/catch 能稳定捕获过账异常（事务/Session 边界不自洽问题见 plan 闭合记录）。
 */
@BizModel("ErpFinVoucher")
public class ErpFinVoucherBizModel extends CrudBizModel<ErpFinVoucher> implements IErpFinVoucherBiz {
    public ErpFinVoucherBizModel() {
        setEntityName(ErpFinVoucher.class.getName());
    }

    @Inject
    ErpFinPostingProcessor postingProcessor;

    @Override
    @BizMutation
    @Transactional(propagation = TransactionPropagation.REQUIRES_NEW)
    public Long post(@Name("event") PostingEvent event, IServiceContext context) {
        return postingProcessor.process(event, context);
    }

    @Override
    @BizMutation
    public Long reverse(@Name("billHeadCode") String billHeadCode,
                        @Name("businessType") ErpFinBusinessType businessType,
                        IServiceContext context) {
        return postingProcessor.reverseProcess(billHeadCode, businessType, context);
    }
}
