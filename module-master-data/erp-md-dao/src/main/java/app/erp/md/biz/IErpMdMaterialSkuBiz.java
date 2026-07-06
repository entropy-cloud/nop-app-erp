
package app.erp.md.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.md.dao.dto.PriceValidationResult;
import app.erp.md.dao.entity.ErpMdMaterialSku;

/**
 * 物料 SKU 业务服务（UC-MD-01/03/04/05/06，{@code docs/design/master-data/use-cases.md}）。
 *
 * <p>读解析方法（find/resolve/validate）以 {@link BizQuery} 暴露——供前端预检与下游域 I*Biz 调用；
 * 不变更 SKU 状态的写操作（停用/删除守卫）以 {@link io.nop.api.core.annotations.biz.BizMutation} 暴露（见 Phase 3）。
 */
public interface IErpMdMaterialSkuBiz extends ICrudBiz<ErpMdMaterialSku>{

    /**
     * UC-MD-01：按条码反查 SKU（不存在返回 null，调用方经关系取 material）。
     */
    @BizQuery
    ErpMdMaterialSku findSkuByBarcode(@Name("barcode") String barcode, IServiceContext context);

    /**
     * UC-MD-05：查询物料的默认 SKU（无默认返回 null）。
     */
    @BizQuery
    ErpMdMaterialSku findDefaultSku(@Name("materialId") Long materialId, IServiceContext context);

    /**
     * UC-MD-05：单据未指定 SKU 时的兜底解析。unitId 非空按物料+单位匹配，否则取默认 SKU；
     * 无默认且配置 sku-default-required=true 抛 {@link NopException}。
     */
    @BizQuery
    ErpMdMaterialSku resolveSku(@Name("materialId") Long materialId,
                                @Optional @Name("unitId") Long unitId,
                                IServiceContext context);

    /**
     * UC-MD-03：价格三级优先级解析（手工价 > 价格表层 > SKU 默认档）。
     *
     * @param skuId       SKU 主键
     * @param partnerId   供应商/客户 ID（价格表层匹配；可空）
     * @param billType    单据类型（PURCHASE/WHOLESALE/RETAIL/DEFAULT，选默认档；可空走 DEFAULT）
     * @param manualPrice 手工输入价（非空直接返回，最高优先级）
     * @return 解析后的单价（永不返回 null：手工价→价格表→SKU 默认档→ZERO 兜底）
     */
    @BizQuery
    java.math.BigDecimal resolvePrice(@Name("skuId") Long skuId,
                                      @Optional @Name("partnerId") Long partnerId,
                                      @Optional @Name("billType") String billType,
                                      @Optional @Name("manualPrice") java.math.BigDecimal manualPrice,
                                      IServiceContext context);

    /**
     * UC-MD-04：最低价校验预检（前端/下游域调用）。按 MaterialCategory.priceValidationLevel 分派：
     * HARD 低于底线抛 {@link NopException}；WARN 返回 warning=true 放行；OFF 直接通过。
     */
    @BizQuery
    PriceValidationResult validatePrice(@Name("skuId") Long skuId,
                                        @Name("finalPrice") java.math.BigDecimal finalPrice,
                                        @Optional @Name("materialCategoryId") Long materialCategoryId,
                                        IServiceContext context);

    /**
     * UC-MD-06：停用/删除 SKU 前的守卫校验。命中约束抛 {@link NopException}（默认 SKU 唯一性 + 引用检查）。
     * 实际停用/删除动作仍走 CRUD save/delete，本方法仅做前置预检。
     */
    @BizQuery
    boolean validateSkuDeactivation(@Name("skuId") Long skuId, IServiceContext context);
}
