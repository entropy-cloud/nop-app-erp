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
    private String orgId;
    private String partnerId;
    private String partnerGroupId;
    private String materialId;
    private String materialCategoryId;
    private String warehouseId;
    private String departmentId;
    private Long projectId;
    /** A3 intercompany 维度（plan 2026-07-22-1000-1，multi-company.md §与 Posting+GL Mapping 关系）：跨法人交易双方组织。 */
    private String fromOrgId;
    /** A3 intercompany 维度：调入方组织。 */
    private String toOrgId;

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerGroupId() {
        return partnerGroupId;
    }

    public void setPartnerGroupId(String partnerGroupId) {
        this.partnerGroupId = partnerGroupId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getMaterialCategoryId() {
        return materialCategoryId;
    }

    public void setMaterialCategoryId(String materialCategoryId) {
        this.materialCategoryId = materialCategoryId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getFromOrgId() {
        return fromOrgId;
    }

    public void setFromOrgId(String fromOrgId) {
        this.fromOrgId = fromOrgId;
    }

    public String getToOrgId() {
        return toOrgId;
    }

    public void setToOrgId(String toOrgId) {
        this.toOrgId = toOrgId;
    }
}
