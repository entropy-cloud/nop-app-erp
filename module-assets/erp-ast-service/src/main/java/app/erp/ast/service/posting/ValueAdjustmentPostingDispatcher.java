package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.ErpAstConstants;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 价值调整过账派发器。调整单 APPROVED 后组装 {@link PostingEvent}(VALUE_ADJUSTMENT)
 * 经 {@link AssetPostingExecutor} 调用财务过账引擎。billHeadCode = 调整单 code，作为幂等/红冲键。
 *
 * <p>失败语义对齐 disposal/capitalization：过账失败吞异常返回 null（保持 posted=false），不阻塞调整终态。
 */
public class ValueAdjustmentPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ValueAdjustmentPostingDispatcher.class);

    @Inject
    AssetPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    public Long tryPost(ErpAstValueAdjustment adjustment, ErpAstAsset asset, ErpAstAssetCategory category) {
        PostingEvent event = buildEvent(adjustment, asset, category);
        try {
            return executor.postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("价值调整过账失败，调整单 {} 保持 posted=false：{}", adjustment.getCode(), e.getMessage());
            } else {
                LOG.error("价值调整过账异常，调整单 {} 保持 posted=false", adjustment.getCode(), e);
            }
            return null;
        }
    }

    public void reverse(ErpAstValueAdjustment adjustment) {
        try {
            executor.reverse(adjustment.getCode(), ErpFinBusinessType.VALUE_ADJUSTMENT);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("价值调整红字冲销失败，调整单 {}：{}", adjustment.getCode(), e.getMessage());
            } else {
                LOG.error("价值调整红字冲销异常，调整单 {}", adjustment.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpAstValueAdjustment adjustment, ErpAstAsset asset,
                                    ErpAstAssetCategory category) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.VALUE_ADJUSTMENT);
        event.setBillHeadCode(adjustment.getCode());
        event.setOrgId(adjustment.getOrgId() != null ? adjustment.getOrgId() : asset.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));
        event.setCurrencyId(adjustment.getCurrencyId() != null ? adjustment.getCurrencyId() : asset.getCurrencyId());
        event.setExchangeRate(adjustment.getExchangeRate() != null ? adjustment.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = adjustment.getBusinessDate() != null ? adjustment.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpAstConstants.BILL_DATA_ADJUSTMENT_TYPE, adjustment.getAdjustmentType());
        billData.put(ErpAstConstants.BILL_DATA_ADJUSTMENT_AMOUNT, nz(adjustment.getAdjustmentAmount()));
        billData.put(ErpAstConstants.BILL_DATA_ASSET_ID, asset.getId());
        billData.put(ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getSubjectId() : null, "1601"));
        billData.put(ErpAstConstants.BILL_DATA_IMPAIRMENT_LOSS_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getExpenseSubjectId() : null, "6702"));
        billData.put(ErpAstConstants.BILL_DATA_IMPAIRMENT_PROVISION_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getDepreciationSubjectId() : null, "1604"));
        billData.put(ErpAstConstants.BILL_DATA_CAPITAL_RESERVE_SUBJECT_CODE, "4002");
        event.setBillData(billData);
        return event;
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    private String resolveSubjectCode(Long subjectId, String defaultCode) {
        if (subjectId == null) {
            return defaultCode;
        }
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = dao.getEntityById(subjectId);
        if (subject == null || subject.getCode() == null || subject.getCode().trim().isEmpty()) {
            return defaultCode;
        }
        return subject.getCode().trim();
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
