package app.erp.inv.biz;

import java.math.BigDecimal;

/**
 * 预留行请求（{@link ReservationCreateRequest#getLines()} 元素）。
 *
 * <p>行维度（materialId/warehouseId 必填；skuId/locationId/batchNo 可选），实际预留量
 * = min({@code requestedQuantity}, 库存可用量) 由库存侧计算并写回行 {@code reservedQuantity}。
 */
public class ReservationLineRequest {
    private Long materialId;
    private Long skuId;
    private Long warehouseId;
    private Long locationId;
    private String batchNo;
    private BigDecimal requestedQuantity;
    private Long uomId;
    private String sourceLineCode;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
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

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(BigDecimal requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Long getUomId() {
        return uomId;
    }

    public void setUomId(Long uomId) {
        this.uomId = uomId;
    }

    public String getSourceLineCode() {
        return sourceLineCode;
    }

    public void setSourceLineCode(String sourceLineCode) {
        this.sourceLineCode = sourceLineCode;
    }
}
