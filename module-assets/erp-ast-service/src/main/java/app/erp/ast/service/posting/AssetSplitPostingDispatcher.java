package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstSplit;
import app.erp.ast.dao.entity.ErpAstSplitLine;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产拆分过账派发器（仅 post 路径，无 reverse——遵守 owner doc {@code split-merge.md} §关键业务规则 5 不可逆契约）。
 *
 * <p>组装 {@link PostingEvent}(ASSET_SPLIT) 的 billData 为结构化借/贷明细：
 * <ul>
 *   <li>借：固定资产（按 SplitLine.targetAssetId × 各行类别 subjectId 拆 N 行，每行原值金额）。</li>
 *   <li>贷：固定资产（源资产 categoryId subjectId，合计原值）。</li>
 * </ul>
 * 由 {@link AssetPostingExecutor}（独立新事务由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW} 承接）
 * 调用财务过账引擎。失败吞异常返回 false（保持 posted=false），不阻塞终态。
 */
public class AssetSplitPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(AssetSplitPostingDispatcher.class);

    @Inject
    AssetPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    public boolean tryPost(ErpAstSplit split, ErpAstAsset source, List<ErpAstSplitLine> lines,
                           List<ErpAstAsset> targets) {
        PostingEvent event = buildEvent(split, source, lines, targets);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("资产拆分过账失败，拆分单 {} 保持 posted=false：{}", split.getCode(), e.getMessage());
            } else {
                LOG.error("资产拆分过账异常，拆分单 {} 保持 posted=false", split.getCode(), e);
            }
            return false;
        }
    }

    private PostingEvent buildEvent(ErpAstSplit split, ErpAstAsset source, List<ErpAstSplitLine> lines,
                                    List<ErpAstAsset> targets) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.ASSET_SPLIT);
        event.setBillHeadCode(split.getCode());
        event.setOrgId(split.getOrgId() != null ? split.getOrgId() : source.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(event.getOrgId()));
        event.setCurrencyId(split.getCurrencyId() != null ? split.getCurrencyId() : source.getCurrencyId());
        event.setExchangeRate(split.getExchangeRate() != null ? split.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = split.getBusinessDate() != null ? split.getBusinessDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        // 借方明细：按行（每行各自类别 subjectId）拆分
        List<Map<String, Object>> debitLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ErpAstSplitLine line = lines.get(i);
            ErpAstAsset target = targets.get(i);
            Long categoryId = line.getCategoryId() != null ? line.getCategoryId() : source.getCategoryId();
            ErpAstAssetCategory category = loadCategory(categoryId);
            String subjectCode = resolveSubjectCode(category != null ? category.getSubjectId() : null, "1601");
            debitLines.add(lineMap(subjectCode, "固定资产", nz(line.getOriginalCostAmount()),
                    target != null ? target.getCode() : line.getTargetAssetCode()));
        }

        // 贷方单行：源资产类别 subjectId，合计 = 源原值
        ErpAstAssetCategory sourceCategory = loadCategory(source.getCategoryId());
        String creditSubject = resolveSubjectCode(
                sourceCategory != null ? sourceCategory.getSubjectId() : null, "1601");
        BigDecimal sourceOriginal = nz(source.getOriginalValue());
        List<Map<String, Object>> creditLines = new ArrayList<>();
        creditLines.add(lineMap(creditSubject, "固定资产", sourceOriginal, source.getCode()));

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
