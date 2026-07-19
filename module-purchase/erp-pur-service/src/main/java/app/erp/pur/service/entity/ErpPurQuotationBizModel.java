
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurQuotationBiz;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import app.erp.pur.dao.entity.ErpPurQuotation;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.SupplierEligibilityChecker;
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
 */
@BizModel("ErpPurQuotation")
public class ErpPurQuotationBizModel extends CrudBizModel<ErpPurQuotation> implements IErpPurQuotationBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPurQuotationBizModel.class);

    @Inject
    SupplierEligibilityChecker eligibilityChecker;

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
        quotation.setDocStatus(ErpPurDocStatus.DOC_STATUS_CANCELLED);
        updateEntity(quotation, null, context);
        return quotation;
    }
}
