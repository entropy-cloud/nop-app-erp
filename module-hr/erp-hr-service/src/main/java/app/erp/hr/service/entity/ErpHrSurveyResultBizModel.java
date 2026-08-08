
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;

import app.erp.hr.biz.IErpHrDepartmentBiz;
import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.biz.IErpHrSurveyAnswerBiz;
import app.erp.hr.biz.IErpHrSurveyBiz;
import app.erp.hr.biz.IErpHrSurveyQuestionBiz;
import app.erp.hr.biz.IErpHrSurveyResponseBiz;
import app.erp.hr.biz.IErpHrSurveyResultBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSurvey;
import app.erp.hr.dao.entity.ErpHrSurveyAnswer;
import app.erp.hr.dao.entity.ErpHrSurveyQuestion;
import app.erp.hr.dao.entity.ErpHrSurveyResponse;
import app.erp.hr.dao.entity.ErpHrSurveyResult;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 调研结果聚合 BizModel（UC-HR-11 基本流程 8-9，RC-R1.9 P1-RC-016）。
 *
 * <p>aggregateResult 由 close() 触发：按部门（经 ErpHrEmployee.departmentId 解析；匿名答卷 employeeId
 * 为空，仅计入整体行 departmentId=null）upsert ErpHrSurveyResult 行；eNPS = (promoters − detractors)
 * / total × 100（promoters=9-10，detractors=0-6，7-8 passive 不计）；avgScore = 全部 RATING 题（含 ENPS 题）
 * 评分算术平均；driverScores/questionBreakdown/trendData 为 JSON 承载（见 Plan Decision 聚合口径）。
 */
@BizModel("ErpHrSurveyResult")
public class ErpHrSurveyResultBizModel extends CrudBizModel<ErpHrSurveyResult> implements IErpHrSurveyResultBiz {

    @Inject
    IErpHrSurveyBiz surveyBiz;

    @Inject
    IErpHrSurveyResponseBiz responseBiz;

    @Inject
    IErpHrSurveyAnswerBiz answerBiz;

    @Inject
    IErpHrSurveyQuestionBiz questionBiz;

    @Inject
    IErpHrEmployeeBiz employeeBiz;

    @Inject
    IErpHrDepartmentBiz departmentBiz;

    public ErpHrSurveyResultBizModel() {
        setEntityName(ErpHrSurveyResult.class.getName());
    }

    @Override
    @BizMutation
    public ErpHrSurveyResult aggregateResult(@Name("surveyId") Long surveyId, IServiceContext context) {
        ErpHrSurvey survey = surveyBiz.requireEntity(String.valueOf(surveyId), null, context);

        List<ErpHrSurveyQuestion> questions = loadQuestions(surveyId, context);
        Map<Long, ErpHrSurveyQuestion> questionById = new HashMap<>();
        for (ErpHrSurveyQuestion question : questions) {
            questionById.put(question.getId(), question);
        }

        QueryBean qr = new QueryBean();
        qr.addFilter(eq("surveyId", surveyId));
        List<ErpHrSurveyResponse> responses = responseBiz.findList(qr, null, context);

        Map<Long, List<ErpHrSurveyAnswer>> answersByResponse = loadAnswersByResponse(responses, context);

        Map<Long, List<ErpHrSurveyResponse>> responsesByDept = new LinkedHashMap<>();
        responsesByDept.put(null, responses);
        for (ErpHrSurveyResponse response : responses) {
            if (response.getEmployeeId() != null) {
                ErpHrEmployee employee = response.getEmployee();
                if (employee != null && employee.getDepartmentId() != null) {
                    responsesByDept.computeIfAbsent(employee.getDepartmentId(), k -> new ArrayList<>()).add(response);
                }
            }
        }

        ErpHrSurveyResult overall = null;
        for (Map.Entry<Long, List<ErpHrSurveyResponse>> entry : responsesByDept.entrySet()) {
            ErpHrSurveyResult row = upsertResultRow(surveyId, entry.getKey(), context);
            fillRow(row, entry.getValue(), questionById, answersByResponse, survey, context);
            if (entry.getKey() == null) {
                overall = row;
            }
        }

        int totalResponses = responses.size();
        survey.setTotalResponses(totalResponses);
        survey.setCompletionRate(completionRateOf(survey, totalResponses, context));
        survey.setAvgScore(overall == null ? null : overall.getAvgScore());
        survey.setENpsScore(overall == null ? null : overall.getENpsScore());
        surveyBiz.updateEntity(survey, null, context);
        return overall;
    }

