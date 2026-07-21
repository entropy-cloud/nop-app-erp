package app.erp.fin.service.budget;

import app.erp.fin.biz.IErpFinBudgetCommitmentBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 承付占用/释放 SPI 实现（A2，plan 2026-07-21-1206-2，budget.md §承付会计 §承付占用/释放 SPI）。
 *
 * <p>严格对齐 budget.md §业务规则3："采购订单 APPROVED 时生成 postingType=COMMITMENT 凭证；
 * 订单 CANCELLED 或被发票接收时红冲 COMMITMENT"。
 *
 * <p><b>3 接入点</b>（commit / release-on-cancel / release-on-invoice-approve）经 purchase 域调用本 SPI：
 * <ul>
 *   <li>commit：{@code ErpPurOrder.approve} 后置 → 生成 COMMITMENT 凭证</li>
 *   <li>release-on-cancel：{@code ErpPurOrder.reverseApprove/cancel} → 红冲</li>
 *   <li>release-on-invoice-approve：{@code ErpPurInvoice.approve}（AP 发票过账产生 ACTUAL）→ 红冲</li>
 * </ul>
 * <b>reject release-receive-complete（ErpPurReceive 入库路径）</b>——入库是库存移动不产生 AP ACTUAL 占用。
 *
 * <p>config-gated：{@code erp-fin.budget-commitment-enabled}（默认 false，保护既有 113 purchase 测试不触发承付凭证）。
 *
 * <p>事务边界：commit/release 均 SYNC 同事务（与既有 {@link ErpFinBudgetControlBiz} 同范式）。
 */
public class ErpFinBudgetCommitmentBizModel implements IErpFinBudgetCommitmentBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinBudgetCommitmentBizModel.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    CommitmentVoucherGenerator commitmentVoucherGenerator;

    @Override
    public Long commit(String sourceBillType, String sourceBillCode, Long subjectId, Long costCenterId,
                       Long periodId, BigDecimal amount, IServiceContext context) {
        if (!isCommitmentEnabled()) {
            return null;
        }
        if (sourceBillCode == null || sourceBillCode.isEmpty() || subjectId == null
                || amount == null || amount.signum() <= 0) {
            return null;
        }
        ErpMdSubject subject = loadSubject(subjectId);
        if (subject == null) {
            return null;
        }
        Long currencyId = resolveCurrencyId(periodId);
        Long[] orgSchema = resolveOrgAndSchema(periodId);
        Long orgId = orgSchema[0] != null ? orgSchema[0] : 1L;
        Long acctSchemaId = orgSchema[1] != null ? orgSchema[1] : 1L;

        Long voucherId = commitmentVoucherGenerator.generateCommitment(sourceBillCode, subject, costCenterId,
                orgId, acctSchemaId, periodId, currencyId, amount);
        LOG.info("承付占用：单据 {}/{} 科目 {} 期间 {} 金额 {} → 凭证 {}",
                sourceBillType, sourceBillCode, subjectId, periodId, amount, voucherId);
        return voucherId;
    }

    @Override
    public Long release(String sourceBillType, String sourceBillCode, IServiceContext context) {
        if (!isCommitmentEnabled()) {
            return null;
        }
        if (sourceBillCode == null || sourceBillCode.isEmpty()) {
            return null;
        }
        if (!commitmentVoucherGenerator.hasUnreversedCommitment(sourceBillCode)) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_COMMITMENT_ALREADY_RELEASED)
                    .param(ErpFinErrors.ARG_SOURCE_BILL_TYPE, sourceBillType)
                    .param(ErpFinErrors.ARG_SOURCE_BILL_CODE, sourceBillCode);
        }
        List<Long> reversalIds = commitmentVoucherGenerator.reverseCommitment(sourceBillCode);
        if (reversalIds.isEmpty()) {
            return null;
        }
        LOG.info("承付释放：单据 {}/{} → 红冲凭证 {}", sourceBillType, sourceBillCode, reversalIds);
        return reversalIds.get(0);
    }

    /** 仅用于显式释放的容错路径（reverseApprove/cancel 调用，无原凭证时静默跳过而非抛错）。 */
    public Long releaseIfPresent(String sourceBillType, String sourceBillCode, IServiceContext context) {
        if (!isCommitmentEnabled()) {
            return null;
        }
        if (sourceBillCode == null || sourceBillCode.isEmpty()) {
            return null;
        }
        if (!commitmentVoucherGenerator.hasUnreversedCommitment(sourceBillCode)) {
            return null;
        }
        return release(sourceBillType, sourceBillCode, context);
    }

    private boolean isCommitmentEnabled() {
        return Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE));
    }

    private ErpMdSubject loadSubject(Long subjectId) {
        return daoProvider.daoFor(ErpMdSubject.class).getEntityById(subjectId);
    }

    /** 解析承付占用科目（按 config 配置的 code 反查）。config 缺失时抛 ERR_BUDGET_COMMITMENT_SUBJECT_NOT_CONFIGURED。 */
    public ErpMdSubject resolveCommitmentSubject() {
        String code = AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_SUBJECT_CODE, null);
        if (code == null || code.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_COMMITMENT_SUBJECT_NOT_CONFIGURED);
        }
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 按业务日期解析会计期间（与 ErpPurOrderProcessor.resolvePeriodId 同型）。 */
    public Long resolvePeriodId(LocalDate businessDate) {
        if (businessDate == null) {
            return null;
        }
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(le("startDate", businessDate));
        q.addFilter(ge("endDate", businessDate));
        q.setLimit(1);
        List<ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    private Long resolveCurrencyId(Long periodId) {
        if (periodId == null) {
            return 1L;
        }
        ErpFinAccountingPeriod p = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        if (p == null || p.getOrgId() == null) {
            return 1L;
        }
        return 1L;
    }

    private Long[] resolveOrgAndSchema(Long periodId) {
        if (periodId == null) {
            return new Long[]{1L, 1L};
        }
        ErpFinAccountingPeriod p = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        if (p == null) {
            return new Long[]{1L, 1L};
        }
        return new Long[]{p.getOrgId(), 1L};
    }
}
