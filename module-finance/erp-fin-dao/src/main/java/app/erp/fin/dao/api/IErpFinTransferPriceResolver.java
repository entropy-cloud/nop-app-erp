package app.erp.fin.dao.api;

import app.erp.fin.dao.dto.TransferPriceResult;

/**
 * 跨法人转移定价规则解析器接口（plan 2026-07-22-1000-1 A3，multi-company.md §转移定价规则模型）。
 *
 * <p>按 (fromOrgId, toOrgId, materialId, businessDate) 匹配 {@code ErpFinIntercompanyTransferPrice} 规则表，
 * 返回三策略（cost-plus / market / negotiated）计算后的定价结果。
 *
 * <p>优先级链：精确(fromOrgId+toOrgId+materialId) → materialCategoryId 回落 → 全通配 default → 空匹配返回 {@code null}。
 *
 * <p>实现在 {@code app.erp.fin.service.posting.ErpFinTransferPriceResolver}（service 模块），进程内缓存 +
 * 主动失效（对齐 A1 {@code IErpFinGlMappingResolver} 范式）。
 *
 * <p>权威：{@code docs/architecture/multi-company.md §转移定价规则模型}。
 */
public interface IErpFinTransferPriceResolver {

    /**
     * 解析转移定价。
     *
     * @param fromOrgId     调出组织 ID（法人根）
     * @param toOrgId       调入组织 ID（法人根）
     * @param materialId    物料 ID
     * @param businessDate  业务日期（用于 validFrom/validTo 有效期匹配，C3 MUTEX 语义）
     * @return 命中规则并计算后的定价结果；空匹配返回 {@code null}（保留 Provider fallback）
     */
    TransferPriceResult resolvePrice(Long fromOrgId, Long toOrgId, Long materialId,
                                     java.time.LocalDate businessDate);

    /**
     * 失效进程内缓存并按需全量 reload。
     */
    void invalidateCache();
}