    @Override
    @BizQuery
    public Map<String, Object> getSurveyDashboard(@Name("surveyId") Long surveyId, IServiceContext context) {
        ErpHrSurvey survey = surveyBiz.requireEntity(String.valueOf(surveyId), null, context);
        QueryBean q = new QueryBean();
        q.addFilter(eq("surveyId", surveyId));
        List<ErpHrSurveyResult> rows = findList(q, null, context);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("surveyId", surveyId);
        result.put("title", survey.getTitle());
        result.put("surveyType", survey.getSurveyType());
        result.put("status", survey.getStatus());
        result.put("totalResponses", survey.getTotalResponses());
        result.put("completionRate", survey.getCompletionRate());

        Map<String, Object> overall = null;
        List<Map<String, Object>> departments = new ArrayList<>();
        for (ErpHrSurveyResult row : rows) {
            Map<String, Object> m = resultRowMap(row);
            if (row.getDepartmentId() == null) {
                overall = m;
            } else {
                ErpHrDepartment department = departmentBiz.get(String.valueOf(row.getDepartmentId()), false, context);
                m.put("departmentName", department == null ? null : department.getName());
                departments.add(m);
            }
        }
        result.put("overall", overall);
        result.put("departments", departments);
        return result;
    }

    // ---------- helpers ----------

