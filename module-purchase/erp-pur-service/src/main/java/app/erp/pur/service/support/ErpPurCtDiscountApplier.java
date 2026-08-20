package app.erp.pur.service.support;

import app.erp.ct.biz.IErpCtVolumeDiscountBiz;
import app.erp.ct.dao.dto.DiscountResult;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 合同量折扣订单行应用器（RC-R1.79 / P1-RC-078，UC-CT-08 A，
 * {@code docs/design/contract/volume-discount.md §折扣应用逻辑}）。
 *
 * <p>引用合同行的订单行按实际数量匹配 {@code ErpCtVolumeDiscount} 区间带，计算折后价写行金额：
 * <ul>
 *   <li>折扣基数（D2 裁决）：合同行单价 {@code ctContractLine.unitPrice}（缺失回退订单行现价）——
 *       save/approve 重复解析均从稳定基数出发，避免折后价二次折扣。</li>
 *   <li>命中区间带：行 unitPrice ← 折后单价、amount ← 折后行金额，taxAmount/amountWithTax 随动
 *       （税额外价公式 amount × rate / 100，scale 2，对齐 RequisitionToOrderConverter 既有约定），
 *       remark 打 {@code [CT_VOLUME_DISCOUNT]} 来源标记（幂等，P2-RC-023 标记范式）。</li>
 *   <li>无命中：回退原价——订单行保持自身录入价，零改写。</li>
 *   <li>门控（D3 裁决）：{@code erp-pur.ct-discount-enabled} 默认 true；关闭时引用字段仅存储不应用。</li>
 *   <li>跨域容错：contract 模块缺失时 {@code @Nullable} 注入跳过（matrix §2.4 披露范式）。</li>
 * </ul>
 */
public class ErpPurCtDiscountApplier {

    @Inject
    @Nullable
    IErpCtVolumeDiscountBiz volumeDiscountBiz;

    public void setVolumeDiscountBiz(IErpCtVolumeDiscountBiz volumeDiscountBiz) {
        this.volumeDiscountBiz = volumeDiscountBiz;
    }

    /** 门控开关（protected 供下游派生覆盖部署语义）。 */
    protected boolean enabled() {
        return AppConfig.var(ErpPurConstants.CONFIG_CT_DISCOUNT_ENABLED, Boolean.TRUE);
    }

    /**
     * 对单行应用量折扣。返回 true 表示发生改写（调用方据此重算头合计）。
     */
    public boolean applyToLine(ErpPurOrderLine line, IServiceContext context) {
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
        line.setUnitPrice(result.getDiscountedUnitPrice());
        line.setAmount(result.getLineAmount());
        recomputeTax(line);
        markRemark(line, base, result);
        return true;
    }

    /** 折扣基数 = 合同行单价（ORM to-one 关系 getter），缺失回退订单行现价。 */
    protected BigDecimal resolveBasePrice(ErpPurOrderLine line) {
        if (line.getCtContractLine() != null && line.getCtContractLine().getUnitPrice() != null
                && line.getCtContractLine().getUnitPrice().signum() > 0) {
            return line.getCtContractLine().getUnitPrice();
        }
        return line.getUnitPrice();
    }

    /** 税额外价公式（对齐 RequisitionToOrderConverter.buildLines 既有约定）。 */
    protected void recomputeTax(ErpPurOrderLine line) {
        BigDecimal amount = line.getAmount() == null ? BigDecimal.ZERO : line.getAmount();
        BigDecimal rate = line.getTaxRate();
        BigDecimal taxAmount = rate == null ? BigDecimal.ZERO
                : amount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        line.setTaxAmount(taxAmount);
        line.setAmountWithTax(amount.add(taxAmount).setScale(4, RoundingMode.HALF_UP));
    }

    /** remark 来源标记（幂等：剥离旧标记后重打，保留用户备注）。 */
    protected void markRemark(ErpPurOrderLine line, BigDecimal base, DiscountResult result) {
        String existing = line.getRemark() == null ? "" : line.getRemark();
        String stripped = existing.replace(ErpPurConstants.CT_DISCOUNT_REMARK_TAG, "").trim();
        BigDecimal saved = base.multiply(line.getQuantity())
                .subtract(result.getLineAmount() == null ? BigDecimal.ZERO : result.getLineAmount())
                .setScale(2, RoundingMode.HALF_UP);
        String tag = ErpPurConstants.CT_DISCOUNT_REMARK_TAG + "节省" + saved.stripTrailingZeros().toPlainString();
        line.setRemark(StringHelper.isBlank(stripped) ? tag : tag + " " + stripped);
    }
}
