package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
import io.nop.api.core.convert.ConvertHelper;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * ErpCtInvoicePlan triggerInvoice per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含开票计划触发生成 AP/AR 发票草稿编排（INBOUND→AP、OUTBOUND→AR + 回写 isInvoiced）。
 *
 * <p><b>跨实体访问方式偏离说明</b>：发票草稿生成经 {@link IDaoProvider} 直接持久化，
 * 而非注入 {@code IErpPurInvoiceBiz}/{@code IErpSalInvoiceBiz}（避免服务依赖级联）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtInvoicePlanTriggerInvoiceProcessor {

    @Inject
    IDaoProvider daoProvider;

    public ErpCtInvoicePlan triggerInvoice(String planId, IServiceContext context) {
        ErpCtInvoicePlan plan = requirePlan(planId);

        if (Boolean.TRUE.equals(plan.getIsInvoiced())) {
            throw new NopException(ErpCtErrors.ERR_CT_INVOICE_PLAN_ALREADY_INVOICED)
                    .param(ErpCtErrors.ARG_INVOICE_PLAN_ID, planId);
        }

        ErpCtContractLine line = plan.getContractLine();
        ErpCtContract contract = line.getContract();
        String status = contract.getStatus();
        if (Objects.equals(status, ErpCtConstants.CONTRACT_STATUS_SUSPENDED)) {
            throw new NopException(ErpCtErrors.ERR_CT_CONTRACT_SUSPENDED)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode());
        }
        if (!Objects.equals(status, ErpCtConstants.CONTRACT_STATUS_ACTIVE)) {
            throw new NopException(ErpCtErrors.ERR_CT_CONTRACT_NOT_ACTIVE)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, status);
        }

        BigDecimal amount = nz(plan.getAmount());
        String billCode = "CT-INV-" + plan.getId();
        if (Objects.equals(contract.getContractDirection(), ErpCtConstants.CONTRACT_DIRECTION_INBOUND)) {
            createApInvoiceDraft(billCode, plan, line, contract, amount);
        } else {
            createArInvoiceDraft(billCode, plan, line, contract, amount);
        }

        // 回写 isInvoiced/invoiceBillCode/invoiceDate
        plan.setIsInvoiced(true);
        plan.setInvoiceBillCode(billCode);
        plan.setInvoiceDate(CoreMetrics.today());
        dao().updateEntity(plan);
        return plan;
    }

    // ---------- 发票草稿生成（经 IDaoProvider 直接持久化） ----------

    protected void createApInvoiceDraft(String code, ErpCtInvoicePlan plan, ErpCtContractLine line,
                                        ErpCtContract contract, BigDecimal amount) {
        IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
        ErpPurInvoice invoice = dao.newEntity();
        invoice.setCode(code);
        // bridge-main-043: ct String orgId/supplierId/currencyId → pur Long（退役 owner M2.5）
        if (contract.getOrgId() != null) {
            invoice.setOrgId(ConvertHelper.toLong(contract.getOrgId()));
        }
        invoice.setSupplierId(ConvertHelper.toLong(contract.getPartnerId()));
        invoice.setBusinessDate(CoreMetrics.today());
        invoice.setCurrencyId(ConvertHelper.toLong(contract.getCurrencyId()));
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setTotalAmount(amount);
        invoice.setAmountSource(amount);
        invoice.setAmountFunctional(amount);
        invoice.setTotalAmountWithTax(amount);
        invoice.setDocStatus("DRAFT");
        invoice.setApproveStatus(ErpCtConstants.APPROVE_STATUS_UNSUBMITTED);
        invoice.setPaidStatus("UNPAID");
        invoice.setPosted(false);
        dao.saveEntity(invoice);

        ErpPurInvoiceLine invLine = daoProvider.daoFor(ErpPurInvoiceLine.class).newEntity();
        invLine.setInvoiceId(invoice.getId());
        invLine.setLineNo(1);
        // bridge-main-044: ct String materialId / md String uoMId → pur Long（退役 owner M2.5）
        if (line.getMaterialId() != null) {
            invLine.setMaterialId(ConvertHelper.toLong(line.getMaterialId()));
            if (line.getMaterial() != null) {
                invLine.setUoMId(ConvertHelper.toLong(line.getMaterial().getUoMId()));
            }
        }
        invLine.setQuantity(nz(line.getQuantity()));
        invLine.setUnitPrice(nz(line.getUnitPrice()));
        invLine.setAmount(amount);
        daoProvider.daoFor(ErpPurInvoiceLine.class).saveEntity(invLine);
    }

    protected void createArInvoiceDraft(String code, ErpCtInvoicePlan plan, ErpCtContractLine line,
                                        ErpCtContract contract, BigDecimal amount) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        ErpSalInvoice invoice = dao.newEntity();
        invoice.setCode(code);
        // bridge-main-045: ct String orgId/customerId/currencyId → sal Long（退役 owner M2.6）
        if (contract.getOrgId() != null) {
            invoice.setOrgId(ConvertHelper.toLong(contract.getOrgId()));
        }
        invoice.setCustomerId(ConvertHelper.toLong(contract.getPartnerId()));
        invoice.setBusinessDate(CoreMetrics.today());
        invoice.setCurrencyId(ConvertHelper.toLong(contract.getCurrencyId()));
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setTotalAmount(amount);
        invoice.setAmountSource(amount);
        invoice.setAmountFunctional(amount);
        invoice.setTotalAmountWithTax(amount);
        invoice.setDocStatus("DRAFT");
        invoice.setApproveStatus(ErpCtConstants.APPROVE_STATUS_UNSUBMITTED);
        invoice.setReceivedStatus("UNRECEIVED");
        invoice.setPosted(false);
        dao.saveEntity(invoice);

        ErpSalInvoiceLine invLine = daoProvider.daoFor(ErpSalInvoiceLine.class).newEntity();
        invLine.setInvoiceId(invoice.getId());
        invLine.setLineNo(1);
        // bridge-main-046: ct String materialId / md String uoMId → sal Long（退役 owner M2.6）
        if (line.getMaterialId() != null) {
            invLine.setMaterialId(ConvertHelper.toLong(line.getMaterialId()));
            if (line.getMaterial() != null) {
                invLine.setUoMId(ConvertHelper.toLong(line.getMaterial().getUoMId()));
            }
        }
        invLine.setQuantity(nz(line.getQuantity()));
        invLine.setUnitPrice(nz(line.getUnitPrice()));
        invLine.setAmount(amount);
        daoProvider.daoFor(ErpSalInvoiceLine.class).saveEntity(invLine);
    }

    // ---------- helpers ----------

    protected ErpCtInvoicePlan requirePlan(String planId) {
        ErpCtInvoicePlan plan = dao().getEntityById(planId);
        if (plan == null) {
            throw new NopException(ErpCtErrors.ERR_CT_INVOICE_PLAN_ALREADY_INVOICED)
                    .param(ErpCtErrors.ARG_INVOICE_PLAN_ID, planId);
        }
        return plan;
    }

    protected BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    protected IEntityDao<ErpCtInvoicePlan> dao() {
        return daoProvider.daoFor(ErpCtInvoicePlan.class);
    }
}
