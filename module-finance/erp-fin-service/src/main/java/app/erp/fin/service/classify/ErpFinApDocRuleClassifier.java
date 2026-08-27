package app.erp.fin.service.classify;

import app.erp.fin.dao.entity.ErpFinApDocument;
import app.erp.fin.service.ErpFinConfigs;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.core.context.IServiceContext;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *
 * <p>P2-6 确定性修复（plan 2026-08-28-0219-1）：伙伴候选查询按 id 升序（首匹配稳定）；
 * 超 {@code erp-fin.ap-doc-partner-match-limit}（默认 2000）截断 WARN 可观测（行为保持落人工门）；
 * 解析要素 null 值不再经 {@code String.valueOf} 产生字面 {@code "null"}。
 */
public class ErpFinApDocRuleClassifier implements IErpFinApDocClassifier {

    static final Logger LOG = LoggerFactory.getLogger(ErpFinApDocRuleClassifier.class);

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
        // P2-6：null 要素直取 null，不经 String.valueOf 产生字面 "null" 幻影值
        String excerpt = stringOrNull(parseResult, "excerpt");
        String supplierName = stringOrNull(parseResult, "supplierName");

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

    private String stringOrNull(Map<String, Object> parseResult, String key) {
        Object value = parseResult == null ? null : parseResult.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String classifyDocType(String excerpt) {
        // P2-6：补 "null" 字面守卫（与 matchPartner 对齐，收口幻影路径）
        if (StringHelper.isBlank(excerpt) || "null".equals(excerpt)) {
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
        int limit = AppConfig.var(ErpFinConfigs.CONFIG_AP_DOC_PARTNER_MATCH_LIMIT,
                ErpFinConfigs.DEFAULT_AP_DOC_PARTNER_MATCH_LIMIT);
        QueryBean q = new QueryBean();
        q.addFilter(in("partnerType", List.of("SUPPLIER", "BOTH")));
        q.addFilter(eq("status", "ACTIVE"));
        // P2-6 确定性：按 id 升序，首匹配稳定（无排序时 DB 返回序不定致同输入首匹配漂移）。
        // 名称过滤保留内存扫描：双向包含（p.name ⊇ supplierName 或 supplierName ⊇ p.name）
        // 无法下推为单一查询条件而不丢失反向包含命中（如下推 like %name% 会漏「伙伴名是供应商名子串」的匹配），
        // 候选集有 limit 上界，内存扫描代价有界（保留理由登记 plan 2026-08-28-0219-1 执行注记）。
        q.addOrderField("id", false);
        if (limit > 0) {
            q.setLimit(limit);
        }
        // findPageByQuery 而非 findAllByQuery：后者忽略 QueryBean.limit（P2-6 修复——原 setLimit(2000) 实为无界加载）
        List<ErpMdPartner> partners = dao.findPageByQuery(q);
        if (limit > 0 && partners.size() >= limit) {
            // P2-6 超 2000 截断显式化：截断后未加载部分不可见，未匹配者照旧落人工门（行为不变，可观测）
            LOG.warn("erp-fin-ap-doc-partner-candidates-truncated: supplierName={}, hit={}, limit={}",
                    supplierName, partners.size(), limit);
        }
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
