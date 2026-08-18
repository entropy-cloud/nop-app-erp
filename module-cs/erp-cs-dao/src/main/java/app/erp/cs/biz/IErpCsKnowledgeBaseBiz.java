
package app.erp.cs.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.cs.dao.entity.ErpCsKnowledgeBase;

import java.util.List;
import java.util.Map;

public interface IErpCsKnowledgeBaseBiz extends ICrudBiz<ErpCsKnowledgeBase>{

    @BizQuery
    List<Map<String, Object>> searchKnowledge(@Optional @Name("keyword") String keyword,
                                              @Optional @Name("categoryId") Long categoryId,
                                              @Optional @Name("limit") Integer limit,
                                              IServiceContext context);

    @BizQuery
    List<Map<String, Object>> suggestForTicket(@Optional @Name("subject") String subject,
                                                @Optional @Name("limit") Integer limit,
                                                IServiceContext context);

    /**
     * 知识库采纳使用统计（UC-CS-05 ⑧，RC-R1.69 B 类裁决 = TicketAction 派生，零 KB 加列）：
     * 统计 ADOPT_KNOWLEDGE 审计行（content 固定整串 {@code knowledgeBaseId={id}}，eq 精确匹配）。
     * {@code knowledgeBaseId} 提供时返回单条计数；缺省返回全量 group（每 KB 一条 {knowledgeBaseId, adoptCount}）。
     */
    @BizQuery
    List<Map<String, Object>> knowledgeUsageStats(@Optional @Name("knowledgeBaseId") Long knowledgeBaseId,
                                                  IServiceContext context);
}
