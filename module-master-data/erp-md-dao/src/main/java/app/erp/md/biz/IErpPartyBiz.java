package app.erp.md.biz;

import app.erp.md.dao.dto.ErpPartyType;
import app.erp.md.dao.dto.PartyRef;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一 Party 跨实体查询接口（{@code docs/design/master-data/unified-party-identity.md §4}）。
 *
 * <p>首例"非实体 + 跨域 {@code @Inject}"组合（owner doc §4.2 约定）：
 * 当且仅当非实体 BizModel 有跨工程消费者时才暴露 {@code IErp*Biz} 接口；纯 UI 入口（如 Dashboard）保持无接口。
 *
 * <p>跨域消费者（purchase/sales/finance）经 {@code @Inject IErpPartyBiz} 注入，无需感知底层 3 实体类型。
 */
public interface IErpPartyBiz {

    /**
     * 关键字跨实体检索 Party。
     *
     * <p>检索范围：{@code code}/{@code name}/{@code phone}/{@code email} 4 字段 OR 模糊匹配
     * （{@code LIKE '%keyword%'}）；Organization 仅匹配 {@code code}/{@code name}（无 phone/email 列）。
     *
     * @param keyword   关键字（{@code < 2} 字符返回空 List，避免全表扫描）
     * @param partyTypes 限定查询的 Party 类型集合；{@code null} 表示查全部 3 类
     * @param limit     返回行数上限（建议 {@code <= 50}，配置 {@code erp-md.party-search.max-results}）
     * @param context   服务上下文（保留跨域调用数据权限管道）
     * @return 匹配的 Party 列表（按 keyword 命中后 Java merge 截断到 limit；空结果返回空 List 不抛异常）
     */
    @BizQuery
    List<PartyRef> findParties(@Name("keyword") String keyword,
                               @Optional @Name("partyTypes") Set<ErpPartyType> partyTypes,
                               @Optional @Name("limit") Integer limit,
                               IServiceContext context);

    /**
     * 按 partyType + partyId 单点查询。
     *
     * @param partyType Party 类型
     * @param partyId   实体主键 ID
     * @param context   服务上下文
     * @return Party 投影；不存在时返回 {@code null}（不抛异常，便于调用方按 null 处理软删数据）
     */
    @BizQuery
    PartyRef getParty(@Name("partyType") ErpPartyType partyType,
                      @Name("partyId") Long partyId,
                      IServiceContext context);

    /**
     * 跨实体引用计数预览（F7 删除引用预览扩展）。
     *
     * <p>返回类型与既有 {@code IErpMdPartnerReferenceChecker.countReferences} 一致
     * （{@code Map<String, Long>}，Path A 严格同构；非 rich DTO）。
     *
     * <p>按 {@code partyType} dispatch：
     * <ul>
     *   <li>PARTNER → 既有 {@code IErpMdPartnerReferenceChecker}（F7 已落地）。</li>
     *   <li>EMPLOYEE → {@code IErpMdEmployeeReferenceChecker}（本计划新增 SPI 端口，下游实现归 Deferred）。</li>
     *   <li>ORGANIZATION → {@code IErpMdOrganizationReferenceChecker}（本计划新增 SPI 端口，下游实现归 Deferred）。</li>
     * </ul>
     *
     * @param partyType Party 类型
     * @param partyId   实体主键 ID
     * @param context   服务上下文
     * @return key=引用域名（如 {@code purchaseOrder}/{@code salesOrder}），value=引用行数。
     *         无引用或 SPI 未注册返回空 Map。
     */
    @BizQuery
    Map<String, Long> findReferences(@Name("partyType") ErpPartyType partyType,
                                     @Name("partyId") Long partyId,
                                     IServiceContext context);
}
