package app.erp.fin.dao.dto;

/**
 * GL 映射规则解析的维度输入 DTO（plan 2026-07-21-0827-1 A1）。
 *
 * <p>业务原始 ID（partnerId/materialId/warehouseId/departmentId/projectId）由 Provider 在 {@code VoucherFact}
 * 上设置；{@code app.erp.fin.service.posting.ErpFinGlMappingResolver} 内部按需扩展为规则维度（如
 * materialId → materialCategoryId）。
 *
 * <p>{@code partnerGroupId} 为预留扩展点：当前 master-data 无 {@code ErpMdPartnerGroup} 实体，
 * 故 resolver 不会自动从 partnerId 推导；调用方业务上下文已有 partnerGroupId 时可显式传入。
 *
 * <p>权威：{@code docs/design/finance/gl-mapping-rules.md §3.4 维度数据来源}。
 */
public class GlMappingDimensions {
    private Long partnerId;
    private Long partnerGroupId;
    private Long materialId;
    private Long materialCategoryId;
    private Long warehouseId;
    private Long departmentId;
    private Long projectId;
    /** A3 intercompany 维度（plan 2026-07-22-1000-1，multi-company.md §与 Posting+GL Mapping 关系）：跨法人交易双方组织。 */
    private Long fromOrgId;
    /** A3 intercompany 维度：调入方组织。 */
    private Long toOrgId;

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public Long getPartnerGroupId() {
        return partnerGroupId;
    }

    public void setPartnerGroupId(Long partnerGroupId) {
        this.partnerGroupId = partnerGroupId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getMaterialCategoryId() {
        return materialCategoryId;
    }

    public void setMaterialCategoryId(Long materialCategoryId) {
        this.materialCategoryId = materialCategoryId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getFromOrgId() {
        return fromOrgId;
    }

    public void setFromOrgId(Long fromOrgId) {
        this.fromOrgId = fromOrgId;
    }

    public Long getToOrgId() {
        return toOrgId;
    }

    public void setToOrgId(Long toOrgId) {
        this.toOrgId = toOrgId;
    }
}
