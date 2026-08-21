
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.convert.ConvertHelper;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.common.service.MaskHelper;
import app.erp.contract.dao.entity.ErpCtRebateAccrual;
import app.erp.contract.dao.entity.ErpCtRebateAgreement;
import app.erp.ct.biz.IErpCtRebateAgreementBiz;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.processor.ErpCtRebateAgreementRunAccrualProcessor;
import app.erp.ct.service.rebate.RebateEngine;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.sal.dao.entity.ErpSalInvoice;
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
import io.nop.biz.crud.EntityData;

/**
 * 返利协议 BizModel。返利计提引擎驱动
 * （对齐 {@code docs/design/contract/volume-discount.md} §年度返利协议 / §追溯调整）。
 *
 * <p>{@code runAccrual} 聚合期间已过账 AP/AR 发票（只读查询，不跨域写），逐张喂
 * {@link RebateEngine#accrue}；PERIOD_END 一次性汇总喂入，PROGRESSIVE 逐张喂入。
 *
 * <p><b>跨实体访问方式偏离说明</b>：发票查询经 {@link IDaoProvider} 只读，而非注入
 * {@code IErpPurInvoiceBiz}/{@code IErpSalInvoiceBiz}（同 InvoicePlan，避免服务依赖级联）。
 */
@BizModel("ErpCtRebateAgreement")
public class ErpCtRebateAgreementBizModel extends CrudBizModel<ErpCtRebateAgreement>
        implements IErpCtRebateAgreementBiz {

    @Inject
    RebateEngine rebateEngine;

    @Inject
    ErpCtRebateAgreementRunAccrualProcessor runAccrualProcessor;

    public ErpCtRebateAgreementBizModel() {
        setEntityName(ErpCtRebateAgreement.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCtRebateAgreement> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpCtRebateAgreement entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    @Override
    @BizMutation
    public ErpCtRebateAgreement runAccrual(@Name("agreementId") String agreementId,
                                           @Name("asOfDate") LocalDate asOfDate,
                                           IServiceContext context) {
        return runAccrualProcessor.runAccrual(agreementId, asOfDate, context);
    }

    // ---------- helpers ----------

    protected ErpCtRebateAgreement requireAgreement(String agreementId, IServiceContext context) {
        ErpCtRebateAgreement agreement = get(agreementId, false, context);
        if (agreement == null) {
            throw new NopException(ErpCtErrors.ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE)
                    .param(ErpCtErrors.ARG_REBATE_AGREEMENT_ID, agreementId);
        }
        return agreement;
    }

    protected Set<String> loadAccruedBillCodes(String agreementId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("rebateAgreementId", agreementId));
        List<ErpCtRebateAccrual> accruals = daoProvider().daoFor(ErpCtRebateAccrual.class).findAllByQuery(q);
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
        if (Objects.equals(agreement.getRebateType(), ErpCtConstants.REBATE_TYPE_PURCHASE)) {
            // bridge-main-037: ct String partnerId → pur Long supplierId 过滤值（退役 owner M2.5）
            q.addFilter(eq("supplierId", ConvertHelper.toLong(agreement.getPartnerId())));
            return daoProvider().daoFor(ErpPurInvoice.class).findAllByQuery(q);
        } else {
            // bridge-main-038: ct String partnerId → sal Long customerId 过滤值（退役 owner M2.6）
            q.addFilter(eq("customerId", ConvertHelper.toLong(agreement.getPartnerId())));
            return daoProvider().daoFor(ErpSalInvoice.class).findAllByQuery(q);
        }
    }

    protected BigDecimal sumPeriodInvoices(ErpCtRebateAgreement agreement, LocalDate from, LocalDate to,
                                           Set<String> alreadyAccrued) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object invoice : findPeriodInvoices(agreement, from, to)) {
            String code = invoiceCode(invoice);
            if (alreadyAccrued.contains(code)) {
                continue;
            }
            total = total.add(invoiceAmount(invoice));
        }
        return total;
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

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // 授权 = 合同审批人/合同专员；非授权 = null。委托 MaskHelper（fail-closed）。
    private static final Set<String> CT_AMOUNT_ROLES = Set.of(MaskHelper.ROLE_CT_APPROVER, MaskHelper.ROLE_CT_CLERK);

    @BizLoader("totalAccumulatedAmount")
    public BigDecimal totalAccumulatedAmountMask(@ContextSource ErpCtRebateAgreement entity) {
        return MaskHelper.maskDecimal(entity.getTotalAccumulatedAmount(), CT_AMOUNT_ROLES, entity, "totalAccumulatedAmount");
    }

    @BizLoader("estimatedRebateAmount")
    public BigDecimal estimatedRebateAmountMask(@ContextSource ErpCtRebateAgreement entity) {
        return MaskHelper.maskDecimal(entity.getEstimatedRebateAmount(), CT_AMOUNT_ROLES, entity, "estimatedRebateAmount");
    }

}
