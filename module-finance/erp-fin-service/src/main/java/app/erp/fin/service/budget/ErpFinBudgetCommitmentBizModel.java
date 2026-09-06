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
    public String commit(String sourceBillType, String sourceBillCode, String subjectId, String costCenterId,
                       String periodId, BigDecimal amount, IServiceContext context) {
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
        String currencyId = resolveCurrencyId(periodId);
        String[] orgSchema = resolveOrgAndSchema(periodId);
        String orgId = orgSchema[0];
        String acctSchemaId = orgSchema[1];

        String voucherId = commitmentVoucherGenerator.generateCommitment(sourceBillType, sourceBillCode, subject, costCenterId,
                orgId, acctSchemaId, periodId, currencyId, amount);
        LOG.info("Budget commitment occupied: bill {}/{} subject {} period {} amount {} → voucher {}",
                sourceBillType, sourceBillCode, subjectId, periodId, amount, voucherId);
        return voucherId;
    }

    @Override
    public String release(String sourceBillType, String sourceBillCode, IServiceContext context) {
        if (!isCommitmentEnabled()) {
            return null;
        }
        if (sourceBillCode == null || sourceBillCode.isEmpty()) {
            return null;
        }
        if (!commitmentVoucherGenerator.hasUnreversedCommitment(sourceBillType, sourceBillCode)) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_COMMITMENT_ALREADY_RELEASED)
                    .param(ErpFinErrors.ARG_SOURCE_BILL_TYPE, sourceBillType)
                    .param(ErpFinErrors.ARG_SOURCE_BILL_CODE, sourceBillCode);
        }
        List<String> reversalIds = commitmentVoucherGenerator.reverseCommitment(sourceBillType, sourceBillCode);
        if (reversalIds.isEmpty()) {
            return null;
        }
        LOG.info("Budget commitment released: bill {}/{} → reversal vouchers {}", sourceBillType, sourceBillCode, reversalIds);
        return reversalIds.get(0);
    }

    /** 容错释放（{@link IErpFinBudgetCommitmentBiz#releaseIfPresent}）：无原承付凭证时静默返回 null，不抛守卫异常。
     *  供 release-on-cancel / release-on-return（P1-MA2-082）容错路径调用。 */
    @Override
    public String releaseIfPresent(String sourceBillType, String sourceBillCode, IServiceContext context) {
        if (!isCommitmentEnabled()) {
            return null;
        }
        if (sourceBillCode == null || sourceBillCode.isEmpty()) {
            return null;
        }
        if (!commitmentVoucherGenerator.hasUnreversedCommitment(sourceBillType, sourceBillCode)) {
            return null;
        }
        return release(sourceBillType, sourceBillCode, context);
    }

    private boolean isCommitmentEnabled() {
        return Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE));
    }

    private ErpMdSubject loadSubject(String subjectId) {
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
    public String resolvePeriodId(LocalDate businessDate) {
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

    private String resolveCurrencyId(String periodId) {
        // F2.3（P1-CK-fin3-005）：经主账套解析本位币——修复前硬编码 "1"
        String[] orgSchema = resolveOrgAndSchema(periodId);
        if (orgSchema[1] == null) {
            return "1"; // 无账套时回退本位币占位（凭证行 mandatory）
        }
        app.erp.md.dao.entity.ErpMdAcctSchema schema =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class).getEntityById(orgSchema[1]);
        return schema != null && schema.getFunctionalCurrencyId() != null ? schema.getFunctionalCurrencyId() : "1";
    }

    private String[] resolveOrgAndSchema(String periodId) {
        // F2.3（P1-CK-fin3-005）：经期间 org → AcctSchemaResolver 解析主账套——修复前 acctSchemaId
        // 硬编码 "1"（多账套下承付凭证全部落账套 1）；org 也可能为 null（期间缺 org）
        if (periodId == null) {
            return new String[]{"1", "1"}; // 无期间回退（凭证行 mandatory）
        }
        ErpFinAccountingPeriod p = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        if (p == null || p.getOrgId() == null) {
            return new String[]{"1", "1"};
        }
        String schemaId = app.erp.md.dao.AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, p.getOrgId());
        return new String[]{p.getOrgId(), schemaId != null ? schemaId : "1"};
    }
}
