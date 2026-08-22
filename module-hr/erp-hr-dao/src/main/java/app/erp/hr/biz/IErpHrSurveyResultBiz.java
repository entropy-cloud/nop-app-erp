
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.hr.dao.entity.ErpHrSurveyResult;

import java.util.Map;

public interface IErpHrSurveyResultBiz extends ICrudBiz<ErpHrSurveyResult>{

    /**
     * CLOSED 自动聚合（UC-HR-11 基本流程 8，RC-R1.9 P1-RC-016）：按部门（经 ErpHrEmployee.departmentId
     * 解析；匿名答卷仅计入整体行）upsert ErpHrSurveyResult 行 + 整体行（departmentId=null），
     * 计算 eNPS/avgScore/driverScores/questionBreakdown/trendData 并回写 ErpHrSurvey 头字段。
     */
    @BizMutation
    ErpHrSurveyResult aggregateResult(@Name("surveyId") String surveyId, IServiceContext context);

    /**
     * 结果仪表盘查询（UC-HR-11 基本流程 9，RC-R1.9 P1-RC-016）：返回问卷头信息 + 整体行 + 部门行，
     * 供 AMIS 直接渲染（评分趋势/部门对比/eNPS/驱动因子分析数据面）。
     */
    @BizQuery
    Map<String, Object> getSurveyDashboard(@Name("surveyId") String surveyId, IServiceContext context);
}
