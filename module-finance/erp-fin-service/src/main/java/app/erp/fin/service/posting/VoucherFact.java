package app.erp.fin.service.posting;

import java.math.BigDecimal;

/**
 * 凭证分录内部 DTO。由 {@link IErpFinAcctDocProvider#createFacts} 产出，经 {@link IErpFinFactsValidator} 链
 * 校验/改写后，由过账编排服务装配为持久化实体 {@code ErpFinVoucherLine}。
 *
 * <p>解耦 Provider 与持久化实体：Validator 可自由改写本 DTO 列表而无需触碰 ORM session。
 */
public class VoucherFact {
    private String subjectCode;
    private String subjectId;
    private String subjectName;
    private String dcDirection;
    private BigDecimal amount;
    /**
     * 源币种金额（plan R1.9 / P1-MA2-002/009 / P1-MA3-039）。Provider 显式填充，{@code persistVoucher} 忠实写入
     * {@code ErpFinVoucherLine.amountSource}。未设置（null）时 fallback 到 {@link #amount}（单币种向后兼容）。
     */
    private BigDecimal amountSource;
    /**
     * 本位币金额（plan R1.9）。{@code functional = source × exchangeRate}，由 Provider 按文档锁定汇率计算。
     * 未设置（null）时 fallback 到 {@link #amount}。{@link #amount} 保留作 balanceTotals/assertBalanced 的功能金额。
     */
    private BigDecimal amountFunctional;
    private String amountKey;
    private String accountKey;
    private String memo;

    // projectId 为登记册延后列（prj 未迁移），保持 Long
    private String orgId;
    private String partnerId;
    private String departmentId;
    private Long projectId;
    private String warehouseId;
    private String materialId;
    private String costCenterId;
    private String businessType;

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDcDirection() {
        return dcDirection;
    }

    public void setDcDirection(String dcDirection) {
        this.dcDirection = dcDirection;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountSource() {
        return amountSource;
    }

    public void setAmountSource(BigDecimal amountSource) {
        this.amountSource = amountSource;
    }

    public BigDecimal getAmountFunctional() {
        return amountFunctional;
    }

    public void setAmountFunctional(BigDecimal amountFunctional) {
        this.amountFunctional = amountFunctional;
    }

    public String getAmountKey() {
        return amountKey;
    }

    public void setAmountKey(String amountKey) {
        this.amountKey = amountKey;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

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

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getCostCenterId() {
        return costCenterId;
    }

    public void setCostCenterId(String costCenterId) {
        this.costCenterId = costCenterId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
}
