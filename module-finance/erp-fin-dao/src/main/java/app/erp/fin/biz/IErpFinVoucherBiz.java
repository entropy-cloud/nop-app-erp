
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinVoucher;

/**
 * 凭证聚合根 Biz（过账记录的主实体契约）。CRUD 之外，承载业财过账的两个动作入口：
 * <ul>
 *   <li>{@link #post(PostingEvent, IServiceContext)} —— 从业务事件创建并过账凭证（工厂+过账入口，幂等）。</li>
 *   <li>{@link #reverse(String, ErpFinBusinessType, IServiceContext)} —— 按回链反查原已过账凭证，生成红字冲销凭证。</li>
 * </ul>
 *
 * <p>Facade 只负责入口/事务/参数；编排（步骤分解）委托 finance-service 的 {@code ErpFinPostingProcessor}。
 * 事务入口钉在 {@code @BizMutation}；跨域失败隔离（过账失败回滚独立事务）由 {@code post()} 显式声明
 * {@code @Transactional(propagation=REQUIRES_NEW)}（见 {@code processor-extension-pattern.md} 硬规则 1）。
 */
public interface IErpFinVoucherBiz extends ICrudBiz<ErpFinVoucher> {

    /**
     * 从业务事件创建并过账凭证。幂等：源单据已过账时返回 {@code null}。
     *
     * @return 新建凭证 ID；源单据已过账（幂等命中）返回 {@code null}
     */
    @BizMutation
    Long post(@Name("event") PostingEvent event, IServiceContext context);

    /**
     * 按业财回链反查原已过账凭证，生成红字冲销凭证。
     *
     * @return 红字凭证 ID；找不到原已过账凭证抛 {@code NopException}
     */
    @BizMutation
    Long reverse(@Name("billHeadCode") String billHeadCode,
                 @Name("businessType") ErpFinBusinessType businessType,
                 IServiceContext context);

    /**
     * 凭证过账：DRAFT→POSTED。仅切换凭证状态；不再生成新凭证。
     * 与 {@link #post(PostingEvent, IServiceContext)}（业财事件→凭证工厂）不同——本方法作用于已存在的 DRAFT 凭证。
     */
    @BizMutation
    ErpFinVoucher postVoucher(@Name("voucherId") Long voucherId, IServiceContext context);

    /**
     * 凭证红冲：标记原凭证为已红冲并生成反向凭证的简化入口（按 voucherId 调用 reverse）。
     */
    @BizMutation
    ErpFinVoucher reverseVoucher(@Name("voucherId") Long voucherId, IServiceContext context);
}
