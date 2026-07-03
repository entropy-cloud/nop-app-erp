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
    private Long subjectId;
    private String subjectName;
    private String dcDirection;
    private BigDecimal amount;
    private String amountKey;
    private String accountKey;
    private String memo;

    private Long partnerId;
    private Long departmentId;
    private Long projectId;
    private Long warehouseId;
    private Long materialId;
    private Long costCenterId;
    private String businessType;

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
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

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getCostCenterId() {
        return costCenterId;
    }

    public void setCostCenterId(Long costCenterId) {
        this.costCenterId = costCenterId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
}
