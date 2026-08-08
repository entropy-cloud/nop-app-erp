package app.erp.pur.biz;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 请购→订单转化的调用方补充字段 DTO（purchase 域内部，不进 api 模块）。
 *
 * <p>请购头/行无供应商/仓库/币种/价格（请购是数量型意向单），转化时由调用方提供：
 * <ul>
 *   <li>{@link #warehouseId}：订单收货仓库（写入订单头，单供应商/未提供 per-supplier 选项时生效）。</li>
 *   <li>{@link #currencyId}：订单币种（写入订单头，单供应商/未提供 per-supplier 选项时生效）。</li>
 *   <li>{@link #lineUnitPrices}：按请购行 lineNo 映射的不含税单价（VARCHAR 存储，对齐采购域金额约定）。</li>
 *   <li>{@link #lineTaxRates}：按请购行 lineNo 映射的税率（可选，VARCHAR）。</li>
 *   <li>{@link #supplierOptions}：per-supplier 头字段映射（RC-R1.10 多供应商拆分），key=供应商
 *       {@code suggestedSupplierId} 的字符串形式（BeanCopier 对 {@code Map<Long,...>} 键类型转换不完整，
 *       执行期决策：采用 {@code Map<String, SupplierConversionOption>} 保证 JSON 键（恒为字符串）与 Java 键一致），
 *       值为该供应商生成订单头的仓库/币种/到货期覆盖；未提供映射的供应商
 *       回退全局 {@code warehouseId}/{@code currencyId}，{@code deliveryDate} 置空。</li>
 * </ul>
 *
 * <p>{@code supplierId}/{@code businessDate}/{@code orgId} 取自请购行 {@code suggestedSupplierId}/请购头，不由调用方提供。
 */
public class ConvertToOrderRequest {

    private Long warehouseId;
    private Long currencyId;
    private Map<Integer, String> lineUnitPrices = new LinkedHashMap<>();
    private Map<Integer, String> lineTaxRates = new LinkedHashMap<>();
    private Map<String, SupplierConversionOption> supplierOptions = new LinkedHashMap<>();

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

    public Map<String, SupplierConversionOption> getSupplierOptions() {
        return supplierOptions;
    }

    public void setSupplierOptions(Map<String, SupplierConversionOption> supplierOptions) {
        this.supplierOptions = supplierOptions == null ? new LinkedHashMap<>() : supplierOptions;
    }
}
