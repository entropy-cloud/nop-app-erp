package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.dao.entity.ErpAstMergeLine;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产合并过账派发器（仅 post 路径，无 reverse——遵守 owner doc {@code split-merge.md} §关键业务规则 5 不可逆契约）。
 *
 * <p>组装 {@link PostingEvent}(ASSET_MERGE) 的 billData 为结构化借/贷明细：
 * <ul>
 *   <li>借：固定资产（新卡片 categoryId subjectId，合计原值，单行）。</li>
 *   <li>贷：固定资产（按 MergeLine.sourceAssetId × 各源 categoryId subjectId 拆 N 行，合计原值）。</li>
 * </ul>
 */
public class AssetMergePostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(AssetMergePostingDispatcher.class);

    @Inject
    AssetPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    public boolean tryPost(ErpAstMerge merge, List<ErpAstAsset> sources, List<ErpAstMergeLine> lines,
                           ErpAstAsset target) {
        PostingEvent event = buildEvent(merge, sources, lines, target);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("资产合并过账失败，合并单 {} 保持 posted=false：{}", merge.getCode(), e.getMessage());
            } else {
                LOG.error("资产合并过账异常，合并单 {} 保持 posted=false", merge.getCode(), e);
            }
            return false;
        }
    }

    private PostingEvent buildEvent(ErpAstMerge merge, List<ErpAstAsset> sources, List<ErpAstMergeLine> lines,
                                    ErpAstAsset target) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.ASSET_MERGE);
        event.setBillHeadCode(merge.getCode());
        Long orgId = merge.getOrgId() != null ? merge.getOrgId()
                : (target != null ? target.getOrgId() : (sources.isEmpty() ? null : sources.get(0).getOrgId()));
        event.setOrgId(orgId);
        event.setAcctSchemaId(resolveAcctSchemaId(orgId));
        Long currencyId = merge.getCurrencyId() != null ? merge.getCurrencyId()
                : (target != null ? target.getCurrencyId() : (sources.isEmpty() ? null : sources.get(0).getCurrencyId()));
        event.setCurrencyId(currencyId);
        event.setExchangeRate(merge.getExchangeRate() != null ? merge.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = merge.getBusinessDate() != null ? merge.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        // 借方单行：新卡片类别 subjectId，合计原值
        ErpAstAssetCategory targetCategory = loadCategory(target != null ? target.getCategoryId() : null);
        String debitSubject = resolveSubjectCode(
                targetCategory != null ? targetCategory.getSubjectId() : null, "1601");
        BigDecimal totalOriginal = BigDecimal.ZERO;
        for (ErpAstAsset src : sources) {
            totalOriginal = totalOriginal.add(nz(src.getOriginalValue()));
        }
        List<Map<String, Object>> debitLines = new ArrayList<>();
        debitLines.add(lineMap(debitSubject, "固定资产", totalOriginal,
                target != null ? target.getCode() : merge.getCode()));

        // 贷方明细：按源资产各自类别 subjectId 拆 N 行
        List<Map<String, Object>> creditLines = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            ErpAstAsset src = sources.get(i);
            ErpAstAssetCategory category = loadCategory(src.getCategoryId());
            String subjectCode = resolveSubjectCode(category != null ? category.getSubjectId() : null, "1601");
            creditLines.add(lineMap(subjectCode, "固定资产", nz(src.getOriginalValue()), src.getCode()));
        }

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpAstConstants.BILL_DATA_DEBIT_LINES, debitLines);
        billData.put(ErpAstConstants.BILL_DATA_CREDIT_LINES, creditLines);
        event.setBillData(billData);
        return event;
    }

    private ErpAstAssetCategory loadCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(categoryId);
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

    private static Map<String, Object> lineMap(String subjectCode, String subjectName, BigDecimal amount, String memo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(ErpAstConstants.BILL_DATA_LINE_SUBJECT_CODE, subjectCode);
        m.put(ErpAstConstants.BILL_DATA_LINE_SUBJECT_NAME, subjectName);
        m.put(ErpAstConstants.BILL_DATA_LINE_AMOUNT, amount);
        m.put(ErpAstConstants.BILL_DATA_LINE_MEMO, memo);
        return m;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
