package app.erp.hr.service;

import app.erp.hr.biz.IErpHrSurveyBiz;
import app.erp.hr.biz.IErpHrSurveyQuestionBiz;
import app.erp.hr.biz.IErpHrSurveyResponseBiz;
import app.erp.hr.biz.IErpHrSurveyResultBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSurvey;
import app.erp.hr.dao.entity.ErpHrSurveyQuestion;
import app.erp.hr.dao.entity.ErpHrSurveyResponse;
import app.erp.hr.dao.entity.ErpHrSurveyResult;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 员工调研族测试（use-cases.md UC-HR-11，RC-R1.9 P1-MA2-041 + P1-RC-016）。覆盖：
 * <ul>
 *   <li>① publish 校验（无题目拒绝/日期缺失拒绝/非法迁移拒绝）+ DRAFT→OPEN。</li>
 *   <li>② publish 后编辑守卫：非 DRAFT 问卷配置字段/title 修改拒绝（ERR_HR_SURVEY_PUBLISHED_IMMUTABLE），remark 放行，DRAFT 可编辑。</li>
 *   <li>③ 匿名提交：respondentHash 非空 + employeeId 空 + 同人重复提交拒绝 + totalResponses 递增 + 匿名聚合仅整体行。</li>
 *   <li>④ 非匿名提交：employeeId 存储 + 同人重复拒绝。</li>
 *   <li>⑤ 非 OPEN 问卷提交拒绝 + 非法题目拒绝。</li>
 *   <li>⑥ close → aggregateResult：多部门答卷 → 整体行 + 部门行数值断言（eNPS promoters/detractors/passive 边界）
 *       + driverScores/questionBreakdown JSON + survey 头 totalResponses/avgScore/eNpsScore/completionRate 回写。</li>
 *   <li>⑦ archive CLOSED→ARCHIVED + 非法迁移拒绝。</li>
 *   <li>⑧ 仪表盘 @BizQuery：整体 + 部门行 + trendData 序列断言。</li>
 *   <li>⑨ GraphQL 冒烟：ErpHrSurvey__publish/close/archive + ErpHrSurveyResponse__submitResponse
 *       + ErpHrSurveyResult__getSurveyDashboard 经 graphQLEngine.executeRpc 可达。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrSurveyLifecycle extends JunitAutoTestCase {

    @RegisterExtension
    static HrFrozenClockExtension frozenClock = new HrFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final LocalDate REF = HrFrozenClockExtension.REFERENCE_DATE;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrSurveyBiz surveyBiz;
    @Inject
    IErpHrSurveyQuestionBiz surveyQuestionBiz;
    @Inject
    IErpHrSurveyResponseBiz responseBiz;
    @Inject
    IErpHrSurveyResultBiz resultBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    private IUserContext prevUserContext;

    @BeforeEach
    void injectUserContext() {
        prevUserContext = IUserContext.get();
        UserContextImpl user = new UserContextImpl();
        user.setUserId("svy-test");
        user.setUserName("svy-test");
        user.setRoles(Set.of("STAFF"));
        IUserContext.set(user);
    }

    @AfterEach
    void restoreUserContext() {
        IUserContext.set(prevUserContext);
    }

    @Test
    public void testPublishValidationsAndTransition() {
        Long noQuestion = ormTemplate.runInSession(session -> seedSurvey("SVY-PUB-NOQ", ErpHrConstants.SURVEY_STATUS_DRAFT,
                true, REF, REF.plusDays(7), "ANNUAL_ENGAGEMENT"));
        NopException exNoQ = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> surveyBiz.publish(noQuestion, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_ILLEGAL_TRANSITION.getErrorCode(), exNoQ.getErrorCode(),
                "无题目 publish 应拒绝");
        assertEquals(ErpHrConstants.SURVEY_STATUS_DRAFT, reloadSurvey(noQuestion).getStatus());

        Long noDate = ormTemplate.runInSession(session -> {
            Long surveyId = seedSurvey("SVY-PUB-NOD", ErpHrConstants.SURVEY_STATUS_DRAFT, true, null, null, "PULSE");
            seedQuestion(surveyId, "题目A", ErpHrConstants.QUESTION_TYPE_RATING, "GROWTH", null);
            return surveyId;
        });
        NopException exNoDate = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> surveyBiz.publish(noDate, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_ILLEGAL_TRANSITION.getErrorCode(), exNoDate.getErrorCode(),
                "起止日期缺失 publish 应拒绝");

        Long ready = ormTemplate.runInSession(session -> {
            Long surveyId = seedSurvey("SVY-PUB-OK", ErpHrConstants.SURVEY_STATUS_DRAFT, true, REF, REF.plusDays(7), "PULSE");
            seedQuestion(surveyId, "题目A", ErpHrConstants.QUESTION_TYPE_RATING, "GROWTH", null);
            return surveyId;
        });
        ErpHrSurvey published = ormTemplate.runInSession(session -> surveyBiz.publish(ready, CTX));
        assertEquals(ErpHrConstants.SURVEY_STATUS_OPEN, published.getStatus());
        assertEquals(ErpHrConstants.SURVEY_STATUS_OPEN, reloadSurvey(ready).getStatus());

        NopException exRepub = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> surveyBiz.publish(ready, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_ILLEGAL_TRANSITION.getErrorCode(), exRepub.getErrorCode(),
                "OPEN 再 publish 应拒绝（非法迁移）");
    }

    @Test
    public void testPublishedSurveyEditGuard() {
        Long publishedId = ormTemplate.runInSession(session -> {
            Long surveyId = seedSurvey("SVY-GUARD", ErpHrConstants.SURVEY_STATUS_DRAFT, true, REF, REF.plusDays(7), "PULSE");
            seedQuestion(surveyId, "题目A", ErpHrConstants.QUESTION_TYPE_RATING, "GROWTH", null);
            return surveyId;
        });
        ormTemplate.runInSession(session -> surveyBiz.publish(publishedId, CTX));

        Map<String, Object> data = new HashMap<>();
        data.put("id", String.valueOf(publishedId));
        data.put("title", "已发布不可改");
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> surveyBiz.update(data, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_PUBLISHED_IMMUTABLE.getErrorCode(), ex.getErrorCode(),
                "OPEN 问卷修改 title 应抛 ERR_HR_SURVEY_PUBLISHED_IMMUTABLE");
        assertEquals("SVY-GUARD-发布问卷", reloadSurvey(publishedId).getTitle(), "拒绝后标题应保持原值");

        Map<String, Object> remarkData = new HashMap<>();
        remarkData.put("id", String.valueOf(publishedId));
        remarkData.put("remark", "仅改备注");
        ormTemplate.runInSession(session -> surveyBiz.update(remarkData, CTX));
        assertEquals("仅改备注", reloadSurvey(publishedId).getRemark(), "非配置字段 remark 应放行");

        Long draftId = ormTemplate.runInSession(session -> seedSurvey("SVY-GUARD-D", ErpHrConstants.SURVEY_STATUS_DRAFT,
                true, REF, REF.plusDays(7), "PULSE"));
        Map<String, Object> draftData = new HashMap<>();
        draftData.put("id", String.valueOf(draftId));
        draftData.put("title", "DRAFT 可改标题");
        ormTemplate.runInSession(session -> surveyBiz.update(draftData, CTX));
        assertEquals("DRAFT 可改标题", reloadSurvey(draftId).getTitle(), "DRAFT 问卷修改配置应放行");
    }

    @Test
    public void testAnonymousSubmitAndDuplicateRejected() {
        Long[] seeded = ormTemplate.runInSession(session -> {
            Long emp1 = seedEmployee("EMP-SVY-AN-1", null);
            Long emp2 = seedEmployee("EMP-SVY-AN-2", null);
            Long surveyId = seedSurvey("SVY-ANON", ErpHrConstants.SURVEY_STATUS_OPEN, true, REF, REF.plusDays(7), "ENPS");
            Long q1 = seedQuestion(surveyId, "评分题", ErpHrConstants.QUESTION_TYPE_RATING, "GROWTH", null);
            Long q2 = seedQuestion(surveyId, "eNPS题", ErpHrConstants.QUESTION_TYPE_ENPS, null, null);
            return new Long[]{emp1, emp2, surveyId, q1, q2};
        });
        Long emp1 = seeded[0];
        Long emp2 = seeded[1];
        Long surveyId = seeded[2];
        Long q1 = seeded[3];
        Long q2 = seeded[4];

        ErpHrSurveyResponse r1 = ormTemplate.runInSession(session -> responseBiz.submitResponse(surveyId, emp1,
                answers(Map.of("questionId", q1, "ratingValue", 4), Map.of("questionId", q2, "ratingValue", 9)),
                CTX));
        assertNotNull(r1.getRespondentHash(), "匿名提交应写入 respondentHash");
        assertNull(r1.getEmployeeId(), "匿名提交 employeeId 应置空");
        assertEquals(Boolean.TRUE, r1.getIsComplete(), "提交应标记 isComplete=true");

        ErpHrSurveyResponse stored = reloadResponse(r1.getId());
        assertNotNull(stored.getRespondentHash(), "respondentHash 应落库");
        assertNull(stored.getEmployeeId(), "employeeId 不应落库");

        NopException dup = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> responseBiz.submitResponse(surveyId, emp1,
                        answers(Map.of("questionId", q1, "ratingValue", 5)), CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_ALREADY_SUBMITTED.getErrorCode(), dup.getErrorCode(),
                "同人匿名重复提交应被 respondentHash 拦截");

        ormTemplate.runInSession(session -> responseBiz.submitResponse(surveyId, emp2,
                answers(Map.of("questionId", q1, "ratingValue", 5), Map.of("questionId", q2, "ratingValue", 10)),
                CTX));
        assertEquals(2, reloadSurvey(surveyId).getTotalResponses(), "两次成功提交后 totalResponses 应为 2");

        ormTemplate.runInSession(session -> surveyBiz.close(surveyId, CTX));
        ErpHrSurveyResult overall = ormTemplate.runInSession(session -> findResultRow(surveyId, null, CTX));
        assertNotNull(overall, "close 后应生成整体聚合行");
        assertEquals(2, overall.getTotalResponses(), "匿名答卷应计入整体行");
        assertEquals(Integer.valueOf(100), overall.getENpsScore(),
                "匿名 eNPS：9/10 两档 promoter，(2-0)/2*100=100");
        assertEquals(new BigDecimal("7.00"), overall.getAvgScore(), "匿名 avgScore = (4+9+5+10)/4 = 7.00");
        assertTrue(overall.getTrendData() != null, "整体行应携带 trendData JSON");
    }

    @Test
    public void testNamedSubmitAndDuplicateRejected() {
        Long[] seeded = ormTemplate.runInSession(session -> {
            Long emp1 = seedEmployee("EMP-SVY-NA-1", null);
            Long surveyId = seedSurvey("SVY-NAMED", ErpHrConstants.SURVEY_STATUS_OPEN, false, REF, REF.plusDays(7), "PULSE");
            Long q1 = seedQuestion(surveyId, "评分题", ErpHrConstants.QUESTION_TYPE_RATING, null, null);
            return new Long[]{emp1, surveyId, q1};
        });
        Long emp1 = seeded[0];
        Long surveyId = seeded[1];
        Long q1 = seeded[2];

        ErpHrSurveyResponse r1 = ormTemplate.runInSession(session -> responseBiz.submitResponse(surveyId, emp1,
                answers(Map.of("questionId", q1, "ratingValue", 5)), CTX));
        assertEquals(emp1, r1.getEmployeeId(), "非匿名提交应存储 employeeId");
        assertNull(r1.getRespondentHash(), "非匿名提交不应写 respondentHash");

        NopException dup = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> responseBiz.submitResponse(surveyId, emp1,
                        answers(Map.of("questionId", q1, "ratingValue", 3)), CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_ALREADY_SUBMITTED.getErrorCode(), dup.getErrorCode(),
                "非匿名同人重复提交应被 employeeId 拦截");
    }

    @Test
    public void testSubmitToNonOpenSurveyRejected() {
        Long[] seeded = ormTemplate.runInSession(session -> {
            Long emp1 = seedEmployee("EMP-SVY-NO-1", null);
            Long draftId = seedSurvey("SVY-NO-DRAFT", ErpHrConstants.SURVEY_STATUS_DRAFT, true, REF, REF.plusDays(7), "PULSE");
            Long closedId = seedSurvey("SVY-NO-CLOSED", ErpHrConstants.SURVEY_STATUS_CLOSED, true, REF, REF.plusDays(7), "PULSE");
            Long openId = seedSurvey("SVY-NO-OPEN", ErpHrConstants.SURVEY_STATUS_OPEN, true, REF, REF.plusDays(7), "PULSE");
            Long q1 = seedQuestion(openId, "评分题", ErpHrConstants.QUESTION_TYPE_RATING, null, null);
            return new Long[]{emp1, draftId, closedId, openId, q1};
        });
        Long emp1 = seeded[0];
        Long draftId = seeded[1];
        Long closedId = seeded[2];
        Long openId = seeded[3];
        Long q1 = seeded[4];

        NopException exDraft = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> responseBiz.submitResponse(draftId, emp1,
                        answers(Map.of("questionId", q1, "ratingValue", 5)), CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_NOT_OPEN.getErrorCode(), exDraft.getErrorCode(),
                "DRAFT 问卷提交应拒绝");

        NopException exClosed = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> responseBiz.submitResponse(closedId, emp1,
                        answers(Map.of("questionId", q1, "ratingValue", 5)), CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_NOT_OPEN.getErrorCode(), exClosed.getErrorCode(),
                "CLOSED 问卷提交应拒绝");

        NopException exBadQ = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> responseBiz.submitResponse(openId, emp1,
                        answers(Map.of("questionId", 999999L, "ratingValue", 5)), CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_INVALID_QUESTION.getErrorCode(), exBadQ.getErrorCode(),
                "不属于问卷的题目提交应拒绝");
    }

    @Test
    public void testCloseAggregatesMultiDepartmentWithEnpsBoundaries() {
        Long[] seeded = ormTemplate.runInSession(session -> {
            Long deptA = seedDepartment("DEPT-SVY-A", "研发部");
            Long deptB = seedDepartment("DEPT-SVY-B", "市场部");
            Long emp1 = seedEmployee("EMP-SVY-AG-1", deptA);
            Long emp2 = seedEmployee("EMP-SVY-AG-2", deptB);
            Long emp3 = seedEmployee("EMP-SVY-AG-3", deptA);
            Long surveyId = seedSurvey("SVY-AGG", ErpHrConstants.SURVEY_STATUS_OPEN, false, REF, REF.plusDays(7), "ANNUAL_ENGAGEMENT");
            Long qEnps = seedQuestion(surveyId, "你有多大可能推荐本公司？", ErpHrConstants.QUESTION_TYPE_ENPS, null, null);
            Long qGrowth = seedQuestion(surveyId, "成长路径", ErpHrConstants.QUESTION_TYPE_RATING, "GROWTH", null);
            Long qRecog = seedQuestion(surveyId, "认可激励", ErpHrConstants.QUESTION_TYPE_RATING, "RECOGNITION", null);
            return new Long[]{deptA, deptB, emp1, emp2, emp3, surveyId, qEnps, qGrowth, qRecog};
        });
        Long emp1 = seeded[2];
        Long emp2 = seeded[3];
        Long emp3 = seeded[4];
        Long surveyId = seeded[5];
        Long qEnps = seeded[6];
        Long qGrowth = seeded[7];
        Long qRecog = seeded[8];

        ormTemplate.runInSession(session -> responseBiz.submitResponse(surveyId, emp1,
                answers(Map.of("questionId", qEnps, "ratingValue", 10),
                        Map.of("questionId", qGrowth, "ratingValue", 5),
                        Map.of("questionId", qRecog, "ratingValue", 4)), CTX));
        ormTemplate.runInSession(session -> responseBiz.submitResponse(surveyId, emp2,
                answers(Map.of("questionId", qEnps, "ratingValue", 7),
                        Map.of("questionId", qGrowth, "ratingValue", 4),
                        Map.of("questionId", qRecog, "ratingValue", 3)), CTX));
        ormTemplate.runInSession(session -> responseBiz.submitResponse(surveyId, emp3,
                answers(Map.of("questionId", qEnps, "ratingValue", 5),
                        Map.of("questionId", qGrowth, "ratingValue", 3),
                        Map.of("questionId", qRecog, "ratingValue", 2)), CTX));

        ormTemplate.runInSession(session -> surveyBiz.close(surveyId, CTX));

        ErpHrSurveyResult overall = ormTemplate.runInSession(session -> findResultRow(surveyId, null, CTX));
        assertNotNull(overall, "应生成整体聚合行");
        assertEquals(3, overall.getTotalResponses());
        assertEquals(Integer.valueOf(0), overall.getENpsScore(),
                "eNPS 边界：promoter(10)/passive(7)/detractor(5) 各一 → (1-1)/3*100=0");
        assertEquals(new BigDecimal("4.78"), overall.getAvgScore(), "整体 avgScore=(10+5+4+7+4+3+5+3+2)/9=4.78");

        Map<String, Object> driverScores = (Map<String, Object>) JsonTool.parseNonStrict(overall.getDriverScores());
        assertEquals(4.0, ((Number) driverScores.get("GROWTH")).doubleValue(), "driver GROWTH=(5+4+3)/3=4.0");
        assertEquals(3.0, ((Number) driverScores.get("RECOGNITION")).doubleValue(), "driver RECOGNITION=(4+3+2)/3=3.0");

        List<?> breakdown = (List<?>) JsonTool.parseNonStrict(overall.getQuestionBreakdown());
        assertEquals(3, breakdown.size(), "questionBreakdown 应按 3 题生成");
        Map<Long, Double> byQuestion = new LinkedHashMap<>();
        for (Object item : breakdown) {
            Map<?, ?> m = (Map<?, ?>) item;
            byQuestion.put(((Number) m.get("questionId")).longValue(), ((Number) m.get("avgScore")).doubleValue());
        }
        assertEquals(7.33, byQuestion.get(qEnps), 0.001, "ENPS 题平均=(10+7+5)/3=7.33");
        assertEquals(4.0, byQuestion.get(qGrowth), 0.001, "GROWTH 题平均=(5+4+3)/3=4.0");
        assertEquals(3.0, byQuestion.get(qRecog), 0.001, "RECOGNITION 题平均=(4+3+2)/3=3.0");

        ErpHrSurveyResult deptA = ormTemplate.runInSession(session -> findResultRow(surveyId, seeded[0], CTX));
        ErpHrSurveyResult deptB = ormTemplate.runInSession(session -> findResultRow(surveyId, seeded[1], CTX));
        assertNotNull(deptA, "应生成部门 A 聚合行");
        assertNotNull(deptB, "应生成部门 B 聚合行");
        assertEquals(2, deptA.getTotalResponses());
        assertEquals(Integer.valueOf(0), deptA.getENpsScore(), "部门 A eNPS：promoter(10)/detractor(5) → 0");
        assertEquals(new BigDecimal("4.83"), deptA.getAvgScore(), "部门 A avgScore=(10+5+4+5+3+2)/6=4.83");
        assertEquals(1, deptB.getTotalResponses());
        assertEquals(Integer.valueOf(0), deptB.getENpsScore(), "部门 B eNPS：passive(7) → 0");
        assertEquals(new BigDecimal("4.67"), deptB.getAvgScore(), "部门 B avgScore=(7+4+3)/3=4.67");

        ErpHrSurvey survey = reloadSurvey(surveyId);
        assertEquals(ErpHrConstants.SURVEY_STATUS_CLOSED, survey.getStatus());
        assertEquals(3, survey.getTotalResponses(), "survey 头 totalResponses 应回写 3");
        assertEquals(new BigDecimal("4.78"), survey.getAvgScore(), "survey 头 avgScore 应回写");
        assertEquals(Integer.valueOf(0), survey.getENpsScore(), "survey 头 eNpsScore 应回写");
        assertEquals(new BigDecimal("100.00"), survey.getCompletionRate(),
                "无目标部门 → 目标=全部 ACTIVE 员工 3，完成率=3/3=100.00%");
    }

    @Test
    public void testArchiveStateMachine() {
        Long surveyId = ormTemplate.runInSession(session -> {
            Long id = seedSurvey("SVY-ARCH", ErpHrConstants.SURVEY_STATUS_DRAFT, true, REF, REF.plusDays(7), "PULSE");
            seedQuestion(id, "题目A", ErpHrConstants.QUESTION_TYPE_RATING, null, null);
            return id;
        });

        NopException exDraftArchive = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> surveyBiz.archive(surveyId, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_ILLEGAL_TRANSITION.getErrorCode(), exDraftArchive.getErrorCode(),
                "DRAFT 直接 archive 应拒绝");

        ormTemplate.runInSession(session -> surveyBiz.publish(surveyId, CTX));
        NopException exOpenArchive = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> surveyBiz.archive(surveyId, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_ILLEGAL_TRANSITION.getErrorCode(), exOpenArchive.getErrorCode(),
                "OPEN 直接 archive 应拒绝");

        ormTemplate.runInSession(session -> surveyBiz.close(surveyId, CTX));
        ErpHrSurvey archived = ormTemplate.runInSession(session -> surveyBiz.archive(surveyId, CTX));
        assertEquals(ErpHrConstants.SURVEY_STATUS_ARCHIVED, archived.getStatus());
        assertEquals(ErpHrConstants.SURVEY_STATUS_ARCHIVED, reloadSurvey(surveyId).getStatus(),
                "CLOSED→ARCHIVED 后状态应持久化");

        NopException exReArchive = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> surveyBiz.archive(surveyId, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SURVEY_ILLEGAL_TRANSITION.getErrorCode(), exReArchive.getErrorCode(),
                "ARCHIVED 再 archive 应拒绝");
    }

    @Test
    public void testSurveyDashboardStructureAndTrend() {
        Long[] seeded = ormTemplate.runInSession(session -> {
            Long dept = seedDepartment("DEPT-SVY-DB", "运营部");
            Long emp1 = seedEmployee("EMP-SVY-DB-1", dept);
            Long historyId = seedSurveyWithScores("SVY-DB-HIST", ErpHrConstants.SURVEY_STATUS_CLOSED, "ANNUAL_ENGAGEMENT",
                    new BigDecimal("4.20"), 50, REF.minusDays(30));
            Long surveyId = seedSurvey("SVY-DB", ErpHrConstants.SURVEY_STATUS_OPEN, false, REF, REF.plusDays(7), "ANNUAL_ENGAGEMENT");
            Long q1 = seedQuestion(surveyId, "评分题", ErpHrConstants.QUESTION_TYPE_RATING, "GROWTH", null);
            return new Long[]{dept, emp1, historyId, surveyId, q1};
        });
        Long dept = seeded[0];
        Long emp1 = seeded[1];
        Long historyId = seeded[2];
        Long surveyId = seeded[3];
        Long q1 = seeded[4];

        ormTemplate.runInSession(session -> responseBiz.submitResponse(surveyId, emp1,
                answers(Map.of("questionId", q1, "ratingValue", 5)), CTX));
        ormTemplate.runInSession(session -> surveyBiz.close(surveyId, CTX));

        Map<String, Object> dashboard = ormTemplate.runInSession(session -> resultBiz.getSurveyDashboard(surveyId, CTX));
        assertEquals(surveyId, ((Number) dashboard.get("surveyId")).longValue());
        assertEquals(ErpHrConstants.SURVEY_STATUS_CLOSED, dashboard.get("status"));
        assertEquals(1, ((Number) dashboard.get("totalResponses")).intValue());
        assertTrue(dashboard.get("overall") != null, "仪表盘应返回整体行");

        Map<?, ?> overall = (Map<?, ?>) dashboard.get("overall");
        assertEquals(1, ((Number) overall.get("totalResponses")).intValue());
        List<?> trend = (List<?>) JsonTool.parseNonStrict((String) overall.get("trendData"));
        assertEquals(1, trend.size(), "trendData 应含同 surveyType 的 1 个历史 CLOSED 问卷点");
        Map<?, ?> point = (Map<?, ?>) trend.get(0);
        assertEquals(historyId, ((Number) point.get("surveyId")).longValue(), "趋势点应为历史问卷");
        assertEquals(4.20, ((Number) point.get("avgScore")).doubleValue(), 0.001);
        assertEquals(50, ((Number) point.get("eNpsScore")).intValue());

        List<?> departments = (List<?>) dashboard.get("departments");
        assertEquals(1, departments.size(), "非匿名答卷应生成部门行");
        Map<?, ?> deptRow = (Map<?, ?>) departments.get(0);
        assertEquals(dept, ((Number) deptRow.get("departmentId")).longValue(), "部门行应指向答卷员工所在部门");
        assertTrue(deptRow.containsKey("departmentName"), "部门行应携带 departmentName");
    }

    @Test
    public void testGraphQLSmokePublishSubmitCloseDashboardArchive() {
        Long[] seeded = ormTemplate.runInSession(session -> {
            Long emp1 = seedEmployee("EMP-SVY-GQL-1", null);
            Long surveyId = seedSurvey("SVY-GQL", ErpHrConstants.SURVEY_STATUS_DRAFT, true, REF, REF.plusDays(7), "PULSE");
            Long q1 = seedQuestion(surveyId, "评分题", ErpHrConstants.QUESTION_TYPE_RATING, "GROWTH", null);
            return new Long[]{emp1, surveyId, q1};
        });
        Long emp1 = seeded[0];
        Long surveyId = seeded[1];
        Long q1 = seeded[2];

        ApiResponse<?> publishResp = executeRpc(GraphQLOperationType.mutation, "ErpHrSurvey__publish",
                ApiRequest.build(Map.of("surveyId", String.valueOf(surveyId))));
        assertEquals(0, publishResp.getStatus(), "GraphQL publish 应成功");
        assertEquals(ErpHrConstants.SURVEY_STATUS_OPEN, reloadSurvey(surveyId).getStatus());

        ApiResponse<?> submitResp = executeRpc(GraphQLOperationType.mutation, "ErpHrSurveyResponse__submitResponse",
                ApiRequest.build(Map.of("surveyId", String.valueOf(surveyId),
                        "employeeId", String.valueOf(emp1),
                        "answers", List.of(Map.of("questionId", String.valueOf(q1), "ratingValue", 5)))));
        assertEquals(0, submitResp.getStatus(), "GraphQL submitResponse 应成功");
        ErpHrSurveyResponse response = ormTemplate.runInSession(session -> findResponseBySurvey(surveyId));
        assertNotNull(response, "GraphQL 提交后答卷应落库");
        assertNull(response.getEmployeeId(), "匿名模式 GraphQL 提交 employeeId 应为空");
        assertNotNull(response.getRespondentHash(), "匿名模式 GraphQL 提交应写 respondentHash");

        ApiResponse<?> closeResp = executeRpc(GraphQLOperationType.mutation, "ErpHrSurvey__close",
                ApiRequest.build(Map.of("surveyId", String.valueOf(surveyId))));
        assertEquals(0, closeResp.getStatus(), "GraphQL close 应成功");
        assertEquals(ErpHrConstants.SURVEY_STATUS_CLOSED, reloadSurvey(surveyId).getStatus());

        ApiResponse<?> dashboardResp = executeRpc(GraphQLOperationType.query, "ErpHrSurveyResult__getSurveyDashboard",
                ApiRequest.build(Map.of("surveyId", String.valueOf(surveyId))));
        assertEquals(0, dashboardResp.getStatus(), "GraphQL getSurveyDashboard 应成功");

        ApiResponse<?> archiveResp = executeRpc(GraphQLOperationType.mutation, "ErpHrSurvey__archive",
                ApiRequest.build(Map.of("surveyId", String.valueOf(surveyId))));
        assertEquals(0, archiveResp.getStatus(), "GraphQL archive 应成功");
        assertEquals(ErpHrConstants.SURVEY_STATUS_ARCHIVED, reloadSurvey(surveyId).getStatus());
    }

    // ---------- helpers ----------

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private List<Map<String, Object>> answers(Map<?, ?>... items) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<?, ?> item : items) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : item.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            list.add(copy);
        }
        return list;
    }

    private Long seedDepartment(String code, String name) {
        IEntityDao<ErpHrDepartment> dao = daoProvider.daoFor(ErpHrDepartment.class);
        ErpHrDepartment dept = new ErpHrDepartment();
        dept.setCode(code);
        dept.setName(name);
        dao.saveEntity(dept);
        return dept.getId();
    }

    private Long seedEmployee(String code, Long departmentId) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = new ErpHrEmployee();
        emp.setCode(code);
        emp.setFirstName("测");
        emp.setLastName("试");
        emp.setFullName(code);
        emp.setGender("MALE");
        emp.setHireDate(LocalDate.of(2025, 1, 1));
        emp.setEmploymentStatus(ErpHrConstants.EMPLOYMENT_ACTIVE);
        emp.setEmployeeType("FULL_TIME");
        emp.setDepartmentId(departmentId);
        dao.saveEntity(emp);
        return emp.getId();
    }

    private Long seedSurvey(String code, String status, boolean anonymous, LocalDate start, LocalDate end,
                            String surveyType) {
        IEntityDao<ErpHrSurvey> dao = daoProvider.daoFor(ErpHrSurvey.class);
        ErpHrSurvey survey = new ErpHrSurvey();
        survey.setCode(code);
        survey.setTitle(code + "-发布问卷");
        survey.setSurveyType(surveyType);
        survey.setStatus(status);
        survey.setIsAnonymous(anonymous);
        survey.setStartDate(start);
        survey.setEndDate(end);
        dao.saveEntity(survey);
        return survey.getId();
    }

    private Long seedSurveyWithScores(String code, String status, String surveyType, BigDecimal avgScore,
                                      Integer eNpsScore, LocalDate endDate) {
        IEntityDao<ErpHrSurvey> dao = daoProvider.daoFor(ErpHrSurvey.class);
        ErpHrSurvey survey = new ErpHrSurvey();
        survey.setCode(code);
        survey.setTitle(code + "-历史问卷");
        survey.setSurveyType(surveyType);
        survey.setStatus(status);
        survey.setStartDate(endDate.minusDays(7));
        survey.setEndDate(endDate);
        survey.setAvgScore(avgScore);
        survey.setENpsScore(eNpsScore);
        dao.saveEntity(survey);
        return survey.getId();
    }

    private Long seedQuestion(Long surveyId, String questionText, String questionType, String driverCategory,
                              Integer sortOrder) {
        IEntityDao<ErpHrSurveyQuestion> dao = daoProvider.daoFor(ErpHrSurveyQuestion.class);
        ErpHrSurveyQuestion question = new ErpHrSurveyQuestion();
        question.setSurveyId(surveyId);
        question.setQuestionText(questionText);
        question.setQuestionType(questionType);
        question.setDriverCategory(driverCategory);
        question.setSortOrder(sortOrder == null ? 1 : sortOrder);
        dao.saveEntity(question);
        return question.getId();
    }

    private ErpHrSurvey reloadSurvey(Long surveyId) {
        return ormTemplate.runInSession(session -> surveyBiz.get(String.valueOf(surveyId), false, CTX));
    }

    private ErpHrSurveyResponse reloadResponse(Long responseId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("id", responseId));
        q.setLimit(1);
        return ormTemplate.runInSession(session -> {
            List<ErpHrSurveyResponse> list = responseBiz.findList(q, null, CTX);
            return list.isEmpty() ? null : list.get(0);
        });
    }

    private ErpHrSurveyResponse findResponseBySurvey(Long surveyId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("surveyId", surveyId));
        q.setLimit(1);
        return ormTemplate.runInSession(session -> {
            List<ErpHrSurveyResponse> list = responseBiz.findList(q, null, CTX);
            return list.isEmpty() ? null : list.get(0);
        });
    }

    private ErpHrSurveyResult findResultRow(Long surveyId, Long departmentId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("surveyId", surveyId));
        if (departmentId == null) {
            q.addFilter(eq("departmentId", null));
        } else {
            q.addFilter(eq("departmentId", departmentId));
        }
        q.setLimit(1);
        List<ErpHrSurveyResult> list = resultBiz.findList(q, null, context);
        return list.isEmpty() ? null : list.get(0);
    }
}
