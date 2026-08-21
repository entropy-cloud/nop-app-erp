package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtRebateAccrual;
import app.erp.contract.dao.entity.ErpCtRebateAgreement;
import app.erp.contract.dao.entity.ErpCtRebateSettlement;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.statemachine.ErpCtRebateSettlementStateMachine;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.convert.ConvertHelper;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCtRebateSettlement postSettlement per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含返利结算过账编排（DRAFT→POSTED + 汇总未结算计提 + 生成贷项凭证 + 标记计提已结算）。
 *
 * <p><b>跨实体访问方式偏离说明</b>：贷项凭证（负额发票）经 {@link IDaoProvider} 直接持久化，
 * 而非注入 {@code IErpPurInvoiceBiz}/{@code IErpSalInvoiceBiz}（避免服务依赖级联）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtRebateSettlementPostSettlementProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpCtRebateSettlementStateMachine stateMachine;

    public ErpCtRebateSettlement postSettlement(String settlementId, IServiceContext context) {
        ErpCtRebateSettlement settlement = requireSettlement(settlementId);
        try {
            stateMachine.assertCanPostSettlement(settlement.getStatus());
        } catch (NopException e) {
            throw illegalTransition(settlementId, settlement, e);
        }

        // 汇总关联未结算计提
        List<ErpCtRebateAccrual> unsettled = findUnsettledAccruals(settlement.getRebateAgreementId());
        BigDecimal total = BigDecimal.ZERO;
        for (ErpCtRebateAccrual a : unsettled) {
            total = total.add(nz(a.getAccruedRebate()));
        }

        ErpCtRebateAgreement agreement = daoProvider.daoFor(ErpCtRebateAgreement.class)
                .getEntityById(settlement.getRebateAgreementId());

        // 币种取自关联合同（发票 CURRENCY_ID NOT NULL）
        Long currencyId = resolveCurrencyId(agreement);
        // 贷项行 materialId/uoMId 取自关联合同首行及其主物料（返利为金额型；发票行 MATERIAL_ID/UO_M_ID NOT NULL）
        Long materialId = resolveMaterialId(agreement);
        Long uomId = resolveUoMId(ConvertHelper.toString(materialId));

        // 生成贷项凭证（负额发票）——Phase 1 Decision：复用既有发票实体以负额表达
        String creditMemoCode = "CT-REBATE-" + settlement.getId();
        BigDecimal creditAmount = total.negate(); // 贷项 = 负额
        if (agreement != null && Objects.equals(agreement.getRebateType(), ErpCtConstants.REBATE_TYPE_PURCHASE)) {
            createNegativeApInvoice(creditMemoCode, agreement, currencyId, materialId, uomId, creditAmount);
            settlement.setCreditMemoBillType("AP_INVOICE");
        } else if (agreement != null) {
            createNegativeArInvoice(creditMemoCode, agreement, currencyId, materialId, uomId, creditAmount);
            settlement.setCreditMemoBillType("AR_INVOICE");
        }
        settlement.setCreditMemoBillCode(creditMemoCode);

        // 标记计提已结算
        LocalDate today = CoreMetrics.today();
        IEntityDao<ErpCtRebateAccrual> accrualDao = daoProvider.daoFor(ErpCtRebateAccrual.class);
        for (ErpCtRebateAccrual a : unsettled) {
            a.setIsSettled(true);
            a.setSettledDate(today);
            accrualDao.updateEntity(a);
        }

        // 结算单过账
        settlement.setTotalRebateAmount(total);
        settlement.setStatus(stateMachine.postSettlementTargetStatus());
        settlement.setPostedAt(CoreMetrics.currentTimestamp());
        settlement.setPostedBy(currentUserId());
        dao().updateEntity(settlement);
        return settlement;
    }

    // ---------- 贷项凭证生成（负额发票，经 IDaoProvider 直接持久化） ----------
    // O-4 架构豁免：返利结算生成的贷项凭证（负额发票）跨模块直接持久化，不走采购/销售域审批管道。
    // 理由/风险/补偿见 docs/architecture/posting-exemptions.md §ErpCtRebateSettlementBizModel

    protected void createNegativeApInvoice(String code, ErpCtRebateAgreement agreement,
                                           Long currencyId, Long materialId, Long uomId, BigDecimal negativeAmount) {
        IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
        ErpPurInvoice invoice = dao.newEntity();
        invoice.setCode(code);
        // bridge-main-049: ct String orgId/partnerId → pur Long（退役 owner M2.5）
        if (agreement.getOrgId() != null) {
            invoice.setOrgId(ConvertHelper.toLong(agreement.getOrgId()));
        }
        invoice.setSupplierId(ConvertHelper.toLong(agreement.getPartnerId()));
        invoice.setBusinessDate(CoreMetrics.today());
        invoice.setCurrencyId(currencyId);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setTotalAmount(negativeAmount);
        invoice.setAmountSource(negativeAmount);
        invoice.setAmountFunctional(negativeAmount);
        invoice.setTotalAmountWithTax(negativeAmount);
        invoice.setDocStatus("DRAFT");
        invoice.setApproveStatus(ErpCtConstants.APPROVE_STATUS_UNSUBMITTED);
        invoice.setPaidStatus("UNPAID");
        invoice.setPosted(false);
        dao.saveEntity(invoice);

        ErpPurInvoiceLine line = daoProvider.daoFor(ErpPurInvoiceLine.class).newEntity();
        line.setInvoiceId(invoice.getId());
        line.setLineNo(1);
        line.setMaterialId(materialId);
        line.setUoMId(uomId);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(negativeAmount);
        line.setAmount(negativeAmount);
        daoProvider.daoFor(ErpPurInvoiceLine.class).saveEntity(line);
    }

    protected void createNegativeArInvoice(String code, ErpCtRebateAgreement agreement,
                                           Long currencyId, Long materialId, Long uomId, BigDecimal negativeAmount) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        ErpSalInvoice invoice = dao.newEntity();
        invoice.setCode(code);
        // bridge-main-051: ct String orgId/partnerId → sal Long（退役 owner M2.6）
        if (agreement.getOrgId() != null) {
            invoice.setOrgId(ConvertHelper.toLong(agreement.getOrgId()));
        }
        invoice.setCustomerId(ConvertHelper.toLong(agreement.getPartnerId()));
        invoice.setBusinessDate(CoreMetrics.today());
        invoice.setCurrencyId(currencyId);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setTotalAmount(negativeAmount);
        invoice.setAmountSource(negativeAmount);
        invoice.setAmountFunctional(negativeAmount);
        invoice.setTotalAmountWithTax(negativeAmount);
        invoice.setDocStatus("DRAFT");
        invoice.setApproveStatus("UNSUBMITTED");
        invoice.setReceivedStatus("UNRECEIVED");
        invoice.setPosted(false);
        dao.saveEntity(invoice);

        ErpSalInvoiceLine line = daoProvider.daoFor(ErpSalInvoiceLine.class).newEntity();
        line.setInvoiceId(invoice.getId());
        line.setLineNo(1);
        line.setMaterialId(materialId);
        line.setUoMId(uomId);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(negativeAmount);
        line.setAmount(negativeAmount);
        daoProvider.daoFor(ErpSalInvoiceLine.class).saveEntity(line);
    }

    // ---------- helpers ----------

    /**
     * 领域非法迁移异常构造。{@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7：
     * Bean 报 common 码 + action/currentStatus/expectedStatus 元数据，Processor 映射领域码 +
     * 实体编号/上下文，common 码作 cause 保留）。领域 re-throw 仅传 {@code settlementId} + {@code currentStatus}
     * （action/expectedStatus 仅存于 common 码 cause，不向领域码传播）。
     */
    protected NopException illegalTransition(String settlementId, ErpCtRebateSettlement settlement, Throwable cause) {
        return new NopException(ErpCtErrors.ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION, cause)
                .param(ErpCtErrors.ARG_SETTLEMENT_ID, settlementId)
                .param(ErpCtErrors.ARG_CURRENT_STATUS, settlement.getStatus());
    }

    protected ErpCtRebateSettlement requireSettlement(String settlementId) {
        ErpCtRebateSettlement settlement = dao().getEntityById(settlementId);
        if (settlement == null) {
            throw new NopException(ErpCtErrors.ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION)
                    .param(ErpCtErrors.ARG_SETTLEMENT_ID, settlementId);
        }
        return settlement;
    }

    protected List<ErpCtRebateAccrual> findUnsettledAccruals(String agreementId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("rebateAgreementId", agreementId));
        q.addFilter(eq("isSettled", false));
        return daoProvider.daoFor(ErpCtRebateAccrual.class).findAllByQuery(q);
    }

    /**
     * 币种取自关联合同（协议无独立币种列；发票 CURRENCY_ID NOT NULL）。
     * 无关联合同时返回 null（由调用方确保关联存在）。
     */
    // bridge-main-049/051: ct String currencyId → pur/sal Long（退役 owner M2.5/M2.6）
    protected Long resolveCurrencyId(ErpCtRebateAgreement agreement) {
        if (agreement == null || agreement.getContractId() == null) {
            return null;
        }
        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class)
                .getEntityById(agreement.getContractId());
        return contract == null ? null : ConvertHelper.toLong(contract.getCurrencyId());
    }

    /**
     * 贷项行 materialId 取自关联合同首行（返利为金额型，无独立物料；发票行 MATERIAL_ID NOT NULL）。
     */
    @SuppressWarnings("unchecked")
    // bridge-main-050/052: ct String materialId → pur/sal Long（退役 owner M2.5/M2.6）
    protected Long resolveMaterialId(ErpCtRebateAgreement agreement) {
        if (agreement == null || agreement.getContractId() == null) {
            return null;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractId", agreement.getContractId()));
        q.setLimit(1);
        List<ErpCtContractLine> lines = daoProvider.daoFor(ErpCtContractLine.class).findAllByQuery(q);
        return lines.isEmpty() ? null : ConvertHelper.toLong(lines.get(0).getMaterialId());
    }

    /**
     * uoMId 取自主物料的默认计量单位（material.uoMId）。
     */
    // bridge-main-050/052: md String uoMId → pur/sal Long（退役 owner M2.5/M2.6）
    protected Long resolveUoMId(String materialId) {
        if (materialId == null) {
            return null;
        }
        app.erp.md.dao.entity.ErpMdMaterial material =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdMaterial.class).getEntityById(materialId);
        return material == null ? null : ConvertHelper.toLong(material.getUoMId());
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    protected IEntityDao<ErpCtRebateSettlement> dao() {
        return daoProvider.daoFor(ErpCtRebateSettlement.class);
    }
}
