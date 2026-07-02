package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinNotesDiscount;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 票据过账派发器（finance 自有域）。票据状态迁移后组装 {@link PostingEvent}（businessType + billHeadCode +
 * billData 含 PARTNER_ID/FACE_AMOUNT/贴现明细 + orgId + acctSchemaId）经 {@link FinPostingExecutor}
 * 调用过账引擎（{@code treasury.md §业财过账}）。
 *
 * <p>对齐 {@code ExpenseClaimPostingDispatcher} 的失败语义：过账失败吞异常保持原态+posted=false；
 * 本类为 Facade 编排层，不持久化源单据（posted 由调用方 BizModel 主事务内统一持久化）。
 */
public class NotesPostingDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(NotesPostingDispatcher.class);

    @Inject
    FinPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;

    public boolean tryPostReceivable(ErpFinNotesReceivable note, ErpFinBusinessType businessType) {
        PostingEvent event = buildReceivableEvent(note, businessType);
        return safePost(note.getCode(), event);
    }

    public boolean tryPostPayable(ErpFinNotesPayable note, ErpFinBusinessType businessType) {
        PostingEvent event = buildPayableEvent(note, businessType);
        return safePost(note.getCode(), event);
    }

    public void reverseReceivable(ErpFinNotesReceivable note, ErpFinBusinessType businessType) {
        executor.reverse(note.getCode(), businessType);
    }

    public void reversePayable(ErpFinNotesPayable note, ErpFinBusinessType businessType) {
        executor.reverse(note.getCode(), businessType);
    }

    private boolean safePost(String code, PostingEvent event) {
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("票据过账失败，单据 {} 保持原态、posted=false：{}", code, e.getMessage());
            } else {
                LOG.error("票据过账异常，单据 {} 保持原态、posted=false", code, e);
            }
            return false;
        }
    }

    private PostingEvent buildReceivableEvent(ErpFinNotesReceivable note, ErpFinBusinessType businessType) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(businessType);
        event.setBillHeadCode(note.getCode());
        event.setOrgId(note.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(note.getOrgId()));
        event.setCurrencyId(note.getCurrencyId());
        event.setExchangeRate(note.getExchangeRate() != null ? note.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = note.getIssueDate() != null ? note.getIssueDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpFinConstants.BILL_DATA_PARTNER_ID, note.getPartnerId());
        billData.put(ErpFinConstants.BILL_DATA_FACE_AMOUNT, nz(note.getAmountFunctional()));
        billData.put(ErpFinConstants.BILL_DATA_BUSINESS_DATE, note.getIssueDate());
        billData.put(ErpFinConstants.BILL_DATA_DUE_DATE, note.getDueDate());
        if (businessType == ErpFinBusinessType.NOTES_RECEIVABLE_DISCOUNTED) {
            ErpFinNotesDiscount discount = loadDiscount(note.getDiscountId());
            if (discount != null) {
                billData.put(ErpFinConstants.BILL_DATA_DISCOUNT_INTEREST, nz(discount.getDiscountInterest()));
                billData.put(ErpFinConstants.BILL_DATA_NET_AMOUNT, nz(discount.getNetAmount()));
                billData.put(ErpFinConstants.BILL_DATA_EXCHANGE_GAIN_LOSS, nz(discount.getExchangeGainLoss()));
                event.setVoucherDate(discount.getDiscountDate() != null ? discount.getDiscountDate() : voucherDate);
            }
        }
        event.setBillData(billData);
        return event;
    }

    private PostingEvent buildPayableEvent(ErpFinNotesPayable note, ErpFinBusinessType businessType) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(businessType);
        event.setBillHeadCode(note.getCode());
        event.setOrgId(note.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(note.getOrgId()));
        event.setCurrencyId(note.getCurrencyId());
        event.setExchangeRate(note.getExchangeRate() != null ? note.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = note.getIssueDate() != null ? note.getIssueDate() : CoreMetrics.today();
        // 兑付以到期日为凭证日（资金实际流出日）。
        if (businessType == ErpFinBusinessType.NOTES_PAYABLE_HONORED && note.getDueDate() != null) {
            voucherDate = note.getDueDate();
        }
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpFinConstants.BILL_DATA_PARTNER_ID, note.getPartnerId());
        billData.put(ErpFinConstants.BILL_DATA_FACE_AMOUNT, nz(note.getAmountFunctional()));
        billData.put(ErpFinConstants.BILL_DATA_BUSINESS_DATE, note.getIssueDate());
        billData.put(ErpFinConstants.BILL_DATA_DUE_DATE, note.getDueDate());
        event.setBillData(billData);
        return event;
    }

    private ErpFinNotesDiscount loadDiscount(Long discountId) {
        if (discountId == null) {
            return null;
        }
        IEntityDao<ErpFinNotesDiscount> dao = daoProvider.daoFor(ErpFinNotesDiscount.class);
        return dao.getEntityById(discountId);
    }

    private Long resolveAcctSchemaId(Long orgId) {
        if (orgId == null) {
            return null;
        }
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("orgId", orgId));
        q.setLimit(1);
        List<ErpMdAcctSchema> schemas = dao.findAllByQuery(q);
        return schemas.isEmpty() ? null : schemas.get(0).getId();
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
