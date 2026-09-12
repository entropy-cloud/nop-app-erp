package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtRebateAccrual;
import app.erp.contract.dao.entity.ErpCtRebateAgreement;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.rebate.RebateEngine;
import app.erp.ct.service.statemachine.ErpCtRebateAgreementStateMachine;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.sal.dao.entity.ErpSalInvoice;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * ErpCtRebateAgreement runAccrual per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含返利计提编排（聚合期间已过账 AP/AR 发票只读查询 + 逐张/期末喂 RebateEngine）。
 *
 * <p><b>跨实体访问方式偏离说明</b>：发票查询经 {@link IDaoProvider} 只读（避免服务依赖级联）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtRebateAgreementRunAccrualProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    RebateEngine rebateEngine;

    @Inject
    ErpCtRebateAgreementStateMachine stateMachine;

    public ErpCtRebateAgreement runAccrual(String agreementId, LocalDate asOfDate, IServiceContext context) {
        ErpCtRebateAgreement agreement = requireAgreement(agreementId);
        if (!stateMachine.isActive(agreement.getStatus())) {
            throw new NopException(ErpCtErrors.ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE)
                    .param(ErpCtErrors.ARG_REBATE_AGREEMENT_ID, agreementId)
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, agreement.getStatus());
        }

        LocalDate periodStart = agreement.getStartDate();
        LocalDate periodEnd = asOfDate == null ? CoreMetrics.today() : asOfDate;
        Set<String> alreadyAccruedCodes = loadAccruedBillCodes(agreementId);

        if (Objects.equals(agreement.getAccrualMethod(), ErpCtConstants.ACCRUAL_METHOD_PERIOD_END)) {
            // P1-CK-ct-001（plan 2026-09-12-0400-1 Phase 1）：PERIOD_END 改逐发票消费——
            // 修复前聚合一次性喂入且 sourceBillCode=PERIOD-伪码与发票 code 去重键不匹配
            // （重跑基数线性翻倍）；现逐张喂 accrue（sourceBillCode=发票 code）：
            // ① 幂等由 loadAccruedBillCodes 发票 code 去重天然获得；
            // ② 期末总额语义由 telescoping 保持（Σ delta_i = expected(期末累计) − 0，
            //    独立审查已证 tier 跨档/负 delta/固定额档均不破坏恒等式）。
            for (Object invoice : findPeriodInvoices(agreement, periodStart, periodEnd)) {
                String code = invoiceCode(invoice);
                if (alreadyAccruedCodes.contains(code)) {
                    continue;
                }
                rebateEngine.accrue(agreement, invoiceAmount(invoice), billTypeFor(agreement), code, context);
            }
        } else {
            // PROGRESSIVE：逐张已过账发票即时计提
            for (Object invoice : findPeriodInvoices(agreement, periodStart, periodEnd)) {
                BigDecimal amount = invoiceAmount(invoice);
                String code = invoiceCode(invoice);
                String billType = billTypeFor(agreement);
                if (alreadyAccruedCodes.contains(code)) {
                    continue;
                }
                rebateEngine.accrue(agreement, amount, billType, code, context);
            }
        }
        return agreement;
    }

    // ---------- helpers ----------

    protected ErpCtRebateAgreement requireAgreement(String agreementId) {
        ErpCtRebateAgreement agreement = dao().getEntityById(agreementId);
        if (agreement == null) {
            throw new NopException(ErpCtErrors.ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE)
                    .param(ErpCtErrors.ARG_REBATE_AGREEMENT_ID, agreementId);
        }
        return agreement;
    }

    protected Set<String> loadAccruedBillCodes(String agreementId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("rebateAgreementId", agreementId));
        List<ErpCtRebateAccrual> accruals = daoProvider.daoFor(ErpCtRebateAccrual.class).findAllByQuery(q);
        Set<String> codes = new HashSet<>();
        for (ErpCtRebateAccrual a : accruals) {
            if (a.getSourceBillCode() != null) {
                codes.add(a.getSourceBillCode());
            }
        }
        return codes;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected List findPeriodInvoices(ErpCtRebateAgreement agreement, LocalDate from, LocalDate to) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("posted", true));
        q.addFilter(ge("businessDate", from));
        q.addFilter(le("businessDate", to));
        List invoices;
        if (Objects.equals(agreement.getRebateType(), ErpCtConstants.REBATE_TYPE_PURCHASE)) {
            q.addFilter(eq("supplierId", agreement.getPartnerId()));
            invoices = daoProvider.daoFor(ErpPurInvoice.class).findAllByQuery(q);
        } else {
            q.addFilter(eq("customerId", agreement.getPartnerId()));
            invoices = daoProvider.daoFor(ErpSalInvoice.class).findAllByQuery(q);
        }
        // P1-CK-ct-002（plan 2026-09-12-0400-1 Phase 1）：排除返利结算贷项发票
        // （code=CT-REBATE-*，与 postSettlement 生成侧前缀约定对偶）——已付返利不得回吸累计基数
        List result = new java.util.ArrayList();
        for (Object invoice : invoices) {
            String code = invoiceCode(invoice);
            if (code != null && code.startsWith("CT-REBATE-")) {
                continue;
            }
            result.add(invoice);
        }
        return result;
    }

    protected BigDecimal invoiceAmount(Object invoice) {
        if (invoice instanceof ErpPurInvoice) {
            return nz(((ErpPurInvoice) invoice).getTotalAmountWithTax());
        }
        if (invoice instanceof ErpSalInvoice) {
            return nz(((ErpSalInvoice) invoice).getTotalAmountWithTax());
        }
        return BigDecimal.ZERO;
    }

    protected String invoiceCode(Object invoice) {
        if (invoice instanceof ErpPurInvoice) {
            return ((ErpPurInvoice) invoice).getCode();
        }
        if (invoice instanceof ErpSalInvoice) {
            return ((ErpSalInvoice) invoice).getCode();
        }
        return null;
    }

    protected String billTypeFor(ErpCtRebateAgreement agreement) {
        return Objects.equals(agreement.getRebateType(), ErpCtConstants.REBATE_TYPE_PURCHASE)
                ? "AP_INVOICE" : "AR_INVOICE";
    }

    protected BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    protected IEntityDao<ErpCtRebateAgreement> dao() {
        return daoProvider.daoFor(ErpCtRebateAgreement.class);
    }
}
