package app.erp.mnt.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.posting.MaintenanceLaborAcctDocProvider;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 维修工时费用化 GL 过账测试（plan 2026-07-18-0949-1 Phase 3）。
 *
 * <p>覆盖 MAINTENANCE_LABOR 凭证生成（Dr: 折旧费用 6602 / Cr: 应付职工薪酬 2211），config 门控
 *（默认 false 向后兼容），totalMinutes=0 跳过，rate=0 跳过，以及 Provider 单元分派。镜像
 * {@code TestErpMntSparePartPosting} 范式（flat service/ 包路径）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntLaborPosting extends JunitAutoTestCase {

    static final Long ORG_ID = 2251L;
    static final Long EQUIPMENT_ID = 251L;
    static final Long ASSIGNEE_ID = 2252L;
    static final Long CURRENCY_ID = 2251L;
    static final Long ACCT_SCHEMA_ID = 2251L;

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

    private final AtomicLong idSeq = new AtomicLong(300000L);

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

    // ---------- 场景 1：正路径 complete → 60min × 80/hr → MAINTENANCE_LABOR 凭证 ----------

    @Test
    public void testLaborPostingBasic() {
        enablePosting(true, "80");
        seedPeriodAndSubjects();
        seedEquipment(EQUIPMENT_ID);

        Long visitId = nextId();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime end = start.plusMinutes(60);
        ormTemplate.runInSession(session -> {
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS,
                    "VST-LBR-BASIC", start, end);
            return null;
        });

        assertEquals(0, complete(visitId).getStatus(), "complete 应成功");

        ErpMntVisit completed = loadVisit(visitId);
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_COMPLETED, completed.getStatus());
        assertEquals(0, bd("60").compareTo(completed.getTotalMinutes()), "totalMinutes=60");

        ErpFinVoucher voucher = findVoucher("VST-LBR-BASIC-ML");
        assertNotNull(voucher, "应生成 MAINTENANCE_LABOR 凭证");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus());

        ErpFinVoucherLine drLine = findVoucherLine(voucher.getId(), SUBJECT_EXPENSE);
        assertNotNull(drLine, "借方 折旧费用 6602 行存在");
        assertEquals("DEBIT", drLine.getDcDirection());
        assertEquals(0, bd("80").compareTo(drLine.getDebitAmount()),
                "借方 = 60×80/60 = 80");

        ErpFinVoucherLine crLine = findVoucherLine(voucher.getId(), SUBJECT_PAYABLE);
        assertNotNull(crLine, "贷方 应付职工薪酬 2211 行存在");
        assertEquals("CREDIT", crLine.getDcDirection());
        assertEquals(0, drLine.getDebitAmount().compareTo(crLine.getCreditAmount()), "借贷平衡");
    }

    // ---------- 场景 2：部分时长 totalMinutes=30 → Dr/Cr = 40 ----------

    @Test
    public void testLaborPostingPartialMinutes() {
        enablePosting(true, "80");
        seedPeriodAndSubjects();
        seedEquipment(EQUIPMENT_ID);

        Long visitId = nextId();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime end = start.plusMinutes(30);
        ormTemplate.runInSession(session -> {
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS,
                    "VST-LBR-PART", start, end);
            return null;
        });

        assertEquals(0, complete(visitId).getStatus(), "complete 应成功");

        ErpFinVoucher voucher = findVoucher("VST-LBR-PART-ML");
        assertNotNull(voucher, "部分时长也应生成凭证");

        ErpFinVoucherLine drLine = findVoucherLine(voucher.getId(), SUBJECT_EXPENSE);
        assertEquals(0, bd("40").compareTo(drLine.getDebitAmount()),
                "借方 = 30×80/60 = 40");
    }

    // ---------- 场景 3：totalMinutes=0 跳过过账 ----------

    @Test
    public void testLaborPostingZeroMinutesSkipped() {
        enablePosting(true, "80");
        seedPeriodAndSubjects();
        seedEquipment(EQUIPMENT_ID);

        Long visitId = nextId();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 10, 0);
        ormTemplate.runInSession(session -> {
            // startTime=null → doComplete 不计算 totalMinutes（保持 null）→ postLabor 守卫跳过
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS,
                    "VST-LBR-ZERO", null, start);
            return null;
        });

        assertEquals(0, complete(visitId).getStatus(), "complete 应成功");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_COMPLETED, loadVisit(visitId).getStatus());

        assertNull(findVoucher("VST-LBR-ZERO-ML"), "totalMinutes 缺失 → 不生成 MAINTENANCE_LABOR 凭证");
    }

    // ---------- 场景 4：config-gated 关闭零回归 ----------

    @Test
    public void testLaborPostingConfigDisabled() {
        enablePosting(false, "80");
        seedPeriodAndSubjects();
        seedEquipment(EQUIPMENT_ID);

        Long visitId = nextId();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime end = start.plusMinutes(60);
        ormTemplate.runInSession(session -> {
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS,
                    "VST-LBR-OFF", start, end);
            return null;
        });

        assertEquals(0, complete(visitId).getStatus(), "complete 应成功");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_COMPLETED, loadVisit(visitId).getStatus());

        assertNull(findVoucher("VST-LBR-OFF-ML"), "config 关闭 → 不生成 MAINTENANCE_LABOR 凭证");
    }

    // ---------- 场景 5：rate=0 跳过过账 ----------

    @Test
    public void testLaborPostingRateZeroSkipped() {
        enablePosting(true, "0");
        seedPeriodAndSubjects();
        seedEquipment(EQUIPMENT_ID);

        Long visitId = nextId();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime end = start.plusMinutes(60);
        ormTemplate.runInSession(session -> {
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS,
                    "VST-LBR-RATE0", start, end);
            return null;
        });

        assertEquals(0, complete(visitId).getStatus(), "complete 应成功");

        assertNull(findVoucher("VST-LBR-RATE0-ML"),
                "rate=0 → 跳过过账，不生成凭证（不抛错）");
    }

    // ---------- 场景 6：Provider createFacts 单元 ----------

    @Test
    public void testLaborAcctDocProviderFacts() {
        MaintenanceLaborAcctDocProvider provider = new MaintenanceLaborAcctDocProvider();

        // 业务类型分派
        assertTrue(provider.getSupportedBusinessTypes()
                .contains(ErpFinBusinessType.MAINTENANCE_LABOR),
                "Provider 应支持 MAINTENANCE_LABOR");
        assertEquals(1, provider.getSupportedBusinessTypes().size(),
                "仅支持 MAINTENANCE_LABOR（不污染其他业务类型 Provider 路由）");

        // 正路径 createFacts（KEY_* 为 Provider 包级常量，测试用字面量引用以匹配既有 1100-6 范式）
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.MAINTENANCE_LABOR);
        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put("TOTAL", bd("120"));
        billData.put("EQUIPMENT_CODE", "EQ-251");
        billData.put("VISIT_CODE", "VST-UNIT-001");
        event.setBillData(billData);

        List<?> facts = provider.createFacts(event, new AcctDocContext());
        assertEquals(2, facts.size(), "Dr 6602 + Cr 2211 = 2 行");

        // total=0 守卫
        billData.put("TOTAL", BigDecimal.ZERO);
        event.setBillData(billData);
        assertEquals(0, provider.createFacts(event, new AcctDocContext()).size(),
                "total=0 → 返回空 facts");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> complete(Long visitId) {
        return executeRpc(mutation, "ErpMntVisit__complete",
                ApiRequest.build(Map.of("visitId", visitId)));
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
            period.setCode("2026-07-LBR");
            period.setName("2026-07-LBR");
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
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
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
