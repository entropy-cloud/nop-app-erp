package app.erp.inv.biz;

import java.math.BigDecimal;

/**
 * 预留消耗行（{@link ReservationConsumeRequest#getLines()} 元素）。
 *
 * <p>按 materialId 匹配预留行（warehouseId 提供时参与匹配过滤）；
 * 消耗量 = min({@code quantity}, 该物料预留未消耗量)。
 */
public class ReservationConsumeLine {
    private Long materialId;
    private Long warehouseId;
    private Long locationId;
    private String batchNo;
    private BigDecimal quantity;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
