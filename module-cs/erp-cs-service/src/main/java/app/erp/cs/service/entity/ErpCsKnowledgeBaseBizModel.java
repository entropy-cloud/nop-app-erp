
package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsKnowledgeBaseBiz;
import app.erp.cs.dao.entity.ErpCsKnowledgeBase;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.FilterBeans;
import io.nop.api.core.beans.TreeBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@BizModel("ErpCsKnowledgeBase")
public class ErpCsKnowledgeBaseBizModel extends CrudBizModel<ErpCsKnowledgeBase> implements IErpCsKnowledgeBaseBiz {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ErpCsKnowledgeBaseBizModel.class);

    public ErpCsKnowledgeBaseBizModel() {
        setEntityName(ErpCsKnowledgeBase.class.getName());
    }

    @Override
    @BizQuery
    public List<Map<String, Object>> searchKnowledge(@Optional @Name("keyword") String keyword,
                                                     @Optional @Name("categoryId") Long categoryId,
                                                     @Optional @Name("limit") Integer limit,
                                                     IServiceContext context) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String kw = keyword.trim();
        if (kw.length() > ErpCsConstants.KNOWLEDGE_SEARCH_KEYWORD_MAX_LENGTH) {
            throw new NopException(ErpCsErrors.ERR_KNOWLEDGE_SEARCH_KEYWORD_TOO_LONG)
                    .param(ErpCsErrors.ARG_KEYWORD, kw)
                    .param(ErpCsErrors.ARG_MAX_LIMIT, ErpCsConstants.KNOWLEDGE_SEARCH_KEYWORD_MAX_LENGTH);
        }

        int effectiveLimit = resolveLimit(limit);

        QueryBean query = new QueryBean();
        query.addFilter(FilterBeans.eq("isPublished", Boolean.TRUE));
        TreeBean titleLike = FilterBeans.like("title", "%" + kw + "%");
        TreeBean contentLike = FilterBeans.like("content", "%" + kw + "%");
        query.addFilter(FilterBeans.or(titleLike, contentLike));
        if (categoryId != null) {
            query.addFilter(FilterBeans.eq("categoryId", categoryId));
        }
        query.addOrderField("createTime", true);
        query.setLimit(effectiveLimit * 2);

        // LIKE/OR 过滤器可能超出 XMeta 默认允许的 filterOp，走 doFindListByQueryDirectly 绕过 meta 限制
        // （同 ErpCsTicketBizModel.scanOverdueTickets 对 deadlineDateTime lt 的处理模式）
        List<ErpCsKnowledgeBase> entities = doFindListByQueryDirectly(query, context);

        final String kwLower = kw.toLowerCase();
        entities.sort(Comparator
                .comparingInt((ErpCsKnowledgeBase e) -> titleMatchScore(e, kwLower))
                .reversed()
                .thenComparing(ErpCsKnowledgeBase::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(effectiveLimit, entities.size()); i++) {
            result.add(toSummary(entities.get(i)));
        }
        return result;
    }

    @Override
    @BizQuery
    public List<Map<String, Object>> suggestForTicket(@Optional @Name("subject") String subject,
                                                      @Optional @Name("limit") Integer limit,
                                                      IServiceContext context) {
        if (subject == null || subject.trim().length() < ErpCsConstants.SUGGEST_SUBJECT_MIN_LENGTH) {
            return new ArrayList<>();
        }
        String keyword = extractKeyword(subject.trim());
        if (keyword == null || keyword.isEmpty()) {
            return new ArrayList<>();
        }
        int effectiveLimit = limit != null && limit > 0
                ? Math.min(limit, ErpCsConfigs.getKnowledgeSearchMaxLimit())
                : ErpCsConfigs.getKnowledgeSearchDefaultLimit();
        return searchKnowledge(keyword, null, effectiveLimit, context);
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return ErpCsConfigs.getKnowledgeSearchDefaultLimit();
        }
        int maxLimit = ErpCsConfigs.getKnowledgeSearchMaxLimit();
        if (limit > maxLimit) {
            LOG.debug("searchKnowledge limit {} exceeded max {}, clamped", limit, maxLimit);
            return maxLimit;
        }
        return limit;
    }

    private int titleMatchScore(ErpCsKnowledgeBase e, String kwLower) {
        String title = e.getTitle();
        if (title != null && title.toLowerCase().contains(kwLower)) {
            return 1;
        }
        return 0;
    }

    private Map<String, Object> toSummary(ErpCsKnowledgeBase entity) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", entity.getId());
        m.put("code", entity.getCode());
        m.put("title", entity.getTitle());
        m.put("contentSummary", truncate(entity.getContent(), ErpCsConstants.KNOWLEDGE_CONTENT_SUMMARY_LENGTH));
        m.put("categoryId", entity.getCategoryId());
        return m;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    private static String extractKeyword(String subject) {
        String[] tokens = subject.split("[\\s,，。.!！?？;；:：、/\\\\()（）\\[\\]【】]+");
        for (String token : tokens) {
            if (token.length() >= ErpCsConstants.SUGGEST_SUBJECT_MIN_LENGTH) {
                return token;
            }
        }
        return subject.length() >= ErpCsConstants.SUGGEST_SUBJECT_MIN_LENGTH ? subject : null;
    }
}
