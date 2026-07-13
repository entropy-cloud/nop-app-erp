
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import app.erp.md.biz.IErpMdMaterialSkuBiz;
import app.erp.md.dao.dto.PriceValidationResult;
import app.erp.md.dao.dto.ResolvedPrice;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialCategory;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.service.ErpMdConstants;
import app.erp.md.service.ErpMdErrors;
import app.erp.md.spi.IErpMdCustomerPriceResolver;
import app.erp.md.spi.IErpMdSkuReferenceChecker;
import app.erp.md.spi.IErpMdSupplierPriceResolver;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 物料 SKU 业务服务（UC-MD-01/03/04/05/06，{@code docs/design/master-data/use-cases.md}）。
 *
 * <p>读解析方法以 {@link BizQuery} 暴露：
 * <ul>
 *   <li>UC-MD-01 {@link #findSkuByBarcode}：条码反查 SKU。</li>
 *   <li>UC-MD-05 {@link #findDefaultSku} / {@link #resolveSku}：默认 SKU 兜底。</li>
 *   <li>UC-MD-03 {@link #resolvePrice}：价格三级优先级（手工价 &gt; 价格表层 SPI &gt; SKU 默认档）。</li>
 *   <li>UC-MD-04 {@link #validatePrice}：最低价校验（OFF/WARN/HARD 分派）。</li>
 *   <li>UC-MD-06 {@link #validateSkuDeactivation}：停用/删除守卫（默认 SKU 唯一性 + 引用 SPI）。</li>
 * </ul>
 *
 * <p>条码全局唯一经 {@link #defaultPrepareSave}/{@link #defaultPrepareUpdate} 应用层查重
 * （{@link ErpMdConstants#CONFIG_SKU_BARCODE_UNIQUE} 开时；DB 唯一索引归 Deferred G1）。
 *
 * <p>跨域访问（价格表/引用检查）经 SPI（{@code IErpMdSupplierPriceResolver} / {@code IErpMdSkuReferenceChecker}），
 * 避免基础域反向依赖下游域构成依赖环。
 */
@BizModel("ErpMdMaterialSku")
public class ErpMdMaterialSkuBizModel extends CrudBizModel<ErpMdMaterialSku> implements IErpMdMaterialSkuBiz {

    @Inject
    @Nullable
    protected IErpMdSupplierPriceResolver supplierPriceResolver;

    @Inject
    @Nullable
    protected IErpMdCustomerPriceResolver customerPriceResolver;

    @Inject
    @Nullable
    protected IErpMdSkuReferenceChecker skuReferenceChecker;

    public ErpMdMaterialSkuBizModel() {
        setEntityName(ErpMdMaterialSku.class.getName());
    }

    // ============ UC-MD-01 扫码开单 ============

    @Override
    @BizQuery
    public ErpMdMaterialSku findSkuByBarcode(@Name("barcode") String barcode, IServiceContext context) {
        if (barcode == null || barcode.isEmpty()) {
            return null;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("barcode", barcode));
        return findFirst(query, null, context);
    }

    // ============ UC-MD-05 默认 SKU 兜底 ============

    @Override
    @BizQuery
    public ErpMdMaterialSku findDefaultSku(@Name("materialId") Long materialId, IServiceContext context) {
        if (materialId == null || !isMaterialActive(materialId)) {
            return null;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("materialId", materialId));
        query.addFilter(eq("isDefault", Boolean.TRUE));
        return findFirst(query, null, context);
    }

    @Override
    @BizQuery
    public ErpMdMaterialSku resolveSku(@Name("materialId") Long materialId,
                                       @Optional @Name("unitId") Long unitId,
                                       IServiceContext context) {
        if (materialId == null || !isMaterialActive(materialId)) {
            return null;
        }
        if (unitId != null) {
            QueryBean query = new QueryBean();
            query.addFilter(eq("materialId", materialId));
            query.addFilter(eq("uoMId", unitId));
            ErpMdMaterialSku matched = findFirst(query, null, context);
            if (matched != null) {
                return matched;
            }
        }
        ErpMdMaterialSku def = findDefaultSku(materialId, context);
        if (def == null && isSkuDefaultRequired()) {
            throw new NopException(ErpMdErrors.ERR_SKU_DEFAULT_REQUIRED)
                    .param(ErpMdErrors.ARG_MATERIAL_ID, materialId);
        }
        return def;
    }

    // ============ UC-MD-03 价格优先级解析 + UC-MD-04 最低价校验（Phase 2 实现） ============

    @Override
    @BizQuery
    public BigDecimal resolvePrice(@Name("skuId") Long skuId,
                                   @Optional @Name("partnerId") Long partnerId,
                                   @Optional @Name("billType") String billType,
                                   @Optional @Name("manualPrice") BigDecimal manualPrice,
                                   IServiceContext context) {
        if (manualPrice != null) {
            return manualPrice;
        }
        ErpMdMaterialSku sku = requireSku(skuId, context);
        // 客户价格清单层（UC-SAL-11）：优先于供应商价格表和 SKU 默认档
        if (customerPriceResolver != null && partnerId != null) {
            ResolvedPrice customerPrice = customerPriceResolver.resolveCustomerPrice(
                    sku, partnerId, null, null, context);
            if (customerPrice != null && customerPrice.getUnitPrice() != null) {
                return customerPrice.getUnitPrice();
            }
        }
        if (supplierPriceResolver != null && partnerId != null) {
            BigDecimal priceListPrice = supplierPriceResolver.resolveSupplierPrice(sku, partnerId);
            if (priceListPrice != null) {
                return priceListPrice;
            }
        }
        return pickDefaultTierPrice(sku, billType);
    }

    @Override
    @BizQuery
    public ResolvedPrice resolvePriceWithSource(@Name("skuId") Long skuId,
                                                 @Optional @Name("partnerId") Long partnerId,
                                                 @Optional @Name("billType") String billType,
                                                 @Optional @Name("quantity") BigDecimal quantity,
                                                 @Optional @Name("currencyId") Long currencyId,
                                                 IServiceContext context) {
        ErpMdMaterialSku sku = requireSku(skuId, context);
        // 客户价格清单层
        if (customerPriceResolver != null && partnerId != null) {
            ResolvedPrice customerPrice = customerPriceResolver.resolveCustomerPrice(
                    sku, partnerId, quantity, currencyId, context);
            if (customerPrice != null && customerPrice.getUnitPrice() != null) {
                return customerPrice;
            }
        }
        // SKU 默认档兜底
        BigDecimal tier = pickDefaultTierPrice(sku, billType);
        return new ResolvedPrice(tier, ErpMdConstants.PRICING_SOURCE_SKU_DEFAULT, null, null);
    }

    @Override
    @BizQuery
    public PriceValidationResult validatePrice(@Name("skuId") Long skuId,
                                               @Name("finalPrice") BigDecimal finalPrice,
                                               @Optional @Name("materialCategoryId") Long materialCategoryId,
                                               IServiceContext context) {
        String level = resolvePriceValidationLevel(materialCategoryId, context);
        if (ErpMdConstants.PRICE_VALIDATION_OFF.equals(level)) {
            return new PriceValidationResult(true, false, null, level);
        }
        ErpMdMaterialSku sku = requireSku(skuId, context);
        BigDecimal minPrice = deriveMinPrice(sku);
        BigDecimal finalVal = finalPrice == null ? BigDecimal.ZERO : finalPrice;
        boolean below = minPrice != null && finalVal.compareTo(minPrice) < 0;
        if (!below) {
            return new PriceValidationResult(true, false, minPrice, level);
        }
        if (ErpMdConstants.PRICE_VALIDATION_HARD.equals(level)) {
            throw new NopException(ErpMdErrors.ERR_PRICE_BELOW_MIN)
                    .param(ErpMdErrors.ARG_FINAL_PRICE, finalVal)
                    .param(ErpMdErrors.ARG_MIN_PRICE, minPrice)
                    .param(ErpMdErrors.ARG_PRICE_VALIDATION_LEVEL, level);
        }
        // WARN：放行带警告（其他未知级别也按 WARN 宽松处理）
        return new PriceValidationResult(true, true, minPrice, level);
    }

    // ============ UC-MD-06 SKU 状态约束（Phase 3 实现完整守卫；此处先骨架返回 true） ============

    @Override
    @BizQuery
    public boolean validateSkuDeactivation(@Name("skuId") Long skuId, IServiceContext context) {
        ErpMdMaterialSku sku = requireSku(skuId, context);
        // 守卫 1：默认 SKU 唯一性——是默认且无其他可用 SKU 则拒绝
        if (Boolean.TRUE.equals(sku.getIsDefault())) {
            if (!hasOtherActiveSku(sku.getMaterialId(), skuId, context)) {
                throw new NopException(ErpMdErrors.ERR_CANNOT_DEACTIVATE_DEFAULT_SKU)
                        .param(ErpMdErrors.ARG_SKU_ID, skuId)
                        .param(ErpMdErrors.ARG_MATERIAL_ID, sku.getMaterialId());
            }
        }
        // 守卫 2：跨域引用检查经 SPI（域内引用校验也由 checker 承载；无 checker 时仅域内默认 SKU 守卫生效）
        if (skuReferenceChecker != null && skuReferenceChecker.isReferencedByBill(sku)) {
            throw new NopException(ErpMdErrors.ERR_SKU_REFERENCED_BY_BILL)
                    .param(ErpMdErrors.ARG_SKU_ID, skuId);
        }
        return true;
    }

    // ============ 条码唯一性应用层校验（UC-MD-01 G1 应用层兜底） ============

    @Override
    protected void defaultPrepareSave(EntityData<ErpMdMaterialSku> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        enforceBarcodeUnique(entityData.getEntity(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpMdMaterialSku> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        enforceBarcodeUnique(entityData.getEntity(), context);
    }

    /**
     * UC-MD-06：删除前引用校验。CRUD delete 经本钩子触发 {@link #validateSkuDeactivation} 守卫
     * （默认 SKU 唯一性 + 跨域引用 SPI），命中约束拒绝删除。
     */
    @Override
    protected void defaultPrepareDelete(ErpMdMaterialSku entity, IServiceContext context) {
        super.defaultPrepareDelete(entity, context);
        validateSkuDeactivation(entity.getId(), context);
    }

    /**
     * 应用层 barcode 查重：配置 sku-barcode-unique=true 时，barcode 非空且已被其他 SKU 占用 → 拒绝。
     * TOCTOU 窗口归 Deferred DB 唯一索引补强（G1）。
     */
    protected void enforceBarcodeUnique(ErpMdMaterialSku entity, IServiceContext context) {
        if (!isBarcodeUniqueEnabled()) {
            return;
        }
        String barcode = entity.getBarcode();
        if (barcode == null || barcode.isEmpty()) {
            return;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("barcode", barcode));
        for (ErpMdMaterialSku existing : findList(query, null, context)) {
            if (entity.getId() == null || !Objects.equals(entity.getId(), existing.getId())) {
                throw new NopException(ErpMdErrors.ERR_SKU_BARCODE_DUPLICATE)
                        .param(ErpMdErrors.ARG_BARCODE, barcode);
            }
        }
    }

    // ============ 内部步骤 ============

    protected ErpMdMaterialSku requireSku(Long skuId, IServiceContext context) {
        if (skuId == null) {
            throw new NopException(ErpMdErrors.ERR_SKU_DEFAULT_REQUIRED)
                    .param(ErpMdErrors.ARG_SKU_ID, skuId);
        }
        ErpMdMaterialSku sku = get(String.valueOf(skuId), true, context);
        if (sku == null) {
            throw new NopException(ErpMdErrors.ERR_SKU_DEFAULT_REQUIRED)
                    .param(ErpMdErrors.ARG_SKU_ID, skuId);
        }
        return sku;
    }

    /**
     * 是否存在其他可用（非当前）SKU。当前实体无 status 列（G2），故「可用」=同物料+id 不同。
     */
    @SuppressWarnings("unchecked")
    protected boolean hasOtherActiveSku(Long materialId, Long excludeSkuId, IServiceContext context) {
        if (materialId == null) {
            return false;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("materialId", materialId));
        List<ErpMdMaterialSku> list = findList(query, null, context);
        for (ErpMdMaterialSku sku : list) {
            if (!Objects.equals(sku.getId(), excludeSkuId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按 billType 选 SKU 默认档价格。PURCHASE→purchasePrice, WHOLESALE→wholesalePrice,
     * RETAIL→retailPrice, 其他/空→salePrice。
     */
    protected BigDecimal pickDefaultTierPrice(ErpMdMaterialSku sku, String billType) {
        if (sku == null) {
            return BigDecimal.ZERO;
        }
        if (ErpMdConstants.BILL_TYPE_PURCHASE.equals(billType)) {
            return nullSafe(sku.getPurchasePrice());
        }
        if (ErpMdConstants.BILL_TYPE_WHOLESALE.equals(billType)) {
            return nullSafe(sku.getWholesalePrice());
        }
        if (ErpMdConstants.BILL_TYPE_RETAIL.equals(billType)) {
            return nullSafe(sku.getRetailPrice());
        }
        return nullSafe(sku.getSalePrice());
    }

    /**
     * UC-MD-04 底线来源（Explore 裁定选项 b：派生底线）。
     * G3 minPrice 列缺失——取四档价中的最小正值作为派生底线；全空返回 null（不限制）。
     */
    protected BigDecimal deriveMinPrice(ErpMdMaterialSku sku) {
        if (sku == null) {
            return null;
        }
        BigDecimal min = null;
        for (BigDecimal p : new BigDecimal[]{
                sku.getPurchasePrice(), sku.getWholesalePrice(),
                sku.getRetailPrice(), sku.getSalePrice()}) {
            if (p != null && p.signum() > 0) {
                if (min == null || p.compareTo(min) < 0) {
                    min = p;
                }
            }
        }
        return min;
    }

    /**
     * UC-MD-04 读 MaterialCategory.priceValidationLevel。
     * G5：字典值为 string OFF/WARN/HARD；列默认值 "20" 为孤儿不参与逻辑（非字典值统一按 WARN 宽松处理）。
     */
    protected String resolvePriceValidationLevel(Long materialCategoryId, IServiceContext context) {
        if (materialCategoryId == null) {
            return ErpMdConstants.PRICE_VALIDATION_WARN;
        }
        IEntityDao<ErpMdMaterialCategory> categoryDao = daoProvider().daoFor(ErpMdMaterialCategory.class);
        ErpMdMaterialCategory category = categoryDao.getEntityById(materialCategoryId);
        if (category == null) {
            return ErpMdConstants.PRICE_VALIDATION_WARN;
        }
        String level = category.getPriceValidationLevel();
        if (ErpMdConstants.PRICE_VALIDATION_OFF.equals(level)
                || ErpMdConstants.PRICE_VALIDATION_WARN.equals(level)
                || ErpMdConstants.PRICE_VALIDATION_HARD.equals(level)) {
            return level;
        }
        // 非字典合法值（含 G5 孤儿 "20"）→ WARN 宽松
        return ErpMdConstants.PRICE_VALIDATION_WARN;
    }

    /**
     * UC-MD-06 物料级 status 过滤：物料停用（status != ACTIVE）时其 SKU 不可被新单引用。
     * G2：SKU 无独立 status 列，联动经物料级 status 承载（Phase 3 Decision 选 (b)）。
     */
    protected boolean isMaterialActive(Long materialId) {
        if (materialId == null) {
            return false;
        }
        ErpMdMaterial material = daoProvider().daoFor(ErpMdMaterial.class).getEntityById(materialId);
        if (material == null) {
            return false;
        }
        String status = material.getStatus();
        return status == null || ErpMdConstants.ACTIVE_STATUS_ACTIVE.equals(status);
    }

    protected boolean isSkuDefaultRequired() {
        return AppConfig.var(ErpMdConstants.CONFIG_SKU_DEFAULT_REQUIRED, Boolean.TRUE);
    }

    protected boolean isBarcodeUniqueEnabled() {
        return AppConfig.var(ErpMdConstants.CONFIG_SKU_BARCODE_UNIQUE, Boolean.TRUE);
    }

    protected boolean isUomConversionStrict() {
        return AppConfig.var(ErpMdConstants.CONFIG_UOM_CONVERSION_STRICT, Boolean.TRUE);
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpMdMaterialSku.class)
    public List<String> materialName(@ContextSource List<ErpMdMaterialSku> rows) {
        orm().batchLoadProps(rows, Collections.singleton("material"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdMaterialSku row : rows) {
            result.add(row.orm_attached() && row.getMaterial() != null ? row.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMdMaterialSku.class)
    public List<String> uomName(@ContextSource List<ErpMdMaterialSku> rows) {
        orm().batchLoadProps(rows, Collections.singleton("uoM"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdMaterialSku row : rows) {
            result.add(row.orm_attached() && row.getUoM() != null ? row.getUoM().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMdMaterialSku.class)
    public List<String> taxRateName(@ContextSource List<ErpMdMaterialSku> rows) {
        orm().batchLoadProps(rows, Collections.singleton("taxRate"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpMdMaterialSku row : rows) {
            result.add(row.orm_attached() && row.getTaxRate() != null ? row.getTaxRate().getName() : null);
        }
        return result;
    }

}