    private List<ErpHrSurveyQuestion> loadQuestions(Long surveyId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("surveyId", surveyId));
        return questionBiz.findList(q, null, context);
    }

    private Map<Long, List<ErpHrSurveyAnswer>> loadAnswersByResponse(List<ErpHrSurveyResponse> responses,
                                                                     IServiceContext context) {
        Map<Long, List<ErpHrSurveyAnswer>> byResponse = new HashMap<>();
        if (responses.isEmpty()) {
            return byResponse;
        }
        List<Long> responseIds = new ArrayList<>();
        for (ErpHrSurveyResponse response : responses) {
            responseIds.add(response.getId());
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("responseId", responseIds));
        for (ErpHrSurveyAnswer answer : answerBiz.findList(q, null, context)) {
            byResponse.computeIfAbsent(answer.getResponseId(), k -> new ArrayList<>()).add(answer);
        }
        return byResponse;
    }

    private ErpHrSurveyResult upsertResultRow(Long surveyId, Long departmentId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("surveyId", surveyId));
        if (departmentId == null) {
            q.addFilter(eq("departmentId", null));
        } else {
            q.addFilter(eq("departmentId", departmentId));
        }
        q.setLimit(1);
        List<ErpHrSurveyResult> existing = findList(q, null, context);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        ErpHrSurveyResult row = newEntity();
        row.setSurveyId(surveyId);
        row.setDepartmentId(departmentId);
        return row;
    }

    private void fillRow(ErpHrSurveyResult row, List<ErpHrSurveyResponse> group,
                         Map<Long, ErpHrSurveyQuestion> questionById,
                         Map<Long, List<ErpHrSurveyAnswer>> answersByResponse,
                         ErpHrSurvey survey, IServiceContext context) {
        List<ErpHrSurveyAnswer> allAnswers = new ArrayList<>();
        for (ErpHrSurveyResponse response : group) {
            List<ErpHrSurveyAnswer> list = answersByResponse.get(response.getId());
            if (list != null) {
                allAnswers.addAll(list);
            }
        }

        List<Integer> allRatings = new ArrayList<>();
        List<Integer> enpsRatings = new ArrayList<>();
        Map<Long, List<Integer>> ratingsByQuestion = new HashMap<>();
        Map<String, List<Integer>> ratingsByDriver = new HashMap<>();
        for (ErpHrSurveyAnswer answer : allAnswers) {
            ErpHrSurveyQuestion question = questionById.get(answer.getQuestionId());
            if (question == null || answer.getRatingValue() == null) {
                continue;
            }
            int value = answer.getRatingValue();
            if (isRatingQuestion(question)) {
                allRatings.add(value);
                ratingsByQuestion.computeIfAbsent(question.getId(), k -> new ArrayList<>()).add(value);
                if (question.getDriverCategory() != null) {
                    ratingsByDriver.computeIfAbsent(question.getDriverCategory(), k -> new ArrayList<>()).add(value);
                }
            }
            if (isEnpsQuestion(question)) {
                enpsRatings.add(value);
            }
        }

        row.setTotalResponses(group.size());
        row.setAvgScore(average(allRatings));
        row.setENpsScore(enpsScore(enpsRatings));
        row.setDriverScores(JsonTool.serialize(driverScoresMap(ratingsByDriver), false));
        row.setQuestionBreakdown(JsonTool.serialize(questionBreakdownList(ratingsByQuestion), false));
        row.setTrendData(JsonTool.serialize(row.getDepartmentId() == null ? trendDataList(survey, context) : new ArrayList<>(), false));
        row.setLastCalculatedAt(CoreMetrics.currentTimestamp());
        if (row.orm_id() == null) {
            saveEntity(row, null, context);
        } else {
            updateEntity(row, null, context);
        }
    }

    private boolean isRatingQuestion(ErpHrSurveyQuestion question) {
        return Objects.equals(question.getQuestionType(), ErpHrConstants.QUESTION_TYPE_RATING)
                || Objects.equals(question.getQuestionType(), ErpHrConstants.QUESTION_TYPE_ENPS);
    }

    private boolean isEnpsQuestion(ErpHrSurveyQuestion question) {
        return Objects.equals(question.getQuestionType(), ErpHrConstants.QUESTION_TYPE_ENPS);
    }

    private static BigDecimal average(List<Integer> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Integer value : values) {
            sum = sum.add(BigDecimal.valueOf(value));
        }
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private static Integer enpsScore(List<Integer> ratings) {
        if (ratings.isEmpty()) {
            return null;
        }
        int promoters = 0;
        int detractors = 0;
        for (Integer rating : ratings) {
            if (rating >= 9) {
                promoters++;
            } else if (rating <= 6) {
                detractors++;
            }
        }
        return (int) Math.round((promoters - detractors) * 100.0 / ratings.size());
    }

    private static Map<String, Object> driverScoresMap(Map<String, List<Integer>> ratingsByDriver) {
        Map<String, Object> map = new TreeMap<>();
        for (Map.Entry<String, List<Integer>> entry : ratingsByDriver.entrySet()) {
            map.put(entry.getKey(), average(entry.getValue()));
        }
        return map;
    }

    private static List<Map<String, Object>> questionBreakdownList(Map<Long, List<Integer>> ratingsByQuestion) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, List<Integer>> entry : ratingsByQuestion.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", entry.getKey());
            item.put("avgScore", average(entry.getValue()));
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> trendDataList(ErpHrSurvey survey, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("surveyType", survey.getSurveyType()));
        q.addFilter(eq("status", ErpHrConstants.SURVEY_STATUS_CLOSED));
        q.addOrderField("endDate", false);
        List<ErpHrSurvey> history = surveyBiz.findList(q, null, context);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ErpHrSurvey item : history) {
            if (Objects.equals(item.getId(), survey.getId())) {
                continue;
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("surveyId", item.getId());
            point.put("title", item.getTitle());
            point.put("endDate", item.getEndDate());
            point.put("avgScore", item.getAvgScore());
            point.put("eNpsScore", item.getENpsScore());
            list.add(point);
        }
        return list;
    }

    private BigDecimal completionRateOf(ErpHrSurvey survey, int totalResponses, IServiceContext context) {
        long target = targetEmployeeCount(survey, context);
        if (target == 0) {
            return null;
        }
        return BigDecimal.valueOf(totalResponses * 100.0 / target).setScale(2, RoundingMode.HALF_UP);
    }

    private long targetEmployeeCount(ErpHrSurvey survey, IServiceContext context) {
        QueryBean q = new QueryBean();
        if (survey.getTargetDepartmentId() != null) {
            q.addFilter(eq("departmentId", survey.getTargetDepartmentId()));
        }
        q.addFilter(in("employmentStatus",
                List.of(ErpHrConstants.EMPLOYMENT_ACTIVE, ErpHrConstants.EMPLOYMENT_PROBATION)));
        return employeeBiz.findCount(q, context);
    }

    private Map<String, Object> resultRowMap(ErpHrSurveyResult row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("departmentId", row.getDepartmentId());
        m.put("totalResponses", row.getTotalResponses());
        m.put("avgScore", row.getAvgScore());
        m.put("eNpsScore", row.getENpsScore());
        m.put("driverScores", row.getDriverScores());
        m.put("questionBreakdown", row.getQuestionBreakdown());
        m.put("trendData", row.getTrendData());
        m.put("lastCalculatedAt", row.getLastCalculatedAt());
        return m;
    }
}
