package app.erp.fin.service.posting;

import java.util.List;

/**
 * 凭证分录校验/改写扩展点。在 Provider 产出原始分录后、借贷平衡校验与写库前执行，
 * 可校验、改写或追加分录行，也可抛 {@code NopException} 阻止过账。
 *
 * <p>多个 Validator 按 {@link #getOrder()} 升序执行。
 */
public interface IErpFinFactsValidator {

    /**
     * 校验/改写分录列表，返回处理后的分录列表（可与入参为不同实例）。
     */
    List<VoucherFact> validate(List<VoucherFact> facts, AcctDocContext ctx);

    /**
     * 执行顺序，升序。多个 Validator 之间通过此值排序。
     */
    int getOrder();
}
