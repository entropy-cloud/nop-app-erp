
package app.erp.cs.biz;

import app.erp.cs.dao.entity.ErpCsSurvey;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

public interface IErpCsSurveyBiz extends ICrudBiz<ErpCsSurvey> {

    @BizMutation
    ErpCsSurvey createSurvey(@Name("ticketId") Long ticketId, IServiceContext context);

    @BizMutation
    ErpCsSurvey submitSurvey(@Name("surveyToken") String surveyToken,
                             @Optional @Name("csatScore") Integer csatScore,
                             @Optional @Name("npsScore") Integer npsScore,
                             @Optional @Name("cesScore") Integer cesScore,
                             @Optional @Name("comment") String comment,
                             IServiceContext context);

    @BizQuery
    List<ErpCsSurvey> findSurveyReminders(@Name("reminderHours") Integer reminderHours,
                                          IServiceContext context);

    @BizQuery
    List<ErpCsSurvey> findExpiredSurveys(@Name("expireDays") Integer expireDays,
                                         IServiceContext context);
}
