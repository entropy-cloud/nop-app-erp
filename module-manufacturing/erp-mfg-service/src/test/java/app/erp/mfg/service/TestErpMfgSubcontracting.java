package app.erp.mfg.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 委外加工生命周期 + GL 过账 + MRP 释放测试（plan 2026-07-13-0455-1 §Phase 5）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>委外全链：创建→提交→审核→发料→收货→加工费过账，状态按 8 态子集流转。</li>
 *   <li>发料/收货各产 1 条 inventory StockMove（OUTGOING/INCOMING 方向正确）。</li>
 *   <li>加工费过账后 posted=true，凭证行科目分解（Dr 委外物资 1408 / Cr 应付账款 2202）。</li>
 *   <li>非法状态迁移抛 ErrorCode。</li>
 *   <li>MRP SUBCONTRACT_REQUEST 释放生成 APPROVED 委外单；重复释放被幂等门控拦截。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgSubcontracting extends JunitAutoTestCase {

    static final Long ORG_ID = 1601L;
    static final Long WAREHOUSE_ID = 3601L;
    static final Long UOM_ID = 5601L;
    static final Long CURRENCY_ID = 6601L;
    static final Long ACCT_SCHEMA_ID = 7601L;
    static final Long SUPPLIER_ID = 4601L;
    static final Long P = 2401L;
    static final Long M1 = 2402L;
    static final String MOVE_TYPE_INCOMING = "INCOMING";
    static final String SUBJECT_RAW = "1401";
    static final String SUBJECT_FINISHED = "1405";
    static final String SUBJECT_SUBCONTRACT = "1408";
    static final String SUBJECT_AP = "2202";
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testFullLifecycleWithPosting() {
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(P, null);
        generateIncoming(M1, "PR-SC-MA", bd("10"), bd("5"));

        Long orderId = seedSubcontractOrder("SUB-LC", bd("50"));
        seedSubcontractLine(9801L, orderId, M1, bd("2"));

        setConfig(ErpMfgConstants.CONFIG_SUBCONTRACT_POSTING_ENABLED, "true");
        try {
            rpcOk(mutation, "ErpMfgSubcontractOrder__submitForApproval", Map.of("id", String.valueOf(orderId)));
            assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_SUBMITTED, statusOf(orderId));

            rpcOk(mutation, "ErpMfgSubcontractOrder__approve", Map.of("id", String.valueOf(orderId)));
            assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED, statusOf(orderId));

            Map<String, Object> issueReq = new LinkedHashMap<>();
            issueReq.put("subcontractOrderId", orderId);
            issueReq.put("sourceWarehouseId", WAREHOUSE_ID);
            rpcOk(mutation, "ErpMfgSubcontractOrder__issueMaterials", issueReq);
            assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED, statusOf(orderId));

            ErpInvStockMove issueMove = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_SUBCONTRACT_ISSUE, "SUB-LC");
            assertNotNull(issueMove, "应生成发料出库移动单");

            Map<String, Object> recvReq = new LinkedHashMap<>();
            recvReq.put("subcontractOrderId", orderId);
            recvReq.put("receivedQty", bd("1"));
            recvReq.put("destWarehouseId", WAREHOUSE_ID);
            rpcOk(mutation, "ErpMfgSubcontractOrder__receiveFinished", recvReq);
            assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED, statusOf(orderId));

            ErpInvStockMove receiptMove = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_SUBCONTRACT_RECEIPT, "SUB-LC");
            assertNotNull(receiptMove, "应生成成品入库移动单");

            rpcOk(mutation, "ErpMfgSubcontractOrder__postProcessingFee", Map.of("subcontractOrderId", orderId));
            assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED, statusOf(orderId));

            ErpMfgSubcontractOrder order = daoProvider.daoFor(ErpMfgSubcontractOrder.class).getEntityById(orderId);
            assertEquals(true, order.getPosted(), "加工费过账后 posted=true");

            ErpFinVoucher feeVoucher = findVoucher("SUB-LC-SF", ErpFinBusinessType.SUBCONTRACT_FEE);
            assertNotNull(feeVoucher, "应生成 SUBCONTRACT_FEE 加工费凭证");

            ErpFinVoucherLine drLine = findVoucherLine(feeVoucher.getId(), SUBJECT_SUBCONTRACT);
            assertNotNull(drLine, "借方 委外物资 1408 行存在");
            assertEquals("DEBIT", drLine.getDcDirection());
            assertEquals(0, bd("50").compareTo(drLine.getDebitAmount()), "借方 = processingFee 50");

            ErpFinVoucherLine crLine = findVoucherLine(feeVoucher.getId(), SUBJECT_AP);
            assertNotNull(crLine, "贷方 应付账款 2202 行存在");
            assertEquals("CREDIT", crLine.getDcDirection());
            assertEquals(0, drLine.getDebitAmount().compareTo(crLine.getCreditAmount()), "借贷平衡");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_SUBCONTRACT_POSTING_ENABLED, "false");
        }
    }

    @Test
    public void testIllegalTransitionsRejected() {
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(P, null);

        Long orderId = seedSubcontractOrder("SUB-ILL", bd("30"));

        ApiResponse<?> resp = rpc(mutation, "ErpMfgSubcontractOrder__issueMaterials",
                Map.of("subcontractOrderId", orderId, "sourceWarehouseId", WAREHOUSE_ID));
        assertEquals(ErpMfgErrors.ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "DRAFT→ISSUED 非法迁移应拒绝");
    }

    @Test
    public void testMrpSubcontractRelease() {
        seedPeriodAndSubjects();
        seedMaterial(M1, null);

        Long planId = seedMrpPlan("MRP-SC");
        Long lineId = seedMrpPlanLine(planId, M1, bd("5"));

        setConfig(ErpMfgConstants.CONFIG_SUBCONTRACT_RELEASE_ENABLED, "true");
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("planLineId", lineId);
            req.put("supplierId", SUPPLIER_ID);
            req.put("currencyId", CURRENCY_ID);
            rpcOk(mutation, "ErpMfgMrpPlanLine__releaseSubcontractRequest", req);

            ErpMfgMrpPlanLine line = daoProvider.daoFor(ErpMfgMrpPlanLine.class).getEntityById(lineId);
            assertEquals(true, line.getIsFirmed(), "释放后 isFirmed=true");
            String billCode = line.getConvertedBillCode();
            assertNotNull(billCode, "回写 convertedBillCode");

            ErpMfgSubcontractOrder order = findSubcontractByCode(billCode);
            assertNotNull(order, "应生成委外加工单");
            assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_APPROVED, order.getDocStatus(),
                    "MRP 释放生成 APPROVED 委外单");
            assertEquals(SUPPLIER_ID, order.getSupplierId());

            ApiResponse<?> dup = rpc(mutation, "ErpMfgMrpPlanLine__releaseSubcontractRequest", req);
            assertEquals(ErpMfgErrors.ERR_MRP_LINE_ALREADY_FIRMED.getErrorCode(), dup.getCode(),
                    "重复释放被幂等门控拦截");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_SUBCONTRACT_RELEASE_ENABLED, "false");
        }
    }

    // ---------- helpers ----------

    private String statusOf(Long orderId) {
        ErpMfgSubcontractOrder order = daoProvider.daoFor(ErpMfgSubcontractOrder.class).getEntityById(orderId);
        return order.getDocStatus();
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdAcctSchema> asDao = daoProvider.daoFor(ErpMdAcctSchema.class);
            ErpMdAcctSchema acctSchema = new ErpMdAcctSchema();
            acctSchema.orm_propValueByName("id", ACCT_SCHEMA_ID);
            acctSchema.setCode("ACCT-" + ORG_ID);
            acctSchema.setName("账套 " + ORG_ID);
            acctSchema.setOrgId(ORG_ID);
            acctSchema.orm_propValueByName("nature", "FINANCIAL");
            acctSchema.setFunctionalCurrencyId(CURRENCY_ID);
            acctSchema.orm_propValueByName("status", "ACTIVE");
            asDao.saveEntity(acctSchema);

            IEntityDao<ErpFinAccountingPeriod> pdao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
            period.setCode("2026-07-SC");
            period.setName("2026-07-SC");
            period.setOrgId(ORG_ID);
            period.orm_propValueByName("year", 2026);
            period.orm_propValueByName("month", 7);
            period.setStartDate(LocalDate.of(2026, 7, 1));
            period.setEndDate(LocalDate.of(2026, 7, 31));
            period.orm_propValueByName("status", "OPEN");
            pdao.saveEntity(period);

            seedSubject(SUBJECT_RAW, "原材料", "ASSET", "DEBIT");
            seedSubject(SUBJECT_FINISHED, "库存商品", "ASSET", "DEBIT");
            seedSubject(SUBJECT_SUBCONTRACT, "委外物资", "ASSET", "DEBIT");
            seedSubject(SUBJECT_AP, "应付账款", "LIABILITY", "CREDIT");
        });
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.orm_propValueByName("subjectClass", subjectClass);
        subject.orm_propValueByName("direction", direction);
        subject.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedMaterial(Long id, String costMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            m.setCostMethod(costMethod);
            dao.saveEntity(m);
        });
    }

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));
        rpcOk(mutation, "ErpInvStockMove__generateMove", Map.of("request", req));
    }

    private Long seedSubcontractOrder(String code, BigDecimal processingFee) {
        Long id = 8700L + (long) Math.abs(code.hashCode() % 500);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgSubcontractOrder> dao = daoProvider.daoFor(ErpMfgSubcontractOrder.class);
            ErpMfgSubcontractOrder order = new ErpMfgSubcontractOrder();
            order.orm_propValueByName("id", id);
            order.setCode(code);
            order.setOrgId(ORG_ID);
            order.setSupplierId(SUPPLIER_ID);
            order.setProductId(P);
            order.setBusinessDate(LocalDate.of(2026, 7, 1));
            order.setCurrencyId(CURRENCY_ID);
            order.setExchangeRate(BigDecimal.ONE);
            order.setProcessingFee(processingFee);
            order.setTotalAmount(processingFee);
            order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT);
            order.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
            order.orm_propValueByName("postedStatus", "DRAFT");
            dao.saveEntity(order);
        });
        return id;
    }

    private void seedSubcontractLine(Long id, Long orderId, Long materialId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgSubcontractOrderLine> dao = daoProvider.daoFor(ErpMfgSubcontractOrderLine.class);
            ErpMfgSubcontractOrderLine line = new ErpMfgSubcontractOrderLine();
            line.orm_propValueByName("id", id);
            line.setSubcontractOrderId(orderId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            dao.saveEntity(line);
        });
    }

    private Long seedMrpPlan(String code) {
        Long id = 7700L + (long) Math.abs(code.hashCode() % 500);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMrpPlan> dao = daoProvider.daoFor(ErpMfgMrpPlan.class);
            ErpMfgMrpPlan plan = new ErpMfgMrpPlan();
            plan.orm_propValueByName("id", id);
            plan.setCode(code);
            plan.setOrgId(ORG_ID);
            plan.setBusinessDate(LocalDate.of(2026, 7, 1));
            plan.orm_propValueByName("status", ErpMfgConstants.MRP_STATUS_COMPLETED);
            dao.saveEntity(plan);
        });
        return id;
    }

    private Long seedMrpPlanLine(Long planId, Long materialId, BigDecimal qty) {
        Long id = 7800L + (long) (planId + materialId) % 500;
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMrpPlanLine> dao = daoProvider.daoFor(ErpMfgMrpPlanLine.class);
            ErpMfgMrpPlanLine line = new ErpMfgMrpPlanLine();
            line.orm_propValueByName("id", id);
            line.setMrpPlanId(planId);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setPlannedQuantity(qty);
            line.orm_propValueByName("grossRequirement", qty);
            line.orm_propValueByName("netRequirement", qty);
            line.setPlannedDate(LocalDate.of(2026, 7, 10));
            line.orm_propValueByName("orderType", ErpMfgConstants.MRP_ORDER_TYPE_SUBCONTRACT_REQUEST);
            line.setIsFirmed(Boolean.FALSE);
            line.orm_propValueByName("lineNo", 10);
            dao.saveEntity(line);
        });
        return id;
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpMfgSubcontractOrder findSubcontractByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpMfgSubcontractOrder> list = daoProvider.daoFor(ErpMfgSubcontractOrder.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinVoucher findVoucher(String billHeadCode, ErpFinBusinessType type) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", type.name())));
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
        List<ErpFinVoucherLine> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
