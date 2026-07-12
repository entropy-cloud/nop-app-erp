package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstMaintenance;
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
 * 维修费用化过账派发器（UC-AST-10）。维修工单 EXPENSE 路径 post 时组装 {@link PostingEvent}(MAINTENANCE_EXPENSE)
 * 经 {@link AssetPostingExecutor} 调用财务过账引擎。billHeadCode = 维修单 code，作为幂等/红冲键。
 *
 * <p>billData 汇总费用合计 + 科目映射 + 关联维护工单标志（防双重扣减分支）；
 * Provider 按分支科目分解（镜像 {@code ValueAdjustmentPostingDispatcher} 范式）。
 */
public class MaintenanceExpensePostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceExpensePostingDispatcher.class);

    @Inject
    AssetPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    public Long tryPost(ErpAstMaintenance maintenance, ErpAstAsset asset, ErpAstAssetCategory category) {
        PostingEvent event = buildEvent(maintenance, asset, category);
        try {
            return executor.postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("维修费用化过账失败，维修单 {} 保持 posted=false：{}", maintenance.getCode(), e.getMessage());
            } else {
                LOG.error("维修费用化过账异常，维修单 {} 保持 posted=false", maintenance.getCode(), e);
            }
            return null;
        }
    }

    public void reverse(ErpAstMaintenance maintenance) {
        try {
            executor.reverse(maintenance.getCode(), ErpFinBusinessType.MAINTENANCE_EXPENSE);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("维修费用化红字冲销失败，维修单 {}：{}", maintenance.getCode(), e.getMessage());
            } else {
                LOG.error("维修费用化红字冲销异常，维修单 {}", maintenance.getCode(), e);
            }
            throw e;
        }
    }

    private PostingEvent buildEvent(ErpAstMaintenance maintenance, ErpAstAsset asset, ErpAstAssetCategory category) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.MAINTENANCE_EXPENSE);
        event.setBillHeadCode(maintenance.getCode());
        event.setOrgId(maintenance.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(maintenance.getOrgId()));
        event.setCurrencyId(maintenance.getCurrencyId());
        event.setExchangeRate(maintenance.getExchangeRate() != null ? maintenance.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = maintenance.getBusinessDate() != null ? maintenance.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        boolean linkedVisit = maintenance.getMaintenanceVisitId() != null;
        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_TREATMENT, ErpAstConstants.MAINTENANCE_TREATMENT_EXPENSE);
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_TOTAL_COST, nz(maintenance.getTotalCostAmount()));
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_LINKED_VISIT, linkedVisit);
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_EXPENSE_SUBJECT_CODE, "6602");
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_CLEARING_SUBJECT_CODE, "2502");
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_BANK_SUBJECT_CODE, "1002");
        billData.put(ErpAstConstants.BILL_DATA_MAINTENANCE_INVENTORY_SUBJECT_CODE, "1403");
        billData.put(ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                resolveSubjectCode(category != null ? category.getSubjectId() : null, "1601"));
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
