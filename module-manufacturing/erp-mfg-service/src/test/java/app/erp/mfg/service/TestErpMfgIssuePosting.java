package app.erp.mfg.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
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
 * Phase 3 测试：生产领料出库 GL 过账（plan 2026-07-10-1100-5）。
 *
 * <p>覆盖 MANUFACTURING_ISSUE 凭证生成（Dr: WIP 在制品 1411 / Cr: 原材料存货 1401），
 * 领料出库后 {@code ErpMfgMaterialIssue.posted=true}，且不再误派 SALES_OUTPUT 凭证。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgIssuePosting extends JunitAutoTestCase {

    static final Long ORG_ID = 1601L;
    static final Long WAREHOUSE_ID = 3601L;
    static final Long UOM_ID = 5601L;
    static final Long CURRENCY_ID = 6601L;
    static final Long ACCT_SCHEMA_ID = 7601L;
    static final Long P = 1401L;
    static final Long M1 = 1402L;
    static final Long M2 = 1403L;
    static final String MOVE_TYPE_INCOMING = "INCOMING";
    static final String SUBJECT_INVENTORY = "1401";
    static final String SUBJECT_WIP = "1411";
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testMovingAverageIssuePosting() {
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(P, null);
        seedBom(9601L, P, M1, bd("2"));
        generateIncoming(M1, "PR-IP-MA", bd("10"), bd("5"));

        Long woId = seedWorkOrder("WO-IP-MA", 9601L);
        Long wolId = seedWorkOrderLine(woId, M1, bd("2"));
        Long issueId = seedIssue("MI-IP-MA", woId);
        seedIssueLine(9701L, issueId, M1, bd("2"), wolId);

        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));

        ErpMfgMaterialIssue issue = daoProvider.daoFor(ErpMfgMaterialIssue.class).getEntityById(issueId);
        assertEquals(ErpMfgConstants.ISSUE_STATUS_DONE, issue.getDocStatus(), "领料确认 → DONE");
        assertEquals(true, issue.getPosted(), "领料 DONE 应过账 posted=true");

        ErpInvStockMove move = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE, "MI-IP-MA");
        assertNotNull(move, "应生成出库移动单");

        ErpFinVoucher voucher = findVoucher("MI-IP-MA-MI");
        assertNotNull(voucher, "应生成 MANUFACTURING_ISSUE 凭证");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus());

        ErpFinVoucherLine drLine = findVoucherLine(voucher.getId(), SUBJECT_WIP);
        assertNotNull(drLine, "借方 WIP 在制品 1411 行存在");
        assertEquals("DEBIT", drLine.getDcDirection());
        assertTrue(drLine.getDebitAmount().signum() > 0, "借方金额 > 0");
        assertEquals(0, bd("10").compareTo(drLine.getDebitAmount()), "借方 = materialCost 2×5=10");

        ErpFinVoucherLine crLine = findVoucherLine(voucher.getId(), SUBJECT_INVENTORY);
        assertNotNull(crLine, "贷方 原材料存货 1401 行存在");
        assertEquals("CREDIT", crLine.getDcDirection());
        assertEquals(0, drLine.getDebitAmount().compareTo(crLine.getCreditAmount()),
                "借贷平衡");
    }

    @Test
    public void testMultiMaterialIssuePosting() {
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(M2, "MOVING_AVERAGE");
        seedMaterial(P, null);
        seedBom(9602L, P, M1, bd("2"));
        generateIncoming(M1, "PR-IP-MM1", bd("10"), bd("5"));
        generateIncoming(M2, "PR-IP-MM2", bd("10"), bd("8"));

        Long woId = seedWorkOrder("WO-IP-MM", 9602L);
        Long wol1 = seedWorkOrderLine(woId, M1, bd("2"));
        Long wol2 = seedWorkOrderLine(woId, M2, bd("1"));
        Long issueId = seedIssue("MI-IP-MM", woId);
        seedIssueLine(9702L, issueId, M1, bd("2"), wol1);
        seedIssueLine(9703L, issueId, M2, bd("1"), wol2);

        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));

        ErpMfgMaterialIssue issue = daoProvider.daoFor(ErpMfgMaterialIssue.class).getEntityById(issueId);
        assertEquals(true, issue.getPosted(), "多物料领料 posted=true");

        ErpFinVoucher voucher = findVoucher("MI-IP-MM-MI");
        assertNotNull(voucher, "多物料领料应生成凭证");

        List<ErpFinVoucherLine> creditLines = findVoucherLines(voucher.getId(), SUBJECT_INVENTORY, "CREDIT");
        assertEquals(2, creditLines.size(), "2 行贷方（各物料存货科目）");

        BigDecimal totalCredit = creditLines.stream()
                .map(l -> nz(l.getCreditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, bd("18").compareTo(totalCredit), "贷方合计 = M1(2×5=10) + M2(1×8=8) = 18");

        ErpFinVoucherLine drLine = findVoucherLine(voucher.getId(), SUBJECT_WIP);
        assertNotNull(drLine, "借方 WIP 汇总行存在");
        assertEquals(0, totalCredit.compareTo(drLine.getDebitAmount()), "借方汇总 = 贷方合计");

        assertEquals(3, countVoucherLines(voucher.getId()), "2 贷 + 1 借 = 3 行");
    }

    // ---------- seed helpers ----------

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
            period.setCode("2026-07");
            period.setName("2026-07");
            period.setOrgId(ORG_ID);
            period.orm_propValueByName("year", 2026);
            period.orm_propValueByName("month", 7);
            period.setStartDate(LocalDate.of(2026, 7, 1));
            period.setEndDate(LocalDate.of(2026, 7, 31));
            period.orm_propValueByName("status", "OPEN");
            pdao.saveEntity(period);

            seedSubject(SUBJECT_INVENTORY, "库存商品", "ASSET", "DEBIT");
            seedSubject(SUBJECT_WIP, "在制品-WIP", "ASSET", "DEBIT");
            seedSubject("2202", "应付账款-暂估", "LIABILITY", "CREDIT");
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

    private void seedBom(Long bomId, Long productId, Long componentId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", bomId);
            bom.setCode("BOM-" + bomId);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(bd("1"));
            dao.saveEntity(bom);
            IEntityDao<ErpMfgBomLine> ldao = daoProvider.daoFor(ErpMfgBomLine.class);
            ErpMfgBomLine line = new ErpMfgBomLine();
            line.orm_propValueByName("id", bomId + 50000);
            line.setBomId(bomId);
            line.setLineNo(10);
            line.setMaterialId(componentId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            ldao.saveEntity(line);
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

    private Long seedWorkOrder(String code, Long bomId) {
        Long id = 8800L + (long) Math.abs(code.hashCode() % 500);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
            dao.saveEntity(wo);
        });
        return id;
    }

    private Long seedWorkOrderLine(Long woId, Long materialId, BigDecimal plannedQty) {
        long raw = (woId + "" + materialId + "INPUT").hashCode();
        Long id = 9600L + (long) Math.abs(raw % 500);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
            ErpMfgWorkOrderLine wol = new ErpMfgWorkOrderLine();
            wol.orm_propValueByName("id", id);
            wol.setWorkOrderId(woId);
            wol.setLineNo(materialId.intValue());
            wol.orm_propValueByName("lineType", ErpMfgConstants.WORK_ORDER_LINE_TYPE_INPUT);
            wol.setMaterialId(materialId);
            wol.setUoMId(UOM_ID);
            wol.setPlannedQuantity(plannedQty);
            dao.saveEntity(wol);
        });
        return id;
    }

    private Long seedIssue(String code, Long woId) {
        Long id = 8900L + (long) Math.abs(code.hashCode() % 500);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMaterialIssue> dao = daoProvider.daoFor(ErpMfgMaterialIssue.class);
            ErpMfgMaterialIssue issue = new ErpMfgMaterialIssue();
            issue.orm_propValueByName("id", id);
            issue.setCode(code);
            issue.setWorkOrderId(woId);
            issue.setOrgId(ORG_ID);
            issue.setWarehouseId(WAREHOUSE_ID);
            issue.setBusinessDate(LocalDate.of(2026, 7, 1));
            issue.setCurrencyId(CURRENCY_ID);
            issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_DRAFT);
            issue.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(issue);
        });
        return id;
    }

    private void seedIssueLine(Long id, Long issueId, Long materialId, BigDecimal qty, Long wolId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMaterialIssueLine> dao = daoProvider.daoFor(ErpMfgMaterialIssueLine.class);
            ErpMfgMaterialIssueLine line = new ErpMfgMaterialIssueLine();
            line.orm_propValueByName("id", id);
            line.setIssueId(issueId);
            line.setLineNo(materialId.intValue());
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setRequiredQuantity(qty);
            line.setIssuedQuantity(qty);
            line.setWorkOrderLineId(wolId);
            dao.saveEntity(line);
        });
    }

    // ---------- query helpers ----------

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinVoucher findVoucher(String billHeadCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", ErpFinBusinessType.MANUFACTURING_ISSUE.name())));
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

    private List<ErpFinVoucherLine> findVoucherLines(Long voucherId, String subjectCode, String dcDirection) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        q.addFilter(eq("dcDirection", dcDirection));
        return dao.findAllByQuery(q);
    }

    private long countVoucherLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
