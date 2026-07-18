package app.erp.mnt.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.posting.MaintenanceLaborPostingDispatcher;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 维修访问 cancel 红冲 MAINTENANCE_LABOR 凭证测试（plan 2026-07-18-1745-1 Phase 3）。
 *
 * <p>覆盖 {@code ErpMntVisitBizModel.doCancel} 内嵌的红冲触发：cancel 时若已生成 MAINTENANCE_LABOR 凭证
 * 则红冲该凭证（原凭证 isReversed=true + 红字凭证行同向取负）。镜像 {@code TestErpMfgSubcontractReverse} 范式
 *（{@code seedPostedVoucherFor} 直接落 voucher+billR 节省生命周期）。
 *
 * <p>状态机前置：cancel 经 {@code validateNotTerminal} 守卫仅允许 DRAFT/SCHEDULED/IN_PROGRESS → CANCELLED，
 * COMPLETED 态不可 cancel（Non-Goal：不新增 cancel 入口语义变化）。故测试以 IN_PROGRESS visit + 显式
 * {@link MaintenanceLaborPostingDispatcher#postLabor} 预置 voucher 模拟「已过账但未终态」场景。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntVisitCancelReversal extends JunitAutoTestCase {

    static final Long ORG_ID = 2351L;
    static final Long EQUIPMENT_ID = 351L;
    static final Long ASSIGNEE_ID = 2352L;
    static final Long CURRENCY_ID = 2351L;
    static final Long ACCT_SCHEMA_ID = 2351L;

    static final String SUBJECT_EXPENSE = "6602";
    static final String SUBJECT_PAYABLE = "2211";
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    MaintenanceLaborPostingDispatcher laborPostingDispatcher;

    private final java.util.concurrent.atomic.AtomicLong idSeq = new java.util.concurrent.atomic.AtomicLong(300000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    @AfterEach
    void resetPostingConfig() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMntConstants.CONFIG_LABOR_POSTING_ENABLED,
                String.valueOf(ErpMntConstants.DEFAULT_LABOR_POSTING_ENABLED));
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMntConstants.CONFIG_DEFAULT_LABOR_HOURLY_RATE,
                ErpMntConstants.DEFAULT_LABOR_HOURLY_RATE_VALUE);
    }

    // ---------- 场景 1：cancel 触发 MAINTENANCE_LABOR 凭证红冲 ----------

    @Test
    public void testCancelReversesLaborVoucher() {
        enablePosting(true, "80");
        seedPeriodAndSubjects();
        seedEquipment(EQUIPMENT_ID);

        Long visitId = nextId();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime end = start.plusMinutes(60);
        ormTemplate.runInSession(session -> {
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS,
                    "VST-CNL-RV", start, end);
            return null;
        });

        // 前置：直接调 postLabor 生成 MAINTENANCE_LABOR 凭证（绕过 complete 因 complete 后 visit 不可 cancel）
        ErpMntVisit visitForPost = daoProvider.daoFor(ErpMntVisit.class).getEntityById(visitId);
        assertTrue(laborPostingDispatcher.postLabor(visitForPost, new io.nop.core.context.ServiceContextImpl()),
                "前置 postLabor 应生成凭证");

        ErpFinVoucher original = findVoucher("VST-CNL-RV-ML");
        assertNotNull(original, "前置：应存在 MAINTENANCE_LABOR 凭证");
        BigDecimal originalDebit = findVoucherLine(original.getId(), SUBJECT_EXPENSE).getDebitAmount();
        assertEquals(0, bd("80").compareTo(originalDebit), "前置：Dr 6602=80");

        // 执行 cancel → doCancel 触发 reverseLabor
        assertEquals(0, cancel(visitId).getStatus(), "cancel 应成功");

        ErpMntVisit cancelled = loadVisit(visitId);
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_CANCELLED, cancelled.getStatus(),
                "cancel 后 status=CANCELLED");

        // 原凭证 isReversed=true
        ErpFinVoucher originalAfter = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(original.getId());
        assertTrue(Boolean.TRUE.equals(originalAfter.getIsReversed()),
                "原 MAINTENANCE_LABOR 凭证应被标记 isReversed=true");

        // 红字凭证存在 + 行同向取负（Dr 6602=-80 / Cr 2211=-80）
        ErpFinVoucher redVoucher = findReversalVoucher("VST-CNL-RV-ML");
        assertNotNull(redVoucher, "应存在 MAINTENANCE_LABOR 红字冲销凭证");
        assertEquals("REVERSAL", redVoucher.getPostingType(), "红字凭证 postingType=REVERSAL");

        ErpFinVoucherLine redDr = findVoucherLine(redVoucher.getId(), SUBJECT_EXPENSE);
        assertNotNull(redDr, "红字凭证 Dr 6602 行存在");
        assertEquals("DEBIT", redDr.getDcDirection(), "方向不变");
        assertTrue(redDr.getDebitAmount().signum() < 0, "红字凭证借方金额为负");
        assertEquals(0, bd("-80").compareTo(redDr.getDebitAmount()), "Dr 6602=-80（同向取负）");

        ErpFinVoucherLine redCr = findVoucherLine(redVoucher.getId(), SUBJECT_PAYABLE);
        assertNotNull(redCr, "红字凭证 Cr 2211 行存在");
        assertEquals("CREDIT", redCr.getDcDirection(), "方向不变");
        assertTrue(redCr.getCreditAmount().signum() < 0, "红字凭证贷方金额为负");
        assertEquals(0, bd("-80").compareTo(redCr.getCreditAmount()), "Cr 2211=-80（同向取负）");
    }

    // ---------- 场景 2：config 关闭 → cancel 零红冲（向后兼容） ----------

    @Test
    public void testCancelNoReversalWhenConfigDisabled() {
        enablePosting(false, "80");
        seedPeriodAndSubjects();
        seedEquipment(EQUIPMENT_ID);

        Long visitId = nextId();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime end = start.plusMinutes(60);
        ormTemplate.runInSession(session -> {
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS,
                    "VST-CNL-OFF", start, end);
            return null;
        });

        assertEquals(0, cancel(visitId).getStatus(), "cancel 应成功");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_CANCELLED, loadVisit(visitId).getStatus());

        assertNull(findVoucher("VST-CNL-OFF-ML"),
                "config 关闭 → cancel 不触发任何 MAINTENANCE_LABOR 凭证动作");
    }

    // ---------- 场景 3：未过账 visit cancel 零红冲（向后兼容） ----------

    @Test
    public void testCancelNoReversalWhenNoVoucher() {
        enablePosting(true, "80");
        seedPeriodAndSubjects();
        seedEquipment(EQUIPMENT_ID);

        Long visitId = nextId();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime end = start.plusMinutes(60);
        ormTemplate.runInSession(session -> {
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS,
                    "VST-CNL-NO-ML", start, end);
            return null;
        });

        // 不调 postLabor，直接 cancel → reverseLabor 经 IErpFinVoucherBiz.reverse 内置幂等守护安全 no-op
        assertEquals(0, cancel(visitId).getStatus(), "cancel 应成功");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_CANCELLED, loadVisit(visitId).getStatus());

        assertNull(findVoucher("VST-CNL-NO-ML-ML"),
                "未过账 visit cancel → 无凭证生成（IErpFinVoucherBiz.reverse 内置无凭证安全 no-op）");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> cancel(Long visitId) {
        return executeRpc(mutation, "ErpMntVisit__cancel",
                ApiRequest.build(java.util.Map.of("visitId", visitId)));
    }

    private ApiResponse<?> executeRpc(io.nop.graphql.core.ast.GraphQLOperationType opType, String action,
                                       ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void enablePosting(boolean enabled, String rate) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMntConstants.CONFIG_LABOR_POSTING_ENABLED, String.valueOf(enabled));
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMntConstants.CONFIG_DEFAULT_LABOR_HOURLY_RATE, rate);
    }

    // ---------- finance / subject seed ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpMdAcctSchema> asDao = daoProvider.daoFor(ErpMdAcctSchema.class);
            ErpMdAcctSchema schema = new ErpMdAcctSchema();
            schema.setId(ACCT_SCHEMA_ID);
            schema.setCode("AS-" + ORG_ID);
            schema.setName("账套" + ORG_ID);
            schema.setOrgId(ORG_ID);
            schema.setNature("FINANCIAL");
            schema.setFunctionalCurrencyId(CURRENCY_ID);
            schema.setStatus("ACTIVE");
            asDao.saveEntity(schema);

            IEntityDao<ErpFinAccountingPeriod> pdao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
            period.setCode("2026-07-CNL");
            period.setName("2026-07-CNL");
            period.setOrgId(ORG_ID);
            period.setYear(2026);
            period.setMonth(7);
            period.setStartDate(LocalDate.of(2026, 7, 1));
            period.setEndDate(LocalDate.of(2026, 7, 31));
            period.setStatus("OPEN");
            pdao.saveEntity(period);

            seedSubject(SUBJECT_EXPENSE, "维修费用", "EXPENSE", "DEBIT");
            seedSubject(SUBJECT_PAYABLE, "应付职工薪酬", "LIABILITY", "CREDIT");
            return null;
        });
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(subjectClass);
        subject.setDirection(direction);
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    // ---------- maintenance seed ----------

    private void seedEquipment(Long id) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment equipment = new ErpMntEquipment();
        equipment.setId(id);
        equipment.setCode("EQ-" + id);
        equipment.setName("设备" + id);
        equipment.setStatus(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
        dao.saveEntity(equipment);
    }

    private void seedVisit(Long id, Long equipmentId, String status, String code,
                           LocalDateTime startTime, LocalDateTime endTime) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        ErpMntVisit visit = new ErpMntVisit();
        visit.setId(id);
        visit.setCode(code);
        visit.setEquipmentId(equipmentId);
        visit.setVisitDate(LocalDate.of(2026, 7, 1));
        visit.setStatus(status);
        visit.setVisitType(ErpMntDaoConstants.VISIT_TYPE_PLANNED);
        visit.setAssignedTo(ASSIGNEE_ID);
        visit.setOrgId(ORG_ID);
        visit.setBusinessDate(LocalDate.of(2026, 7, 1));
        if (startTime != null) {
            visit.setStartTime(Timestamp.valueOf(startTime));
            visit.setTotalMinutes(bd("60"));
        }
        if (endTime != null) {
            visit.setEndTime(Timestamp.valueOf(endTime));
        }
        visit.setPosted(false);
        dao.saveEntity(visit);
    }

    // ---------- query helpers ----------

    private ErpMntVisit loadVisit(Long visitId) {
        return daoProvider.daoFor(ErpMntVisit.class).getEntityById(visitId);
    }

    private ErpFinVoucher findVoucher(String billHeadCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", ErpFinBusinessType.MAINTENANCE_LABOR.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        // 返回 NORMAL 原凭证（reverseProcess 同 billHeadCode 写红字凭证 billR，故需筛选 postingType=NORMAL）
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
            if (v != null && "NORMAL".equals(v.getPostingType())) {
                return v;
            }
        }
        return null;
    }

    private ErpFinVoucher findReversalVoucher(String billHeadCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", ErpFinBusinessType.MAINTENANCE_LABOR.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
            if (v != null && "REVERSAL".equals(v.getPostingType())) {
                return v;
            }
        }
        return null;
    }

    private ErpFinVoucherLine findVoucherLine(Long voucherId, String subjectCode) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
