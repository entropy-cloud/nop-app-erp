package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.ErpAstConstants;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
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
 * 处置过账派发器。处置单 APPROVED 后（清理损益计算 + 资产终态 + 后续折旧取消之后）组装 {@link PostingEvent}(DISPOSAL)
 * 经 {@link AssetPostingExecutor} 调用财务过账引擎。billHeadCode = 处置单 code，作为幂等/红冲键。
 *
 * <p>失败语义对齐 sales/inventory：过账失败吞异常返回 null（保持 posted=false），不阻塞处置终态。
 */
public class DisposalPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(DisposalPostingDispatcher.class);

    @Inject
    AssetPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    public Long tryPost(ErpAstDisposal disposal, ErpAstAsset asset, ErpAstAssetCategory category) {
        PostingEvent event = buildEvent(disposal, asset, category);
        try {
            return executor.postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("处置过账失败，处置单 {} 保持 posted=false：{}", disposal.getCode(), e.getMessage());
            } else {
                LOG.error("处置过账异常，处置单 {} 保持 posted=false", disposal.getCode(), e);
            }
            return null;
        }
    }

    public void reverse(ErpAstDisposal disposal) {
        try {
            executor.reverse(disposal.getCode(), ErpFinBusinessType.DISPOSAL);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("处置红字冲销失败，处置单 {}：{}", disposal.getCode(), e.getMessage());
            } else {
                LOG.error("处置红字冲销异常，处置单 {}", disposal.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpAstDisposal disposal, ErpAstAsset asset, ErpAstAssetCategory category) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.DISPOSAL);
        event.setBillHeadCode(disposal.getCode());
        event.setOrgId(disposal.getOrgId() != null ? disposal.getOrgId() : asset.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));
        event.setCurrencyId(disposal.getCurrencyId() != null ? disposal.getCurrencyId() : asset.getCurrencyId());
        event.setExchangeRate(disposal.getExchangeRate() != null ? disposal.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = disposal.getBusinessDate() != null ? disposal.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        BigDecimal original = nz(asset.getOriginalValue());
        BigDecimal accumDep = nz(asset.getAccumulatedDepreciation());
        BigDecimal disposalAmount = nz(disposal.getDisposalAmount());
        BigDecimal nbv = original.subtract(accumDep);
        BigDecimal gainLoss = disposalAmount.subtract(nbv);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpAstConstants.BILL_DATA_ORIGINAL_VALUE, original);
        billData.put(ErpAstConstants.BILL_DATA_ACCUMULATED_DEPRECIATION, accumDep);
        billData.put(ErpAstConstants.BILL_DATA_DISPOSAL_AMOUNT, disposalAmount);
        billData.put(ErpAstConstants.BILL_DATA_GAIN_LOSS, gainLoss);
        billData.put(ErpAstConstants.BILL_DATA_DISPOSAL_TYPE, disposal.getDisposalType());
        billData.put(ErpAstConstants.BILL_DATA_ASSET_ID, asset.getId());
        billData.put(ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getSubjectId() : null, "1601"));
        billData.put(ErpAstConstants.BILL_DATA_ACCUM_DEPRE_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getDepreciationSubjectId() : null, "1602"));
        billData.put(ErpAstConstants.BILL_DATA_DISPOSAL_GAINLOSS_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getDisposalGainLossSubjectId() : null, "6711"));
        event.setBillData(billData);
        return event;
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
