package app.erp.inv.biz;

import java.math.BigDecimal;

/**
 * 预留行请求（{@link ReservationCreateRequest#getLines()} 元素）。
 *
 * <p>行维度（materialId/warehouseId 必填；skuId/locationId/batchNo 可选），实际预留量
 * = min({@code requestedQuantity}, 库存可用量) 由库存侧计算并写回行 {@code reservedQuantity}。
 */
public class ReservationLineRequest {
    private String materialId;
    private String skuId;
    private String warehouseId;
    private String locationId;
    private String batchNo;
    private BigDecimal requestedQuantity;
    private String uomId;
    private String sourceLineCode;

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
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

    public String getUomId() {
        return uomId;
    }

    public void setUomId(String uomId) {
        this.uomId = uomId;
    }

    public String getSourceLineCode() {
        return sourceLineCode;
    }

    public void setSourceLineCode(String sourceLineCode) {
        this.sourceLineCode = sourceLineCode;
    }
}
