package app.erp.md.dao.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一 Party 投影 DTO（{@code IErpPartyBiz.findParties}/{@code IErpPartyBiz.getParty} 返回值）。
 *
 * <p>跨 {@link ErpPartyType} 三实体投影为统一结构，跨域调用方不需感知具体实体类型
 * （{@code docs/design/master-data/unified-party-identity.md §2.1 字段对齐表}）。
 *
 * <p>字段说明：
 * <ul>
 *   <li>{@code partyType} — Party 类型（PARTNER/EMPLOYEE/ORGANIZATION）。</li>
 *   <li>{@code partyId} — 实体主键 ID。</li>
 *   <li>{@code code}/{@code name}/{@code status} — 三实体公共字段。</li>
 *   <li>{@code phone}/{@code email} — Partner/Employee 公共字段；**Organization 投影为 null**（无对应列）。</li>
 *   <li>{@code displayName} — 拼接 {@code code + " - " + name}，picker 默认显示。</li>
 *   <li>{@code extension} — 实体特定字段容器（Map）：
 *     <ul>
 *       <li>PARTNER: {@code partnerType}。</li>
 *       <li>EMPLOYEE: {@code position}/{@code orgId}/{@code partnerId}。</li>
 *       <li>ORGANIZATION: {@code orgType}/{@code parentId}/{@code functionalCurrencyId}。</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class PartyRef {

    private ErpPartyType partyType;
    private Long partyId;
    private String code;
    private String name;
    private String phone;
    private String email;
    private String status;
    private String displayName;
    private Map<String, Object> extension;

    public PartyRef() {
    }

    public ErpPartyType getPartyType() {
        return partyType;
    }

    public void setPartyType(ErpPartyType partyType) {
        this.partyType = partyType;
    }

    public Long getPartyId() {
        return partyId;
    }

    public void setPartyId(Long partyId) {
        this.partyId = partyId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, Object> getExtension() {
        return extension;
    }

    public void setExtension(Map<String, Object> extension) {
        this.extension = extension;
    }

    public PartyRef putExtension(String key, Object value) {
        if (extension == null) {
            extension = new LinkedHashMap<>();
        }
        extension.put(key, value);
        return this;
    }
}
