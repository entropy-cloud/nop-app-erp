package app.erp.pur.biz;

import java.time.LocalDate;

/**
 * 请购→订单转化 per-supplier 头字段选项（RC-R1.10 多供应商拆分）。
 *
 * <p>按 {@code supplierId} 挂在 {@link ConvertToOrderRequest#getSupplierOptions()} 上，
 * 为该供应商生成的订单头覆盖全局 {@code warehouseId}/{@code currencyId} 并指定到货期。
 * 字段为 null 时回退全局字段（兼容单供应商现状）；{@code deliveryDate} 无全局回退（订单头可空）。
 */
public class SupplierConversionOption {

    private Long warehouseId;
    private Long currencyId;
    private LocalDate deliveryDate;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Long currencyId) {
        this.currencyId = currencyId;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }
}
