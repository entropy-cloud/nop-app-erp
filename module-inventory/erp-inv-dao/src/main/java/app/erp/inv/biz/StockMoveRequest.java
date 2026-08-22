package app.erp.inv.biz;

import java.time.LocalDate;
import java.util.List;

/**
 * 库存移动单生成请求（{@link IErpInvStockMoveBiz#generateMove} 入参）。
 *
 * <p>跨域调用方（purchase/sales Processor）与库管员手工创建均通过本请求构造移动单。
 *
 * <p>推进策略（对齐 {@code docs/design/inventory/cross-domain.md}；判别键 = {@link #isBusinessLinked()}——两字段均非空）：
 * <ul>
 *   <li>{@code isBusinessLinked()} 为 true（业务单据联动，relatedBillType + relatedBillCode 均非空）→ 自动 DRAFT→CONFIRMED→DONE 一次推进。</li>
 *   <li>{@code isBusinessLinked()} 为 false（独立创建，含带类型键但 code 为空的判别载体，如盘点差异移动单
 *       {@code relatedBillType=ERP_INV_STOCK_TAKE + relatedBillCode=null}，RC-R1.56）→ 停在 CONFIRMED，待库管员二次确认执行 DONE。</li>
 * </ul>
 *
 * <p>幂等键：{@code (relatedBillType, relatedBillCode)}——同源单重复触发反查已有移动单直接返回。
 */
public class StockMoveRequest {
    private String moveType;
    private String orgId;
    private LocalDate businessDate;
    private String sourceWarehouseId;
    private String sourceLocationId;
    private String destWarehouseId;
    private String destLocationId;
    private String relatedBillType;
    private String relatedBillCode;
    private String acctSchemaId;
    private String currencyId;
    private String code;
    private String remark;
    private String originMoveId;
    private String originReturnedMoveId;
    private List<StockMoveLineRequest> lines;

    public String getMoveType() {
        return moveType;
    }

    public void setMoveType(String moveType) {
        this.moveType = moveType;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public String getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(String sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    public String getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(String sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public String getDestWarehouseId() {
        return destWarehouseId;
    }

    public void setDestWarehouseId(String destWarehouseId) {
        this.destWarehouseId = destWarehouseId;
    }

    public String getDestLocationId() {
        return destLocationId;
    }

    public void setDestLocationId(String destLocationId) {
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

    public String getAcctSchemaId() {
        return acctSchemaId;
    }

    public void setAcctSchemaId(String acctSchemaId) {
        this.acctSchemaId = acctSchemaId;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(String currencyId) {
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

    public String getOriginMoveId() {
        return originMoveId;
    }

    public void setOriginMoveId(String originMoveId) {
        this.originMoveId = originMoveId;
    }

    public String getOriginReturnedMoveId() {
        return originReturnedMoveId;
    }

    public void setOriginReturnedMoveId(String originReturnedMoveId) {
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
