package app.erp.mnt.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntSparePartUsageLine;
import app.erp.mnt.service.posting.MaintenanceIssuePostingDispatcher;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 备件消耗 reverseConfirm 红冲测试（plan 2026-07-18-1745-1 Phase 3）。
 *
 * <p>覆盖 {@code ErpMntSparePartUsageBizModel.reverseConfirm}：confirm 产 MAINTENANCE_ISSUE 凭证 + OUTGOING 移动后，
 * 调 reverseConfirm → 红冲凭证（原 isReversed=true + 红字凭证同向取负）+ 反向移动单（库存域
 * {@code IErpInvStockMoveBiz.reverse} 生成 REVERSAL 移动单）+ posted=false + docStatus=CANCELLED。
 *
 * <p>镜像 {@code TestErpMfgSubcontractReverse} 范式（红冲正路径 + 非法态守卫）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntSparePartUsageReversal extends JunitAutoTestCase {

    static final Long ORG_ID = 1351L;
    static final Long EQUIPMENT_ID = 1351L;
    static final Long WAREHOUSE_ID = 3351L;
    static final Long UOM_ID = 5351L;
    static final Long CURRENCY_ID = 6351L;
    static final Long ACCT_SCHEMA_ID = 7351L;
    static final Long M1 = 4351L;

    static final String SUBJECT_INVENTORY = "1403";
    static final String SUBJECT_EXPENSE = "6602";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    MaintenanceIssuePostingDispatcher issuePostingDispatcher;

    private final AtomicLong idSeq = new AtomicLong(200300L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    @AfterEach
    void resetPostingConfig() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMntConstants.CONFIG_SPARE_PART_POSTING_ENABLED,
                String.valueOf(ErpMntConstants.DEFAULT_SPARE_PART_POSTING_ENABLED));
    }

    // ---------- 场景 1：confirm → reverseConfirm 正路径（红冲凭证 + 反向移动单） ----------

    @Test
    public void testReverseConfirmRedReversesVoucherAndMove() {
        enablePosting(true);
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedEquipment(EQUIPMENT_ID);
        seedStock("SEED-MNT-RV", M1, bd("20"), bd("5"));

        Long usageId = nextId();
        Long lineId = nextId();
        ormTemplate.runInSession(session -> {
            seedUsage(usageId, EQUIPMENT_ID, "SP-RV-MAIN");
            seedUsageLine(lineId, usageId, M1, bd("10"), bd("5"));
            return null;
        });

        // confirm 产 MAINTENANCE_ISSUE 凭证 + OUTGOING 移动 + posted=true
        assertEquals(0, confirm(usageId).getStatus(), "confirm 应成功");
        ErpMntSparePartUsage confirmed = loadUsage(usageId);
        assertTrue(Boolean.TRUE.equals(confirmed.getPosted()), "前置：posted=true");
        assertEquals(ErpMntDaoConstants.DOC_STATUS_ACTIVE, confirmed.getDocStatus());

        ErpFinVoucher original = findVoucher("SP-RV-MAIN-MI");
        assertNotNull(original, "前置：应存在 MAINTENANCE_ISSUE 凭证");
        ErpInvStockMove originalMove = findMove("SP-RV-MAIN");
        assertNotNull(originalMove, "前置：应存在 OUTGOING 移动单");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, originalMove.getDocStatus());

        // 执行 reverseConfirm
        assertEquals(0, reverseConfirm(usageId).getStatus(), "reverseConfirm 应成功");

        // 1. 状态翻转：posted=false + docStatus=CANCELLED
        ErpMntSparePartUsage reversed = loadUsage(usageId);
        assertFalse(Boolean.TRUE.equals(reversed.getPosted()), "红冲后 posted=false");
        assertEquals(ErpMntDaoConstants.DOC_STATUS_CANCELLED, reversed.getDocStatus(),
                "红冲后 docStatus=CANCELLED");

        // 2. 原 MAINTENANCE_ISSUE 凭证 isReversed=true
        ErpFinVoucher originalAfter = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(original.getId());
        assertTrue(Boolean.TRUE.equals(originalAfter.getIsReversed()),
                "原 MAINTENANCE_ISSUE 凭证应被标记 isReversed=true");

        // 3. 红字凭证存在（同 billHeadCode，postingType=REVERSAL）
        ErpFinVoucher redVoucher = findReversalVoucher("SP-RV-MAIN-MI");
        assertNotNull(redVoucher, "应存在 MAINTENANCE_ISSUE 红字凭证");
        assertEquals("REVERSAL", redVoucher.getPostingType());

        // 4. 反向 OUTGOING 移动单（REVERSAL 移动单，relatedBillType=REVERSAL）
        ErpInvStockMove reversalMove = findReversalMove(originalMove.getCode());
        assertNotNull(reversalMove, "应存在 REVERSAL 反向冲销移动单");
        assertEquals("REVERSAL", reversalMove.getRelatedBillType(),
                "REVERSAL 移动单 relatedBillType=REVERSAL");
        assertEquals(originalMove.getCode(), reversalMove.getRelatedBillCode(),
                "REVERSAL 移动单 relatedBillCode 指向原移动单 code");
    }

    // ---------- 场景 2：未过账守卫（confirm 前 reverseConfirm 抛 ERR_SPARE_PART_USAGE_NOT_POSTED） ----------

    @Test
    public void testReverseConfirmRejectsNotPosted() {
        enablePosting(true);
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedEquipment(EQUIPMENT_ID);
        seedStock("SEED-MNT-REJ", M1, bd("20"), bd("5"));

        Long usageId = nextId();
        Long lineId = nextId();
        ormTemplate.runInSession(session -> {
            seedUsage(usageId, EQUIPMENT_ID, "SP-RV-REJ");
            seedUsageLine(lineId, usageId, M1, bd("10"), bd("5"));
            return null;
        });

        // 未 confirm 直接 reverseConfirm → 守卫拒绝
        ApiResponse<?> resp = reverseConfirm(usageId);
        assertEquals(ErpMntErrors.ERR_SPARE_PART_USAGE_NOT_POSTED.getErrorCode(), resp.getCode(),
                "未过账消耗单 reverseConfirm 应被守卫拒绝");

        // 状态不变（守卫前置，未进入红冲步骤）
        ErpMntSparePartUsage usage = loadUsage(usageId);
        assertFalse(Boolean.TRUE.equals(usage.getPosted()));
        assertEquals(ErpMntDaoConstants.DOC_STATUS_DRAFT, usage.getDocStatus());
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> confirm(Long usageId) {
        return executeRpc(mutation, "ErpMntSparePartUsage__confirm",
                ApiRequest.build(Map.of("usageId", usageId)));
    }

    private ApiResponse<?> reverseConfirm(Long usageId) {
        return executeRpc(mutation, "ErpMntSparePartUsage__reverseConfirm",
                ApiRequest.build(Map.of("usageId", usageId)));
    }

    private ApiResponse<?> executeRpc(io.nop.graphql.core.ast.GraphQLOperationType opType, String action,
                                       ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void enablePosting(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpMntConstants.CONFIG_SPARE_PART_POSTING_ENABLED, String.valueOf(value));
    }

    // ---------- finance / material / stock seed ----------

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
            period.setCode("2026-07-RV");
            period.setName("2026-07-RV");
            period.setOrgId(ORG_ID);
            period.setYear(2026);
            period.setMonth(7);
            period.setStartDate(LocalDate.of(2026, 7, 1));
            period.setEndDate(LocalDate.of(2026, 7, 31));
            period.setStatus("OPEN");
            pdao.saveEntity(period);

            seedSubject(SUBJECT_INVENTORY, "存货", "ASSET", "DEBIT");
            seedSubject(SUBJECT_EXPENSE, "维修费用", "EXPENSE", "DEBIT");
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

    private void seedMaterial(Long id, String costMethod) {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("备件 " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            m.setCostMethod(costMethod);
            dao.saveEntity(m);
            return null;
        });
    }

    private void seedStock(String billCode, Long materialId, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", "INCOMING");
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("relatedBillType", "SEED_STOCK");
        req.put("relatedBillCode", billCode);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));
        assertEquals(0, resp.getStatus(), "seedStock generateMove 应成功");
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

    private void seedUsage(Long id, Long equipmentId, String code) {
        IEntityDao<ErpMntSparePartUsage> dao = daoProvider.daoFor(ErpMntSparePartUsage.class);
        ErpMntSparePartUsage usage = new ErpMntSparePartUsage();
        usage.setId(id);
        usage.setCode(code);
        usage.setEquipmentId(equipmentId);
        usage.setWarehouseId(WAREHOUSE_ID);
        usage.setOrgId(ORG_ID);
        usage.setBusinessDate(LocalDate.of(2026, 7, 1));
        usage.setDocStatus(ErpMntDaoConstants.DOC_STATUS_DRAFT);
        usage.setApproveStatus(ErpMntConstants.APPROVE_STATUS_UNSUBMITTED);
        usage.setPosted(false);
        dao.saveEntity(usage);
    }

    private void seedUsageLine(Long id, Long usageId, Long materialId, BigDecimal qty, BigDecimal unitCost) {
        IEntityDao<ErpMntSparePartUsageLine> dao = daoProvider.daoFor(ErpMntSparePartUsageLine.class);
        ErpMntSparePartUsageLine line = new ErpMntSparePartUsageLine();
        line.setId(id);
        line.setSparePartUsageId(usageId);
        line.setLineNo(materialId.intValue());
        line.setMaterialId(materialId);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitCost(unitCost);
        line.setAmount(qty.multiply(unitCost));
        dao.saveEntity(line);
    }

    // ---------- query helpers ----------

    private ErpMntSparePartUsage loadUsage(Long usageId) {
        return daoProvider.daoFor(ErpMntSparePartUsage.class).getEntityById(usageId);
    }

    private ErpInvStockMove findMove(String usageCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpMntConstants.RELATED_BILL_TYPE_MNT_SPARE_PART));
        q.addFilter(eq("relatedBillCode", usageCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpInvStockMove findReversalMove(String originalMoveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", "REVERSAL"));
        q.addFilter(eq("relatedBillCode", originalMoveCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpFinVoucher findVoucher(String billHeadCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", ErpFinBusinessType.MAINTENANCE_ISSUE.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
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
                eq("businessType", ErpFinBusinessType.MAINTENANCE_ISSUE.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
            if (v != null && "REVERSAL".equals(v.getPostingType())) {
                return v;
            }
        }
        return null;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
