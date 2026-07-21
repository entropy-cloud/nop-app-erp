package app.erp.md.dao.dto;

/**
 * 统一 Party 类型枚举（{@code IErpPartyBiz} 跨实体查询的 partyType 维度）。
 *
 * <p>对应 3 个分离实体（{@code docs/design/master-data/unified-party-identity.md §2.1}）：
 * <ul>
 *   <li>{@link #PARTNER} — {@code ErpMdPartner}（客户与供应商统一主数据，外部 Party）。</li>
 *   <li>{@link #EMPLOYEE} — {@code ErpMdEmployee}（职员主数据，内部人员 Party）。</li>
 *   <li>{@link #ORGANIZATION} — {@code ErpMdOrganization}（组织主数据，内部组织 Party）。</li>
 * </ul>
 *
 * <p>每值携带 {@code entityName}（实体全限定名，用于 {@code IDaoProvider#daoFor(Class)} 反查）
 * 和 {@code displayName}（中文，picker 显示）。
 */
public enum ErpPartyType {

    PARTNER("app.erp.md.dao.entity.ErpMdPartner", "往来单位"),
    EMPLOYEE("app.erp.md.dao.entity.ErpMdEmployee", "职员"),
    ORGANIZATION("app.erp.md.dao.entity.ErpMdOrganization", "组织");

    private final String entityName;
    private final String displayName;

    ErpPartyType(String entityName, String displayName) {
        this.entityName = entityName;
        this.displayName = displayName;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
