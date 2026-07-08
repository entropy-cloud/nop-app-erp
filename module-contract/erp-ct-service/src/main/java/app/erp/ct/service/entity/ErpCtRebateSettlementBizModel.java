
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtRebateAccrual;
import app.erp.contract.dao.entity.ErpCtRebateAgreement;
import app.erp.contract.dao.entity.ErpCtRebateSettlement;
import app.erp.ct.biz.IErpCtRebateSettlementBiz;
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
import io.nop.biz.crud.EntityData;

/**
 * 返利结算单 BizModel。结算过账 + 贷项凭证生成
 * （对齐 {@code docs/design/contract/volume-discount.md} §返利信用单 / §结算流程）。
 *
 * <p>{@code postSettlement}：DRAFT → POSTED，汇总关联未结算计提 → 生成贷项凭证（Phase 1 Decision：
 * PURCHASE→AP 负额发票，SALES→AR 负额发票）→ 标记计提 {@code isSettled=true}。
 *
 * <p><b>跨实体访问方式偏离说明</b>：贷项凭证（负额发票）经 {@link IDaoProvider} 直接持久化，
 * 而非注入 {@code IErpPurInvoiceBiz}/{@code IErpSalInvoiceBiz}（同 InvoicePlan，避免服务依赖级联）。
 */
