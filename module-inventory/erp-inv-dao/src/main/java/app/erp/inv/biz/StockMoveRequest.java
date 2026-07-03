package app.erp.inv.biz;

import java.time.LocalDate;
import java.util.List;

/**
 * 库存移动单生成请求（{@link IErpInvStockMoveBiz#generateMove} 入参）。
 *
 * <p>跨域调用方（purchase/sales Processor）与库管员手工创建均通过本请求构造移动单。
 *
 * <p>推进策略（对齐 {@code docs/design/inventory/cross-domain.md}）：
 * <ul>
 *   <li>{@code relatedBillType} 非空（业务单据联动）→ 自动 DRAFT→CONFIRMED→DONE 一次推进。</li>
 *   <li>{@code relatedBillType} 为空（独立创建）→ 停在 CONFIRMED，待库管员二次确认执行 DONE。</li>
 * </ul>
 *
 * <p>幂等键：{@code (relatedBillType, relatedBillCode)}——同源单重复触发反查已有移动单直接返回。
 */
public class StockMoveRequest {
    private String moveType;
    private Long orgId;
    private LocalDate businessDate;
    private Long sourceWarehouseId;
    private Long sourceLocationId;
    private Long destWarehouseId;
    private Long destLocationId;
    private String relatedBillType;
    private String relatedBillCode;
    private Long acctSchemaId;
    private Long currencyId;
    private String code;
    private String remark;
    private Long originMoveId;
    private Long originReturnedMoveId;
    private List<StockMoveLineRequest> lines;

    public String getMoveType() {
        return moveType;
    }

    public void setMoveType(String moveType) {
        this.moveType = moveType;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public Long getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(Long sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public Long getDestWarehouseId() {
        return destWarehouseId;
    }

    public void setDestWarehouseId(Long destWarehouseId) {
        this.destWarehouseId = destWarehouseId;
    }

    public Long getDestLocationId() {
        return destLocationId;
    }

    public void setDestLocationId(Long destLocationId) {
        this.destLocationId = destLocationId;
    }

    public String getRelatedBillType() {
        return relatedBillType;
    }

    public void setRelatedBillType(String relatedBillType) {
        this.relatedBillType = relatedBillType;
    }

    public String getRelatedBillCode() {
        return relatedBillCode;
    }

    public void setRelatedBillCode(String relatedBillCode) {
        this.relatedBillCode = relatedBillCode;
    }

    public Long getAcctSchemaId() {
        return acctSchemaId;
    }

    public void setAcctSchemaId(Long acctSchemaId) {
        this.acctSchemaId = acctSchemaId;
    }

    public Long getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Long currencyId) {
        this.currencyId = currencyId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getOriginMoveId() {
        return originMoveId;
    }

    public void setOriginMoveId(Long originMoveId) {
        this.originMoveId = originMoveId;
    }

    public Long getOriginReturnedMoveId() {
        return originReturnedMoveId;
    }

    public void setOriginReturnedMoveId(Long originReturnedMoveId) {
        this.originReturnedMoveId = originReturnedMoveId;
    }

    public List<StockMoveLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<StockMoveLineRequest> lines) {
        this.lines = lines;
    }

    public boolean isBusinessLinked() {
        return relatedBillType != null && !relatedBillType.isEmpty()
                && relatedBillCode != null && !relatedBillCode.isEmpty();
    }
}
