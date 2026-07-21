package app.erp.fin.dao.api;

import app.erp.fin.dao.dto.GlMappingDimensions;

/**
 * GL 映射规则解析器接口（plan 2026-07-21-0827-1 A1）。
 *
 * <p>按 (businessType, accountKey, dimensions, acctSchemaId) 多维匹配 {@code ErpFinGlMappingRule} 规则表，
 * 返回建议的目标科目编码字符串（{@code targetSubjectCode}）。
 *
 * <p>空匹配返回 {@code null}（默认行为）—— 由调用方（{@code ErpFinPostingProcessor.resolveSubjects}）
 * 保留 Provider 既有 {@code subjectCode} 作 fallback；strict-mode 下空匹配由调用方抛
 * {@code ERR_GL_MAPPING_NOT_FOUND}。
 *
 * <p>实现在 {@code app.erp.fin.service.posting.ErpFinGlMappingResolver}（service 模块），由 dao 模块
 * 暴露接口便于 ErpFinPostingProcessor 跨模块注入 + 未来跨工程复用。
 *
 * <p>权威：{@code docs/design/finance/gl-mapping-rules.md §3 优先级链算法}。
 */
public interface IErpFinGlMappingResolver {

    /**
     * 解析目标科目编码。
     *
     * @param businessType 业务类型（字典 erp-fin/business-type，如 AP_INVOICE）
     * @param accountKey   科目映射键（字典 erp-fin/account-key，如 PURCHASE）
     * @param dimensions   业务维度（partnerId/materialId/warehouseId/departmentId/projectId + 可选 partnerGroupId）
     * @param acctSchemaId 账套 ID（{@code null} 表示未指定账套，匹配 acctSchemaId IS NULL 的通配规则）
     * @return 命中规则的 {@code targetSubjectCode}；空匹配返回 {@code null}
     */
    String resolveSubjectCode(String businessType, String accountKey, GlMappingDimensions dimensions,
                              Long acctSchemaId);

    /**
     * 失效进程内缓存并按需全量 reload（{@code ErpFinGlMappingRuleBizModel} 的 {@code save_/update_/delete_}
     * 末尾调用 + operator UI {@code refreshCache} 按钮调用）。
     *
     * <p>多节点部署下仅刷新本节点缓存（{@code docs/design/finance/gl-mapping-rules.md §4.3}）。
     */
    void invalidateCache();
}
