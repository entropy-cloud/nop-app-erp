package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
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
import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资本化过账派发器。资本化单 APPROVED 后（建卡/折旧计划生成之后）组装 {@link PostingEvent}(CAPITALIZATION)
 * 经 {@link AssetPostingExecutor}（独立新事务由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW}
 * 承接）调用财务过账引擎。
 *
 * <p>对齐 sales/inventory 失败语义：过账失败吞异常记日志、保持 APPROVED+{@code posted=false}
 * （由 Deferred 兜底扫描重试），不阻塞终态。本类为 Facade 编排层，不持久化源单据——源单据 {@code posted}
 * 标志由调用方 BizModel 在主事务内统一持久化。
 */
public class CapitalizationPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(CapitalizationPostingDispatcher.class);

    @Inject
    AssetPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    /**
     * 资本化审核通过后调用。成功返回 true（调用方据此置 posted=true）；失败吞异常返回 false（保持 posted=false）。
     */
    public boolean tryPost(ErpAstAssetCapitalization cap) {
        PostingEvent event = buildEvent(cap);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("资本化过账失败，资本化单 {} 保持 APPROVED、posted=false：{}", cap.getCode(), e.getMessage());
            } else {
                LOG.error("资本化过账异常，资本化单 {} 保持 APPROVED、posted=false", cap.getCode(), e);
            }
            return false;
        }
    }

    /**
     * 反审核前红字冲销已过账凭证（对齐 posting.md §冲销）。冲销是硬前置，失败向上抛出阻断状态迁移。
     */
    public void reverse(ErpAstAssetCapitalization cap) {
        try {
            executor.reverse(cap.getCode(), ErpFinBusinessType.CAPITALIZATION);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("资本化红字冲销失败，资本化单 {}：{}", cap.getCode(), e.getMessage());
            } else {
                LOG.error("资本化红字冲销异常，资本化单 {}", cap.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpAstAssetCapitalization cap) {
        ErpAstAssetCategory category = cap.getCategoryId() == null ? null : loadCategory(cap.getCategoryId());

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.CAPITALIZATION);
        event.setBillHeadCode(cap.getCode());
        event.setOrgId(cap.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(cap.getOrgId()));
        event.setCurrencyId(cap.getCurrencyId());
        event.setExchangeRate(cap.getExchangeRate() != null ? cap.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = cap.getCapitalizationDate() != null ? cap.getCapitalizationDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpAstConstants.BILL_DATA_ORIGINAL_VALUE, nz(cap.getOriginalValue()));
        billData.put(ErpAstConstants.BILL_DATA_SOURCE_TYPE, cap.getSourceType());
        billData.put(ErpAstConstants.BILL_DATA_CATEGORY_ID, cap.getCategoryId());
        billData.put(ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getSubjectId() : null, "1601"));
        String sourceType = cap.getSourceType();
        String defaultCredit = Objects.equals(sourceType, ErpAstConstants.SOURCE_TYPE_CIP) ? "1603" : "1002";
        billData.put(ErpAstConstants.BILL_DATA_CREDIT_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getCipSubjectId() : null, defaultCredit));
        event.setBillData(billData);
        return event;
    }

    private ErpAstAssetCategory loadCategory(Long categoryId) {
        // 资本化驱动同模块聚合读取（对齐 ExpenseClaimBizModel.loadItem 经 daoProvider 范式）
        IEntityDao<ErpAstAssetCategory> dao = daoProvider.daoFor(ErpAstAssetCategory.class);
        return dao.getEntityById(categoryId);
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
