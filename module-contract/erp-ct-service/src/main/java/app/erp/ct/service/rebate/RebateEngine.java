
package app.erp.ct.service.rebate;

import app.erp.contract.dao.entity.ErpCtRebateAccrual;
import app.erp.contract.dao.entity.ErpCtRebateAgreement;
import app.erp.contract.dao.entity.ErpCtRebateTier;
import app.erp.ct.biz.IErpCtRebateAccrualBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;

/**
 * 返利计提引擎（{@code volume-discount.md} §返利计提明细 / §追溯调整）。
 *
 * <p>支持两法：
 * <ul>
 *   <li>{@code PERIOD_END}：期末一次性按累计金额所在档计提。</li>
 *   <li>{@code PROGRESSIVE}：即时计提，跨档时对已计提行追溯补差（delta = 新预期返利 - 已计提返利），
 *       返回负额触发反向计提（退货扣减场景）。</li>
 * </ul>
 *
 * <p>返利计算模型（对齐 {@code volume-discount.md} §追溯调整示例：¥1.2M × 2% = ¥24K）：
 * 按累计金额命中所在档，整额 × 该档 rebatePercent（或 rebateAmount 固定额）。
 * 跨档时 delta 自然捕获全量追溯补差（已计提行被重新评级到新档率）。
 */
public class RebateEngine {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCtRebateAccrualBiz rebateAccrualBiz;

    /**
     * 对单张已过账发票事件计提返利。
     *
     * @param agreement       返利协议（须 ACTIVE）
     * @param invoiceAmount   本张发票金额（退货为负）
     * @param sourceBillType  来源单据类型（AP_INVOICE / AR_INVOICE）
     * @param sourceBillCode  来源单据号
     * @param context         服务上下文（穿透 CrudBizModel 数据权限/审计管道）
     * @return 新建的计提明细（accruedRebate 可能为负——跨档回落/退货）
     */
    public ErpCtRebateAccrual accrue(ErpCtRebateAgreement agreement, BigDecimal invoiceAmount,
                                     String sourceBillType, String sourceBillCode,
                                     IServiceContext context) {
        validateActive(agreement);

        BigDecimal amount = invoiceAmount == null ? BigDecimal.ZERO : invoiceAmount;
        BigDecimal newCumulative = nz(agreement.getTotalAccumulatedAmount()).add(amount);

        List<ErpCtRebateTier> tiers = loadTiers(agreement.getId());
        BigDecimal expectedRebate = computeRebate(tiers, newCumulative);
        BigDecimal alreadyAccrued = sumAccrued(agreement.getId());

        BigDecimal delta = expectedRebate.subtract(alreadyAccrued);

        ErpCtRebateAccrual accrual = newEntity();
        accrual.setRebateAgreementId(agreement.getId());
        accrual.setSourceBillType(sourceBillType);
        accrual.setSourceBillCode(sourceBillCode);
        accrual.setBillAmountSource(amount);
        accrual.setAccruedRebate(delta);
        accrual.setAccrualDate(CoreMetrics.today());
        accrual.setIsSettled(false);
        // H-4（plan 2026-07-20-2200-1）：经 I*Biz 走 CrudBizModel 管道（数据权限/审计/钩子），
        // 不再直接 dao().saveEntity() 绕过生命周期。
        rebateAccrualBiz.saveEntity(accrual, null, context);

        // 更新协议累计/预估
        agreement.setTotalAccumulatedAmount(newCumulative);
        agreement.setEstimatedRebateAmount(expectedRebate);
        daoProvider.daoFor(ErpCtRebateAgreement.class).updateEntity(agreement);

        return accrual;
    }

    /**
     * 期末一次性计提（PERIOD_END）：直接按期末累计金额所在档计提全额返利。
     *
     * @param agreement      返利协议
     * @param periodAmount   本期新增累计金额
     * @param context        服务上下文（穿透 CrudBizModel 数据权限/审计管道）
     * @return 计提明细
     */
    public ErpCtRebateAccrual accruePeriodEnd(ErpCtRebateAgreement agreement, BigDecimal periodAmount,
                                              IServiceContext context) {
        return accrue(agreement, periodAmount, "PERIOD_END",
                "PERIOD-" + io.nop.api.core.time.CoreMetrics.today(), context);
    }

    /**
     * 按累计金额命中所在档计算返利（整额 × 命中档率，对齐设计文档示例）。
     */
    public BigDecimal computeRebate(List<ErpCtRebateTier> tiers, BigDecimal cumulative) {
        ErpCtRebateTier matched = matchTier(tiers, cumulative);
        if (matched == null) {
            return BigDecimal.ZERO;
        }
        // 固定返利金额优先于比例
        if (matched.getRebateAmount() != null && matched.getRebateAmount().signum() > 0) {
            return matched.getRebateAmount().setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal percent = matched.getRebatePercent() == null ? BigDecimal.ZERO : matched.getRebatePercent();
        return cumulative.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    protected ErpCtRebateTier matchTier(List<ErpCtRebateTier> tiers, BigDecimal amount) {
        return tiers.stream()
                .filter(t -> (t.getFromAmount() == null || amount.compareTo(t.getFromAmount()) >= 0)
                        && (t.getToAmount() == null || amount.compareTo(t.getToAmount()) < 0))
                .max(Comparator.comparing(t -> t.getFromAmount() == null ? BigDecimal.ZERO : t.getFromAmount()))
                .orElse(null);
    }

    protected BigDecimal sumAccrued(Long agreementId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpCtRebateAccrual a : loadAccruals(agreementId)) {
            sum = sum.add(nz(a.getAccruedRebate()));
        }
        return sum;
    }

    protected void validateActive(ErpCtRebateAgreement agreement) {
        if (!Objects.equals(agreement.getStatus(), ErpCtConstants.REBATE_AGREEMENT_STATUS_ACTIVE)) {
            throw new NopException(ErpCtErrors.ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE)
                    .param(ErpCtErrors.ARG_REBATE_AGREEMENT_ID, agreement.getId())
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, agreement.getStatus());
        }
    }

    protected List<ErpCtRebateTier> loadTiers(Long agreementId) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("rebateAgreementId", agreementId));
        return daoProvider.daoFor(ErpCtRebateTier.class).findAllByQuery(query);
    }

    protected List<ErpCtRebateAccrual> loadAccruals(Long agreementId) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("rebateAgreementId", agreementId));
        return daoProvider.daoFor(ErpCtRebateAccrual.class).findAllByQuery(query);
    }

    protected ErpCtRebateAccrual newEntity() {
        return dao().newEntity();
    }

    protected IEntityDao<ErpCtRebateAccrual> dao() {
        return daoProvider.daoFor(ErpCtRebateAccrual.class);
    }

    protected BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
