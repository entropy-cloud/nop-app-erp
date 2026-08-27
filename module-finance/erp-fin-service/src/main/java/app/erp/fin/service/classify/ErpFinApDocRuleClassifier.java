package app.erp.fin.service.classify;

import app.erp.fin.dao.entity.ErpFinApDocument;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * E3.5 默认规则分类引擎（Phase 1，`document-driven-ap-automation.md` §2）：
 * 文本关键字 → 单据类型；解析出的供应商名 → {@link ErpMdPartner} 名称双向包含匹配 → 对应方。
 *
 * <p>置信度：0.3（有文本）+ 0.3（发票三要素齐：号码/日期/金额）+ 0.4（对应方匹配），
 * 上限 1.0。低于阈值（{@code erp-fin.ap-doc-confidence-threshold}）由管道挂人工复核。
 *
 * <p>跨域只读 {@link ErpMdPartner}（md-dao，主数据供应商匹配）：IDaoProvider 直访对齐
 * {@code ApsLoadSourceProvider} 范式（非业务动作调用方，只读聚合导出消费）。
 */
public class ErpFinApDocRuleClassifier implements IErpFinApDocClassifier {

    private static final BigDecimal SCORE_TEXT = new BigDecimal("0.3");
    private static final BigDecimal SCORE_ELEMENTS = new BigDecimal("0.3");
    private static final BigDecimal SCORE_PARTNER = new BigDecimal("0.4");

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    @Override
    public ApDocClassification classify(ErpFinApDocument doc, Map<String, Object> parseResult, IServiceContext context) {
        ApDocClassification result = new ApDocClassification();
        String excerpt = parseResult == null ? null : String.valueOf(parseResult.get("excerpt"));
        String supplierName = parseResult == null ? null : String.valueOf(parseResult.get("supplierName"));

        result.setDocType(classifyDocType(excerpt));

        BigDecimal score = BigDecimal.ZERO;
        if (!StringHelper.isBlank(excerpt)) {
            score = score.add(SCORE_TEXT);
        }
        if (hasInvoiceElements(parseResult)) {
            score = score.add(SCORE_ELEMENTS);
        }
        ErpMdPartner partner = matchPartner(supplierName);
        if (partner != null) {
            result.setPartnerId(partner.getId());
            score = score.add(SCORE_PARTNER);
            result.setReason("供应商名「" + supplierName + "」匹配对应方 " + partner.getCode());
        } else {
            result.setReason(StringHelper.isBlank(supplierName) ? "未识别供应商名" : "供应商名「" + supplierName + "」无匹配对应方");
        }
        result.setConfidence(score.min(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP));
        return result;
    }

    private String classifyDocType(String excerpt) {
        if (StringHelper.isBlank(excerpt)) {
            return null;
        }
        if (excerpt.contains("增值税专用发票")) {
            return "VAT_INVOICE";
        }
        if (excerpt.contains("增值税普通发票") || excerpt.contains("电子发票") || excerpt.contains("发票")) {
            return "GENERAL_INVOICE";
        }
        if (excerpt.contains("收据")) {
            return "RECEIPT";
        }
        return "OTHER";
    }

    private boolean hasInvoiceElements(Map<String, Object> parseResult) {
        if (parseResult == null) {
            return false;
        }
        return parseResult.get("invoiceNo") != null
                && parseResult.get("invoiceDate") != null
                && parseResult.get("amount") != null;
    }

    private ErpMdPartner matchPartner(String supplierName) {
        if (StringHelper.isBlank(supplierName) || supplierName.length() < 2 || "null".equals(supplierName)) {
            return null;
        }
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        // 优先活跃供应商（SUPPLIER/BOTH），名称双向包含匹配
        QueryBean q = new QueryBean();
        q.addFilter(in("partnerType", List.of("SUPPLIER", "BOTH")));
        q.addFilter(eq("status", "ACTIVE"));
        q.setLimit(2000);
        List<ErpMdPartner> partners = dao.findAllByQuery(q);
        ErpMdPartner fallback = null;
        for (ErpMdPartner p : partners) {
            if (p.getName() == null) {
                continue;
            }
            if (p.getName().contains(supplierName) || supplierName.contains(p.getName())) {
                if ("SUPPLIER".equals(p.getPartnerType())) {
                    return p;
                }
                fallback = p;
            }
        }
        return fallback;
    }
}