@BizModel("ErpCtRebateSettlement")
public class ErpCtRebateSettlementBizModel extends CrudBizModel<ErpCtRebateSettlement>
        implements IErpCtRebateSettlementBiz {

    public ErpCtRebateSettlementBizModel() {
        setEntityName(ErpCtRebateSettlement.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCtRebateSettlement> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpCtRebateSettlement entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }


    @Override
    @BizMutation
    public ErpCtRebateSettlement postSettlement(@Name("settlementId") Long settlementId, IServiceContext context) {
        ErpCtRebateSettlement settlement = requireSettlement(settlementId, context);
        if (!Objects.equals(settlement.getStatus(), ErpCtConstants.SETTLEMENT_STATUS_DRAFT)) {
            throw new NopException(ErpCtErrors.ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION)
                    .param(ErpCtErrors.ARG_SETTLEMENT_ID, settlementId)
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, settlement.getStatus());
        }

        // 汇总关联未结算计提
        List<ErpCtRebateAccrual> unsettled = findUnsettledAccruals(settlement.getRebateAgreementId());
        BigDecimal total = BigDecimal.ZERO;
        for (ErpCtRebateAccrual a : unsettled) {
            total = total.add(nz(a.getAccruedRebate()));
        }

        ErpCtRebateAgreement agreement = daoProvider().daoFor(ErpCtRebateAgreement.class)
                .getEntityById(settlement.getRebateAgreementId());

        // 币种取自关联合同（发票 CURRENCY_ID NOT NULL）
        Long currencyId = resolveCurrencyId(agreement);
        // 贷项行 materialId/uoMId 取自关联合同首行及其主物料（返利为金额型；发票行 MATERIAL_ID/UO_M_ID NOT NULL）
        Long materialId = resolveMaterialId(agreement);
        Long uomId = resolveUoMId(materialId);

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
        IEntityDao<ErpCtRebateAccrual> accrualDao = daoProvider().daoFor(ErpCtRebateAccrual.class);
        for (ErpCtRebateAccrual a : unsettled) {
            a.setIsSettled(true);
            a.setSettledDate(today);
            accrualDao.updateEntity(a);
        }

        // 结算单过账
        settlement.setTotalRebateAmount(total);
        settlement.setStatus(ErpCtConstants.SETTLEMENT_STATUS_POSTED);
        settlement.setPostedAt(CoreMetrics.currentDateTime());
        settlement.setPostedBy(currentUserId());
        updateEntity(settlement, null, context);
        return settlement;
    }

    // ---------- 贷项凭证生成（负额发票，经 IDaoProvider 直接持久化） ----------
    // O-4 架构豁免：返利结算生成的贷项凭证（负额发票）跨模块直接持久化，不走采购/销售域审批管道。
    // 理由/风险/补偿见 docs/architecture/posting-exemptions.md §ErpCtRebateSettlementBizModel

    protected void createNegativeApInvoice(String code, ErpCtRebateAgreement agreement,
                                           Long currencyId, Long materialId, Long uomId, BigDecimal negativeAmount) {
        IEntityDao<ErpPurInvoice> dao = daoProvider().daoFor(ErpPurInvoice.class);
        ErpPurInvoice invoice = dao.newEntity();
        invoice.setCode(code);
        if (agreement.getOrgId() != null) {
            invoice.setOrgId(agreement.getOrgId());
        }
        invoice.setSupplierId(agreement.getPartnerId());
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

        ErpPurInvoiceLine line = daoProvider().daoFor(ErpPurInvoiceLine.class).newEntity();
        line.setInvoiceId(invoice.getId());
        line.setLineNo(1);
        line.setMaterialId(materialId);
        line.setUoMId(uomId);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(negativeAmount);
        line.setAmount(negativeAmount);
        daoProvider().daoFor(ErpPurInvoiceLine.class).saveEntity(line);
    }

    protected void createNegativeArInvoice(String code, ErpCtRebateAgreement agreement,
                                           Long currencyId, Long materialId, Long uomId, BigDecimal negativeAmount) {
        IEntityDao<ErpSalInvoice> dao = daoProvider().daoFor(ErpSalInvoice.class);
        ErpSalInvoice invoice = dao.newEntity();
        invoice.setCode(code);
        if (agreement.getOrgId() != null) {
            invoice.setOrgId(agreement.getOrgId());
        }
        invoice.setCustomerId(agreement.getPartnerId());
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

        ErpSalInvoiceLine line = daoProvider().daoFor(ErpSalInvoiceLine.class).newEntity();
        line.setInvoiceId(invoice.getId());
        line.setLineNo(1);
        line.setMaterialId(materialId);
        line.setUoMId(uomId);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(negativeAmount);
        line.setAmount(negativeAmount);
        daoProvider().daoFor(ErpSalInvoiceLine.class).saveEntity(line);
    }

    // ---------- helpers ----------

    protected ErpCtRebateSettlement requireSettlement(Long settlementId, IServiceContext context) {
        ErpCtRebateSettlement settlement = get(String.valueOf(settlementId), false, context);
        if (settlement == null) {
            throw new NopException(ErpCtErrors.ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION)
                    .param(ErpCtErrors.ARG_SETTLEMENT_ID, settlementId);
        }
        return settlement;
    }

    protected List<ErpCtRebateAccrual> findUnsettledAccruals(Long agreementId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("rebateAgreementId", agreementId));
        q.addFilter(eq("isSettled", false));
        return daoProvider().daoFor(ErpCtRebateAccrual.class).findAllByQuery(q);
    }

    /**
     * 币种取自关联合同（协议无独立币种列；发票 CURRENCY_ID NOT NULL）。
     * 无关联合同时返回 null（由调用方确保关联存在）。
     */
    protected Long resolveCurrencyId(ErpCtRebateAgreement agreement) {
        if (agreement == null || agreement.getContractId() == null) {
            return null;
        }
        ErpCtContract contract = daoProvider().daoFor(ErpCtContract.class)
                .getEntityById(agreement.getContractId());
        return contract == null ? null : contract.getCurrencyId();
    }

    /**
     * 贷项行 materialId 取自关联合同首行（返利为金额型，无独立物料；发票行 MATERIAL_ID NOT NULL）。
     */
    @SuppressWarnings("unchecked")
    protected Long resolveMaterialId(ErpCtRebateAgreement agreement) {
        if (agreement == null || agreement.getContractId() == null) {
            return null;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractId", agreement.getContractId()));
        q.setLimit(1);
        List<ErpCtContractLine> lines = daoProvider().daoFor(ErpCtContractLine.class).findAllByQuery(q);
        return lines.isEmpty() ? null : lines.get(0).getMaterialId();
    }

    /**
     * uoMId 取自主物料的默认计量单位（material.uoMId）。
     */
    protected Long resolveUoMId(Long materialId) {
        if (materialId == null) {
            return null;
        }
        app.erp.md.dao.entity.ErpMdMaterial material =
                daoProvider().daoFor(app.erp.md.dao.entity.ErpMdMaterial.class).getEntityById(materialId);
        return material == null ? null : material.getUoMId();
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
}
