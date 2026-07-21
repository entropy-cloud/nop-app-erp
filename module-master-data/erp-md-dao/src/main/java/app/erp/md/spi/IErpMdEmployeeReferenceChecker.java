package app.erp.md.spi;

import java.util.Map;

/**
 * 职员业务单据引用计数 SPI（{@code IErpPartyBiz.findReferences(EMPLOYEE, ...)} 跨域解耦端口）。
 *
 * <p>与既有 {@link IErpMdPartnerReferenceChecker} 严格同构（Path A）：
 * master-data 是基础域，不得反向依赖 purchase/sales/inventory/hr（依赖环约束，见
 * {@code docs/architecture/domain-module-split-analysis.md}）。职员被业务单据引用的计数经本 SPI 解耦：
 * master-data 仅声明端口，下游域（purchase/sales/inventory/hr 等）各自实现并注册
 * （{@link jakarta.inject.Inject}{@code (required=false)} 单实例 nullable 注入）。
 *
 * <p>默认无实现时返回空 Map。下游域注册实现归 Deferred successor
 * （{@code docs/design/master-data/unified-party-identity.md §4.3 方案 (a)}）。
 */
public interface IErpMdEmployeeReferenceChecker {

    /**
     * 统计指定职员被各业务单据引用的行数。
     *
     * @param employeeId 职员 ID
     * @return key=引用域名（如 {@code purchaseOrderBuyer}/{@code salesOrderSalesman} 等），
     *         value=引用行数。无引用或无实现返回空 Map。
     */
    Map<String, Long> countReferences(Long employeeId);
}
