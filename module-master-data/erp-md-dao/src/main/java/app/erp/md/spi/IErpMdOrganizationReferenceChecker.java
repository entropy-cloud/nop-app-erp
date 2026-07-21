package app.erp.md.spi;

import java.util.Map;

/**
 * 组织业务单据引用计数 SPI（{@code IErpPartyBiz.findReferences(ORGANIZATION, ...)} 跨域解耦端口）。
 *
 * <p>与既有 {@link IErpMdPartnerReferenceChecker} 严格同构（Path A）：
 * master-data 是基础域，不得反向依赖 purchase/sales/inventory 等。组织被业务单据引用的计数经本 SPI 解耦：
 * master-data 仅声明端口，下游域各自实现并注册（{@link jakarta.inject.Inject}{@code (required=false)}
 * 单实例 nullable 注入）。
 *
 * <p>默认无实现时返回空 Map。下游域注册实现归 Deferred successor
 * （{@code docs/design/master-data/unified-party-identity.md §4.3 方案 (a)}）。
 *
 * <p>{@code orgId} 作为审计维度（每条业务单据都有），纳入引用扫描会产生海量噪声，因此
 * {@code docs/design/master-data/unified-party-identity.md §6 Decision} 裁决：{@code orgId} 默认不纳入
 * Organization 的引用扫描；具体扫描字段（如 {@code Warehouse.organizationId}）由下游域 SPI 实现自决。
 */
public interface IErpMdOrganizationReferenceChecker {

    /**
     * 统计指定组织被各业务单据引用的行数。
     *
     * @param organizationId 组织 ID
     * @return key=引用域名（如 {@code warehouse}/{@code department} 等），
     *         value=引用行数。无引用或无实现返回空 Map。
     */
    Map<String, Long> countReferences(Long organizationId);
}
