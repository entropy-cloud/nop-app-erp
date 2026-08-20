package app.erp.sal.service.support;

import app.erp.ct.biz.IErpCtVolumeDiscountBiz;
import app.erp.ct.dao.dto.DiscountResult;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.core.context.IServiceContext;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 合同量折扣销售订单行应用器（RC-R1.79 / P1-RC-078，UC-CT-08 A，
 * {@code docs/design/contract/volume-discount.md §折扣应用逻辑}）。
 *
 * <p>引用合同行的销售订单行按实际数量匹配 {@code ErpCtVolumeDiscount} 区间带，计算折后价写行金额：
 * <ul>
 *   <li>折扣基数（D2 裁决）：合同行单价 {@code ctContractLine.unitPrice}（缺失回退订单行现价）——
 *       save/approve 重复解析均从稳定基数出发，避免折后价二次折扣。</li>
 *   <li>命中区间带：行 unitPrice ← 折后单价（L1「计算折后价」字面）；既有折扣列承载可见性——
 *       discountRate ← 隐含折扣率、discountAmount ← 基数口径节省额（可见性载体，不参与净额减扣——
 *       unitPrice 已是折后价）；pricingSource ← {@code CT_VOLUME_DISCOUNT}
 *       （显式合同行引用优先于促销/目录价）；amount ← 折后行金额，税额按销售价税分离公式随动
 *       （税额 = 折后金额 / (1 + 税率) × 税率，对齐 recomputeLineAmount 既有约定）。</li>
 *   <li>无命中：回退原价——订单行保持自身录入价，零改写。</li>
 *   <li>门控（D3 裁决）：{@code erp-sal.ct-discount-enabled} 默认 true；关闭时引用字段仅存储不应用。</li>
 *   <li>跨域容错：contract 模块缺失时 {@code @Nullable} 注入跳过（matrix §2.4 披露范式）。</li>
 * </ul>
 */
public class ErpSalCtDiscountApplier {

    @Inject
    @Nullable
    IErpCtVolumeDiscountBiz volumeDiscountBiz;

    public void setVolumeDiscountBiz(IErpCtVolumeDiscountBiz volumeDiscountBiz) {
        this.volumeDiscountBiz = volumeDiscountBiz;
    }

    /** 门控开关（protected 供下游派生覆盖部署语义）。 */
    protected boolean enabled() {
        return AppConfig.var(ErpSalConstants.CONFIG_CT_DISCOUNT_ENABLED, Boolean.TRUE);
    }

    /**
     * 对单行应用量折扣。返回 true 表示发生改写（调用方据此重算头合计）。
     */
    public boolean applyToLine(ErpSalOrderLine line, IServiceContext context) {
        if (line == null || line.getCtContractLineId() == null || volumeDiscountBiz == null || !enabled()) {
            return false;
        }
        BigDecimal qty = line.getQuantity();
        if (qty == null || qty.signum() <= 0) {
            return false;
        }
        BigDecimal base = resolveBasePrice(line);
        if (base == null || base.signum() <= 0) {
            return false;
        }
        DiscountResult result = volumeDiscountBiz.resolveDiscount(
                line.getCtContractLineId(), qty, base, context);
        if (result == null || !result.isBandMatched() || result.getDiscountedUnitPrice() == null) {
            return false;
        }
        BigDecimal discounted = result.getDiscountedUnitPrice();
        line.setUnitPrice(discounted);
        if (base.compareTo(discounted) > 0) {
            line.setDiscountRate(base.subtract(discounted)
                    .divide(base, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP));
            line.setDiscountAmount(base.subtract(discounted).multiply(qty).setScale(4, RoundingMode.HALF_UP));
        } else {
            line.setDiscountRate(BigDecimal.ZERO);
            line.setDiscountAmount(BigDecimal.ZERO);
        }
        line.setPricingSource(ErpSalConstants.PRICING_SOURCE_CT_VOLUME_DISCOUNT);
        // 行金额 = 折后单价 × 数量（discountAmount 为基数口径节省额的可见性载体，不参与净额减扣——
        // unitPrice 已是折后价，再减折扣金额会双重扣减）
        line.setAmount(result.getLineAmount());
        recomputeTax(line);
        return true;
    }

    /** 折扣基数 = 合同行单价（ORM to-one 关系 getter），缺失回退订单行现价。 */
    protected BigDecimal resolveBasePrice(ErpSalOrderLine line) {
        if (line.getCtContractLine() != null && line.getCtContractLine().getUnitPrice() != null
                && line.getCtContractLine().getUnitPrice().signum() > 0) {
            return line.getCtContractLine().getUnitPrice();
        }
        return line.getUnitPrice();
    }

    /** 价税分离（对齐 ErpSalOrderBizModel.recomputeLineAmount：税额 = 折后金额 / (1 + 税率) × 税率）。 */
    protected void recomputeTax(ErpSalOrderLine line) {
        BigDecimal net = line.getAmount() == null ? BigDecimal.ZERO : line.getAmount();
        BigDecimal taxRate = line.getTaxRate() == null ? BigDecimal.ZERO : line.getTaxRate();
        BigDecimal taxAmount;
        if (taxRate.signum() == 0) {
            taxAmount = BigDecimal.ZERO;
        } else {
            BigDecimal rate = taxRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            taxAmount = net.multiply(rate).divide(BigDecimal.ONE.add(rate), 4, RoundingMode.HALF_UP);
        }
        line.setTaxAmount(taxAmount);
        line.setAmountWithTax(net.add(taxAmount).setScale(4, RoundingMode.HALF_UP));
    }
}
