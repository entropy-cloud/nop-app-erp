
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurQuotationBiz;
import app.erp.pur.dao.entity.ErpPurQuotation;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.SupplierEligibilityChecker;
import app.erp.pur.service.statemachine.ErpPurQuotationDocumentStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 供应商报价单 BizModel。报价单是供应商参与询价（RFQ）的入口——供应商经报价单成为 RFQ 收件人，
 * 故 AVL 准入与评分 standing 联动校验落在报价单保存前置钩子
 * （{@code docs/design/purchase/supplier-evaluation.md §业务规则3/5}）。
 *
 * <p>设计偏离补注：RFQ 头 {@code ErpPurRfq} 无 supplierId（一份询价发往多个供应商），
 * 「RFQ 创建校验」的供应商落点为报价单（supplier 参与点）。设计意图（SUSPENDED/RED 供应商不可参与询价）不变。
 *
 * <p>{@link #defaultPrepareSave} 委托 {@link SupplierEligibilityChecker}：
 * PREVENT → 抛 {@link ErpPurErrors#ERR_SUPPLIER_NOT_APPROVED}；WARN（YELLOW）→ 仅记录日志提示，不阻止保存。
 *
 * <p><b>docStatus cancel 守卫（plan 2026-08-12-0918-1 Phase 3 Fix）</b>：{@link #cancel} 经
 * {@link ErpPurQuotationDocumentStateMachine} 断言来源态合法（owner doc §2「非已作废」守卫）。
 * 迁移前 cancel 无守卫（允许幂等 CANCELLED→CANCELLED）→ 经层 2 四方对照裁定为 implementation drift → Fix：
 * 已作废报价单再 cancel 抛领域码 {@link ErpPurErrors#ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION}。
 */
@BizModel("ErpPurQuotation")
public class ErpPurQuotationBizModel extends CrudBizModel<ErpPurQuotation> implements IErpPurQuotationBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPurQuotationBizModel.class);

    @Inject
    SupplierEligibilityChecker eligibilityChecker;

    @Inject
    ErpPurQuotationDocumentStateMachine stateMachine;

    public ErpPurQuotationBizModel() {
        setEntityName(ErpPurQuotation.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpPurQuotation> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpPurQuotation quotation = entityData.getEntity();
        if (quotation == null || quotation.getSupplierId() == null) {
            return;
        }
        SupplierEligibilityChecker.Decision decision = eligibilityChecker.check(quotation.getSupplierId(), context);
        if (decision == SupplierEligibilityChecker.Decision.PREVENT) {
            throw new NopException(ErpPurErrors.ERR_SUPPLIER_NOT_APPROVED)
                    .param(ErpPurErrors.ARG_PARTNER_ID, quotation.getSupplierId())
                    .param(ErpPurErrors.ARG_STANDING, "SUSPENDED/REJECTED/RED");
        }
        if (decision == SupplierEligibilityChecker.Decision.WARN) {
            LOG.warn("供应商 {} 近期评分偏低（YELLOW），请关注其交付/质量表现", quotation.getSupplierId());
        }
    }

    @Override
    @BizMutation
    public ErpPurQuotation cancel(@Name("quotationId") Long quotationId, IServiceContext context) {
        ErpPurQuotation quotation = requireEntity(String.valueOf(quotationId), null, context);
        try {
            stateMachine.assertCanCancel(quotation.getDocStatus());
        } catch (NopException e) {
            throw new NopException(ErpPurErrors.ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION)
                    .param(ErpPurErrors.ARG_QUOTATION_CODE, quotation.getCode())
                    .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, quotation.getDocStatus())
                    .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, "非已作废");
        }
        quotation.setDocStatus(stateMachine.cancelTargetStatus());
        updateEntity(quotation, null, context);
        return quotation;
    }
}
