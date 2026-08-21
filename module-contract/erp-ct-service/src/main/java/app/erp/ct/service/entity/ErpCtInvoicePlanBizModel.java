
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import app.erp.common.service.MaskHelper;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.ct.biz.ErpCtInvoicePlanGenerateItem;
import app.erp.ct.biz.IErpCtInvoicePlanBiz;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.processor.ErpCtInvoicePlanGenerateByTermProcessor;
import app.erp.ct.service.processor.ErpCtInvoicePlanTriggerDuePlansProcessor;
import app.erp.ct.service.processor.ErpCtInvoicePlanTriggerInvoiceProcessor;
import io.nop.biz.crud.EntityData;
import io.nop.api.core.convert.ConvertHelper;
import jakarta.inject.Inject;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    @Inject
    ErpCtInvoicePlanTriggerInvoiceProcessor triggerInvoiceProcessor;

    @Inject
    ErpCtInvoicePlanTriggerDuePlansProcessor triggerDuePlansProcessor;

    @Inject
    ErpCtInvoicePlanGenerateByTermProcessor generateByTermProcessor;

    public ErpCtInvoicePlanBizModel() {
        setEntityName(ErpCtInvoicePlan.class.getName());
    }

    @Override
    @BizMutation
    public ErpCtInvoicePlan triggerInvoice(@Name("planId") String planId, IServiceContext context) {
        return triggerInvoiceProcessor.triggerInvoice(planId, context);
    }

    @Override
    @BizMutation
    public int triggerDuePlans(@Name("contractId") String contractId,
                               @Name("asOfDate") LocalDate asOfDate,
                               IServiceContext context) {
        return triggerDuePlansProcessor.triggerDuePlans(contractId, asOfDate, context);
    }

    @Override
    @BizMutation
    public List<ErpCtInvoicePlan> generateInvoicePlansByTerm(@Name("contractId") String contractId,
                                                             @Name("items") List<ErpCtInvoicePlanGenerateItem> items,
                                                             IServiceContext context) {
        return generateByTermProcessor.generateInvoicePlansByTerm(contractId, items, context);
    }

    // ---------- 已开票禁改守卫（RC-R1.33 P1-RC-074，UC-CT-03 C，D4 契约） ----------

    /**
     * isInvoiced=true 的 InvoicePlan 拒绝修改 amount/planDate/invoiceTerm（已开票一致性）。
     * 已开票状态经 ORM 脏值追踪取"本次更新前"的持久化状态（客户端同请求改 isInvoiced 时也能拿到旧值，
     * 防解锁绕过），对齐 R1.9 publish 守卫范式（{@code ErpHrSurveyBizModel#defaultPrepareUpdate}）。
     * 非计费字段（remark 等）放行。triggerInvoice 回写经 Processor 层 dao 直写
     * 绕过本守卫，不阻塞既有触发面。
     */
    @Override
    protected void defaultPrepareUpdate(EntityData<ErpCtInvoicePlan> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        Map<String, Object> data = entityData.getData();
        if (data == null || !touchesGuardedFields(data)) {
            return;
        }
        ErpCtInvoicePlan entity = entityData.getEntity();
        boolean persistedInvoiced = Boolean.TRUE.equals(entity.getIsInvoiced());
        if (entity.orm_propDirtyByName("isInvoiced")) {
            Object old = entity.orm_dirtyOldValues().get("isInvoiced");
            if (old != null) {
                Boolean oldVal = ConvertHelper.toBoolean(old);
                persistedInvoiced = oldVal != null && oldVal;
            }
        }
        if (persistedInvoiced) {
            throw new NopException(ErpCtErrors.ERR_CT_INVOICE_PLAN_INVOICED_IMMUTABLE)
                    .param(ErpCtErrors.ARG_INVOICE_PLAN_ID, entity.getId());
        }
    }

    private boolean touchesGuardedFields(Map<String, Object> data) {
        return data.containsKey("amount")
                || data.containsKey("planDate")
                || data.containsKey("invoiceTerm");
    }

    // ---------- 发票草稿生成（经 IDaoProvider 直接持久化） ----------

    protected void createApInvoiceDraft(String code, ErpCtInvoicePlan plan, ErpCtContractLine line,
                                        ErpCtContract contract, BigDecimal amount) {
        IEntityDao<ErpPurInvoice> dao = daoProvider().daoFor(ErpPurInvoice.class);
        ErpPurInvoice invoice = dao.newEntity();
        invoice.setCode(code);
        // bridge-main-033: ct String orgId/supplierId/currencyId → pur Long（退役 owner M2.5）
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

        ErpPurInvoiceLine invLine = daoProvider().daoFor(ErpPurInvoiceLine.class).newEntity();
        invLine.setInvoiceId(invoice.getId());
        invLine.setLineNo(1);
        // bridge-main-034: ct String materialId / md String uoMId → pur Long（退役 owner M2.5）
        if (line.getMaterialId() != null) {
            invLine.setMaterialId(ConvertHelper.toLong(line.getMaterialId()));
            if (line.getMaterial() != null) {
                invLine.setUoMId(ConvertHelper.toLong(line.getMaterial().getUoMId()));
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
        // bridge-main-035: ct String orgId/customerId/currencyId → sal Long（退役 owner M2.6）
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

        ErpSalInvoiceLine invLine = daoProvider().daoFor(ErpSalInvoiceLine.class).newEntity();
        invLine.setInvoiceId(invoice.getId());
        invLine.setLineNo(1);
        // bridge-main-036: ct String materialId / md String uoMId → sal Long（退役 owner M2.6）
        if (line.getMaterialId() != null) {
            invLine.setMaterialId(ConvertHelper.toLong(line.getMaterialId()));
            if (line.getMaterial() != null) {
                invLine.setUoMId(ConvertHelper.toLong(line.getMaterial().getUoMId()));
            }
        }
        invLine.setQuantity(nz(line.getQuantity()));
        invLine.setUnitPrice(nz(line.getUnitPrice()));
        invLine.setAmount(amount);
        daoProvider().daoFor(ErpSalInvoiceLine.class).saveEntity(invLine);
    }

    // ---------- helpers ----------

    protected ErpCtInvoicePlan requirePlan(String planId, IServiceContext context) {
        ErpCtInvoicePlan plan = get(planId, false, context);
        if (plan == null) {
            throw new NopException(ErpCtErrors.ERR_CT_INVOICE_PLAN_ALREADY_INVOICED)
                    .param(ErpCtErrors.ARG_INVOICE_PLAN_ID, planId);
        }
        return plan;
    }

    protected BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // 授权 = 合同审批人/合同专员；非授权 = null。委托 MaskHelper（fail-closed）。
    private static final Set<String> CT_AMOUNT_ROLES = Set.of(MaskHelper.ROLE_CT_APPROVER, MaskHelper.ROLE_CT_CLERK);

    @BizLoader("amount")
    public BigDecimal amountMask(@ContextSource ErpCtInvoicePlan entity) {
        return MaskHelper.maskDecimal(entity.getAmount(), CT_AMOUNT_ROLES, entity, "amount");
    }

}
