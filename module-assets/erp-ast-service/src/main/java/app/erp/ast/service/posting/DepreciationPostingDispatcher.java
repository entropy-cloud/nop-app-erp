package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
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
 * 折旧过账派发器。折旧执行后（计划条目与资产卡片汇总列同事务确立之后）组装 {@link PostingEvent}(DEPRECIATION)
 * 经 {@link AssetPostingExecutor} 调用财务过账引擎。billHeadCode = 资产编码#期间，作为幂等/红冲键。
 *
 * <p>失败语义对齐 sales/inventory：过账失败吞异常返回 null（保持 posted=false），不阻塞折旧终态。
 * 反向（reverse）为硬前置，失败向上抛出。本类不持久化源单据——{@code posted}/voucherId 由调用方 BizModel 持久化。
 */
public class DepreciationPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(DepreciationPostingDispatcher.class);

    @Inject
    AssetPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    /**
     * 折旧执行后调用。成功返回 voucherId（调用方据此置 posted=true + voucherId）；失败返回 null（保持 posted=false）。
     */
    public Long tryPost(ErpAstDepreciationSchedule schedule, ErpAstAsset asset, ErpAstAssetCategory category) {
        PostingEvent event = buildEvent(schedule, asset, category);
        try {
            return executor.postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("折旧过账失败，资产 {} 期间 {} 保持 posted=false：{}",
                        asset.getCode(), schedule.getPeriod(), e.getMessage());
            } else {
                LOG.error("折旧过账异常，资产 {} 期间 {} 保持 posted=false",
                        asset.getCode(), schedule.getPeriod(), e);
            }
            return null;
        }
    }

    /**
     * 幂等重执行前红字冲销已过账折旧凭证（对齐 §5.1）。冲销是硬前置，失败向上抛出。
     */
    public void reverse(ErpAstAsset asset, String period) {
        try {
            executor.reverse(billHeadCode(asset.getCode(), period), ErpFinBusinessType.DEPRECIATION);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("折旧红字冲销失败，资产 {} 期间 {}：{}", asset.getCode(), period, e.getMessage());
            } else {
                LOG.error("折旧红字冲销异常，资产 {} 期间 {}", asset.getCode(), period, e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpAstDepreciationSchedule schedule, ErpAstAsset asset,
                                    ErpAstAssetCategory category) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.DEPRECIATION);
        event.setBillHeadCode(billHeadCode(asset.getCode(), schedule.getPeriod()));
        event.setOrgId(schedule.getOrgId() != null ? schedule.getOrgId() : asset.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));
        event.setCurrencyId(asset.getCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = schedule.getBusinessDate() != null ? schedule.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpAstConstants.BILL_DATA_DEPRECIATION_AMOUNT, nz(schedule.getActualAmount()));
        billData.put(ErpAstConstants.BILL_DATA_ASSET_ID, asset.getId());
        billData.put(ErpAstConstants.BILL_DATA_CATEGORY_ID, asset.getCategoryId());
        billData.put(ErpAstConstants.BILL_DATA_DEPARTMENT_ID, asset.getDepartmentId());
        billData.put(ErpAstConstants.BILL_DATA_PERIOD, schedule.getPeriod());
        billData.put(ErpAstConstants.BILL_DATA_EXPENSE_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getExpenseSubjectId() : null, "6602"));
        billData.put(ErpAstConstants.BILL_DATA_ACCUM_DEPRE_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getDepreciationSubjectId() : null, "1602"));
        event.setBillData(billData);
        return event;
    }

    static String billHeadCode(String assetCode, String period) {
        return assetCode + "#" + period;
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
