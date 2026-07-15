
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import app.erp.md.biz.IErpMdUoMConversionBiz;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdUoMConversion;
import app.erp.md.service.ErpMdConstants;
import app.erp.md.service.ErpMdErrors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;

/**
 * 单位换算业务服务（UC-MD-02，{@code docs/design/master-data/use-cases.md}）。
 *
 * <p>换算引擎优先级（Plan Phase 1 Decision 裁定）：
 * <ol>
 *   <li>物料级 ErpMdUoMConversion（materialId 非空，fromUoMId→toUoMId 精确匹配）；</li>
 *   <li>通用 ErpMdUoMConversion（materialId null）；</li>
 *   <li>strict=false 时回退 SKU.conversionRate；strict=true 抛 {@link NopException}。</li>
 * </ol>
 * 同单位换算（fromUoMId==toUoMId）直接返回原值。{@link BigDecimal} {@link RoundingMode#HALF_UP} scale=4。
 */
@BizModel("ErpMdUoMConversion")
public class ErpMdUoMConversionBizModel extends CrudBizModel<ErpMdUoMConversion> implements IErpMdUoMConversionBiz {

    public ErpMdUoMConversionBizModel() {
        setEntityName(ErpMdUoMConversion.class.getName());
    }

    @Override
    @BizQuery
    public BigDecimal convertQty(@Name("materialId") Long materialId,
                                 @Name("qty") BigDecimal qty,
                                 @Name("fromUoMId") Long fromUoMId,
                                 @Name("toUoMId") Long toUoMId,
                                 IServiceContext context) {
        BigDecimal quantity = qty == null ? BigDecimal.ZERO : qty;
        if (fromUoMId == null || toUoMId == null) {
            return quantity.setScale(ErpMdConstants.UOM_CONVERSION_SCALE, RoundingMode.HALF_UP);
        }
        if (Objects.equals(fromUoMId, toUoMId)) {
            return quantity.setScale(ErpMdConstants.UOM_CONVERSION_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal rate = resolveConversionRate(materialId, fromUoMId, toUoMId, context);
        if (rate == null) {
            rate = fallbackSkuConversionRate(materialId, fromUoMId, context);
        }
        if (rate == null) {
            if (isUomConversionStrict()) {
                throw new NopException(ErpMdErrors.ERR_UOM_CONVERSION_NOT_FOUND)
                        .param(ErpMdErrors.ARG_MATERIAL_ID, materialId)
                        .param(ErpMdErrors.ARG_FROM_UOM_ID, fromUoMId)
                        .param(ErpMdErrors.ARG_TO_UOM_ID, toUoMId);
            }
            // 宽松模式无系数 → 返回原值（不强制失败）
            return quantity.setScale(ErpMdConstants.UOM_CONVERSION_SCALE, RoundingMode.HALF_UP);
        }
        return quantity.multiply(rate).setScale(ErpMdConstants.UOM_CONVERSION_SCALE, RoundingMode.HALF_UP);
    }

    // ============ 内部步骤 ============

    /**
     * 解析换算系数：物料级 → 通用。返回 null 表示两层均未命中。
     */
    protected BigDecimal resolveConversionRate(Long materialId, Long fromUoMId, Long toUoMId,
                                               IServiceContext context) {
        BigDecimal rate = findRate(materialId, fromUoMId, toUoMId, context);
        if (rate != null) {
            return rate;
        }
        // 通用层（materialId is null）
        return findRate(null, fromUoMId, toUoMId, context);
    }

    /**
     * 按精确条件（materialId, fromUoMId, toUoMId）查 ErpMdUoMConversion 取系数。
     * materialId=null 表示查通用层。
     */
    @SuppressWarnings("unchecked")
    protected BigDecimal findRate(Long materialId, Long fromUoMId, Long toUoMId, IServiceContext context) {
        IEntityDao<ErpMdUoMConversion> conversionDao = daoProvider().daoFor(ErpMdUoMConversion.class);
        QueryBean query = new QueryBean();
        if (materialId == null) {
            query.addFilter(isNull("materialId"));
        } else {
            query.addFilter(eq("materialId", materialId));
        }
        query.addFilter(eq("fromUoMId", fromUoMId));
        query.addFilter(eq("toUoMId", toUoMId));
        List<ErpMdUoMConversion> list = conversionDao.findAllByQuery(query);
        for (ErpMdUoMConversion conv : list) {
            if (conv.getConversionRate() != null) {
                return conv.getConversionRate();
            }
        }
        return null;
    }

    /**
     * strict=false 时的回退：取源 SKU.conversionRate（SKU 单位→物料基本单位的换算系数）。
     * 仅当能定位到 (materialId, fromUoMId) 的 SKU 时使用。
     */
    @SuppressWarnings("unchecked")
    protected BigDecimal fallbackSkuConversionRate(Long materialId, Long fromUoMId, IServiceContext context) {
        if (materialId == null) {
            return null;
        }
        IEntityDao<ErpMdMaterialSku> skuDao = daoProvider().daoFor(ErpMdMaterialSku.class);
        QueryBean query = new QueryBean();
        query.addFilter(eq("materialId", materialId));
        query.addFilter(eq("uoMId", fromUoMId));
        List<ErpMdMaterialSku> list = skuDao.findAllByQuery(query);
        for (ErpMdMaterialSku sku : list) {
            if (sku.getConversionRate() != null) {
                return sku.getConversionRate();
            }
        }
        return null;
    }

    protected boolean isUomConversionStrict() {
        return AppConfig.var(ErpMdConstants.CONFIG_UOM_CONVERSION_STRICT, Boolean.TRUE);
    }
}
