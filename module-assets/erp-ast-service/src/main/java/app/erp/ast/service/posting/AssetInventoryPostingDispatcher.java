package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstInventory;
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
 * 资产盘点差异过账派发器。盘点单 POSTED 时组装 {@link PostingEvent}(ASSET_INVENTORY_ADJUSTMENT)
 * 经 {@link AssetPostingExecutor} 调用财务过账引擎。billHeadCode = 盘点单 code，作为幂等/红冲键。
 *
 * <p>billData 汇总盘盈/盘亏合计金额 + 类别科目映射；Provider 按差异分支科目分解（镜像
 * {@code ValueAdjustmentPostingDispatcher} 范式）。
 */
public class AssetInventoryPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(AssetInventoryPostingDispatcher.class);

    @Inject
    AssetPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    public Long tryPost(ErpAstInventory inventory) {
        PostingEvent event = buildEvent(inventory);
        try {
            return executor.postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("盘点过账失败，盘点单 {} 保持 posted=false：{}", inventory.getCode(), e.getMessage());
            } else {
                LOG.error("盘点过账异常，盘点单 {} 保持 posted=false", inventory.getCode(), e);
            }
            return null;
        }
    }

    public void reverse(ErpAstInventory inventory) {
        try {
            executor.reverse(inventory.getCode(), ErpFinBusinessType.ASSET_INVENTORY_ADJUSTMENT);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("盘点红字冲销失败，盘点单 {}：{}", inventory.getCode(), e.getMessage());
            } else {
                LOG.error("盘点红字冲销异常，盘点单 {}", inventory.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpAstInventory inventory) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.ASSET_INVENTORY_ADJUSTMENT);
        event.setBillHeadCode(inventory.getCode());
        event.setOrgId(inventory.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(inventory.getOrgId()));
        event.setCurrencyId(inventory.getCurrencyId());
        event.setExchangeRate(inventory.getExchangeRate() != null ? inventory.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = inventory.getBusinessDate() != null ? inventory.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        ErpAstAssetCategory category = resolveRangeCategory(inventory.getRangeCategoryId());
        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpAstConstants.BILL_DATA_INVENTORY_SURPLUS_AMOUNT, nz(inventory.getSurplusAmount()));
        billData.put(ErpAstConstants.BILL_DATA_INVENTORY_SHORTAGE_AMOUNT, nz(inventory.getShortageAmount()));
        billData.put(ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getSubjectId() : null, "1601"));
        billData.put(ErpAstConstants.BILL_DATA_NON_OPERATING_INCOME_SUBJECT_CODE, "6301");
        billData.put(ErpAstConstants.BILL_DATA_NON_OPERATING_EXPENSE_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getDisposalGainLossSubjectId() : null, "6711"));
        event.setBillData(billData);
        return event;
    }

    private ErpAstAssetCategory resolveRangeCategory(Long rangeCategoryId) {
        if (rangeCategoryId == null) {
            return null;
        }
        return daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(rangeCategoryId);
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
