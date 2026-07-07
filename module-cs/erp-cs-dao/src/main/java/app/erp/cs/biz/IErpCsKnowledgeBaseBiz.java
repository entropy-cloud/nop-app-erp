
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
}
