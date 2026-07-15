
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.ct.biz.IErpCtInvoicePlanBiz;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 开票计划 BizModel。InvoicePlan 触发生成 AP/AR 发票草稿
 * （对齐 {@code docs/design/contract/state-machine.md} §InvoicePlan 触发）。
 *
 * <p>INBOUND 合同→AP 发票草稿，OUTBOUND 合同→AR 发票草稿。
 *
 * <p><b>跨实体访问方式偏离说明</b>：发票草稿生成经 {@link IDaoProvider} 直接持久化，
 * 而非注入 {@code IErpPurInvoiceBiz}/{@code IErpSalInvoiceBiz}。原因：硬注入跨域发票
 * BizModel 会将其完整服务依赖链（sales→inventory→...）级联进合同域，破坏其隔离单元测试。
 * 发票草稿为纯实体构造 + 持久化（不经 submit/approve 业务管道），IDaoProvider 是最小耦合方案。
 * 合同→发票为显式业务触发，生成的草稿后续由 purchase/sales 域审核过账管道处理。
 */
@BizModel("ErpCtInvoicePlan")
public class ErpCtInvoicePlanBizModel extends CrudBizModel<ErpCtInvoicePlan> implements IErpCtInvoicePlanBiz {

    public ErpCtInvoicePlanBizModel() {
        setEntityName(ErpCtInvoicePlan.class.getName());
    }

    @Override
    @BizMutation
    public ErpCtInvoicePlan triggerInvoice(@Name("planId") Long planId, IServiceContext context) {
        ErpCtInvoicePlan plan = requirePlan(planId, context);

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
        updateEntity(plan, null, context);
        return plan;
    }

    @Override
    @BizMutation
    public int triggerDuePlans(@Name("contractId") Long contractId,
                               @Name("asOfDate") LocalDate asOfDate,
                               IServiceContext context) {
        // config-gated：erp-ct.invoiceplan-auto-trigger 默认 true
        if (!AppConfig.var(ErpCtConfigs.CFG_INVOICEPLAN_AUTO_TRIGGER, true)) {
            return 0;
        }
        QueryBean query = new QueryBean();
        query.addFilter(le("planDate", asOfDate));
        query.addFilter(eq("isInvoiced", false));
        // 经 dao() 直查绕过 XMeta 查询算子白名单（planDate 仅允许 [eq,in,dateBetween,dateTimeBetween]，
        // 不支持 le；findList 会经 meta 安全层校验报错）。对齐同模块 loadAccruedBillCodes /
        // findPeriodInvoices 经 daoProvider 直查的范式——内部批量逻辑不经外部 GraphQL 查询算子约束。
        List<ErpCtInvoicePlan> due = daoProvider().daoFor(ErpCtInvoicePlan.class).findAllByQuery(query);
        int triggered = 0;
        for (ErpCtInvoicePlan plan : due) {
            // 里程碑/完工条款需人工/上游事件确认；triggerInvoice 单点入口校验合同 ACTIVE
            ErpCtContractLine line = plan.getContractLine();
            if (line == null || line.getContractId() == null
                    || !Objects.equals(line.getContractId(), contractId)) {
                continue;
            }
            triggerInvoice(plan.getId(), context);
            triggered++;
        }
        return triggered;
    }

    // ---------- 发票草稿生成（经 IDaoProvider 直接持久化） ----------

    protected void createApInvoiceDraft(String code, ErpCtInvoicePlan plan, ErpCtContractLine line,
                                        ErpCtContract contract, BigDecimal amount) {
        IEntityDao<ErpPurInvoice> dao = daoProvider().daoFor(ErpPurInvoice.class);
        ErpPurInvoice invoice = dao.newEntity();
        invoice.setCode(code);
        if (contract.getOrgId() != null) {
            invoice.setOrgId(contract.getOrgId());
        }
        invoice.setSupplierId(contract.getPartnerId());
        invoice.setBusinessDate(CoreMetrics.today());
        invoice.setCurrencyId(contract.getCurrencyId());
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

        ErpPurInvoiceLine invLine = daoProvider().daoFor(ErpPurInvoiceLine.class).newEntity();
        invLine.setInvoiceId(invoice.getId());
        invLine.setLineNo(1);
        if (line.getMaterialId() != null) {
            invLine.setMaterialId(line.getMaterialId());
            if (line.getMaterial() != null) {
                invLine.setUoMId(line.getMaterial().getUoMId());
            }
        }
        invLine.setQuantity(nz(line.getQuantity()));
        invLine.setUnitPrice(nz(line.getUnitPrice()));
        invLine.setAmount(amount);
        daoProvider().daoFor(ErpPurInvoiceLine.class).saveEntity(invLine);
    }

    protected void createArInvoiceDraft(String code, ErpCtInvoicePlan plan, ErpCtContractLine line,
                                        ErpCtContract contract, BigDecimal amount) {
        IEntityDao<ErpSalInvoice> dao = daoProvider().daoFor(ErpSalInvoice.class);
        ErpSalInvoice invoice = dao.newEntity();
        invoice.setCode(code);
        if (contract.getOrgId() != null) {
            invoice.setOrgId(contract.getOrgId());
        }
        invoice.setCustomerId(contract.getPartnerId());
        invoice.setBusinessDate(CoreMetrics.today());
        invoice.setCurrencyId(contract.getCurrencyId());
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

        ErpSalInvoiceLine invLine = daoProvider().daoFor(ErpSalInvoiceLine.class).newEntity();
        invLine.setInvoiceId(invoice.getId());
        invLine.setLineNo(1);
        if (line.getMaterialId() != null) {
            invLine.setMaterialId(line.getMaterialId());
            if (line.getMaterial() != null) {
                invLine.setUoMId(line.getMaterial().getUoMId());
            }
        }
        invLine.setQuantity(nz(line.getQuantity()));
        invLine.setUnitPrice(nz(line.getUnitPrice()));
        invLine.setAmount(amount);
        daoProvider().daoFor(ErpSalInvoiceLine.class).saveEntity(invLine);
    }

    // ---------- helpers ----------

    protected ErpCtInvoicePlan requirePlan(Long planId, IServiceContext context) {
        ErpCtInvoicePlan plan = get(String.valueOf(planId), false, context);
        if (plan == null) {
            throw new NopException(ErpCtErrors.ERR_CT_INVOICE_PLAN_ALREADY_INVOICED)
                    .param(ErpCtErrors.ARG_INVOICE_PLAN_ID, planId);
        }
        return plan;
    }

    protected BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}
