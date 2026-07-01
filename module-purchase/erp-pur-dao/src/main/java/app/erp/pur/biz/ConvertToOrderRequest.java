package app.erp.pur.biz;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 请购→订单转化的调用方补充字段 DTO（purchase 域内部，不进 api 模块）。
 *
 * <p>请购头/行无供应商/仓库/币种/价格（请购是数量型意向单），转化时由调用方提供：
 * <ul>
 *   <li>{@link #warehouseId}：订单收货仓库（写入订单头）。</li>
 *   <li>{@link #currencyId}：订单币种（写入订单头）。</li>
 *   <li>{@link #lineUnitPrices}：按请购行 lineNo 映射的不含税单价（VARCHAR 存储，对齐采购域金额约定）。</li>
 *   <li>{@link #lineTaxRates}：按请购行 lineNo 映射的税率（可选，VARCHAR）。</li>
 * </ul>
 *
 * <p>{@code supplierId}/{@code businessDate}/{@code orgId} 取自请购行 {@code suggestedSupplierId}/请购头，不由调用方提供。
 */
public class ConvertToOrderRequest {

    private Long warehouseId;
    private Long currencyId;
    private Map<Integer, String> lineUnitPrices = new LinkedHashMap<>();
    private Map<Integer, String> lineTaxRates = new LinkedHashMap<>();

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

    public Map<Integer, String> getLineUnitPrices() {
        return lineUnitPrices;
    }

    public void setLineUnitPrices(Map<Integer, String> lineUnitPrices) {
        this.lineUnitPrices = lineUnitPrices == null ? new LinkedHashMap<>() : lineUnitPrices;
    }

    public Map<Integer, String> getLineTaxRates() {
        return lineTaxRates;
    }

    public void setLineTaxRates(Map<Integer, String> lineTaxRates) {
        this.lineTaxRates = lineTaxRates == null ? new LinkedHashMap<>() : lineTaxRates;
    }
}
