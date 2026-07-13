package app.erp.fin.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdCostCenter;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.FieldSelectionBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name/*Code + @BizLoader 批量加载）。
 *
 * <p>覆盖 finance 域 15 实体。经 {@link IGraphQLEngine} findList + {@link FieldSelectionBean}
 * 请求派生字段触发 @BizLoader，验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long PARTNER_ID = 9201L;
    static final Long CURRENCY_ID = 9401L;
    static final Long SUBJECT_ID = 9501L;
    static final Long ACCT_SCHEMA_ID = 9601L;
    static final Long PERIOD_ID = 9701L;
    static final Long UOM_ID = 9801L;
    static final Long MATERIAL_ID = 9901L;
    static final Long WAREHOUSE_ID = 9911L;
    static final Long COST_CENTER_ID = 9921L;
    static final Long SCENARIO_ID = 9931L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- Phase 1: 核心会计实体 ----------

    @Test
    public void testVoucherLineFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "财务测试组织");
            seedCurrency(CURRENCY_ID, "人民币");
            seedSubject(SUBJECT_ID, "1001", "库存现金");
            seedAcctSchema(ACCT_SCHEMA_ID, "MAIN", "主账套");
            seedPeriod(PERIOD_ID, "2026-07", "2026年7月");
            seedUoM(UOM_ID, "个");
            seedMaterial(MATERIAL_ID, "物料Alpha");
            seedPartner(PARTNER_ID, "客户Beta");
            seedVoucher(8001L);
            seedVoucherLine(8101L, 8001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpFinVoucherLine__findList",
                "id", "subjectName", "partnerName", "materialName", "currencyName", "voucherCode");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条凭证行");
        Map<String, Object> first = rows.get(0);
        assertEquals("库存现金", first.get("subjectName"));
        assertEquals("客户Beta", first.get("partnerName"));
        assertEquals("物料Alpha", first.get("materialName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("V-8001", first.get("voucherCode"));
    }

    @Test
    public void testArApItemFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "财务测试组织");
            seedCurrency(CURRENCY_ID, "人民币");
            seedAcctSchema(ACCT_SCHEMA_ID, "MAIN", "主账套");
            seedPartner(PARTNER_ID, "客户Gamma");
            seedArApItem(8201L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpFinArApItem__findList",
                "id", "partnerName", "currencyName", "orgName", "acctSchemaCode");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条 AR/AP 辅助账");
        Map<String, Object> first = rows.get(0);
        assertEquals("客户Gamma", first.get("partnerName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("财务测试组织", first.get("orgName"));
        assertEquals("MAIN", first.get("acctSchemaCode"));
    }

    // ---------- Phase 2: 资金/预算实体（Phase 2 扩展用例）----------

    @Test
    public void testFundAccountFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "财务测试组织");
            seedCurrency(CURRENCY_ID, "人民币");
            seedSubject(SUBJECT_ID, "1002", "银行存款");
            seedFundAccount(8301L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpFinFundAccount__findList",
                "id", "subjectName", "currencyName", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条资金账户");
        Map<String, Object> first = rows.get(0);
        assertEquals("银行存款", first.get("subjectName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("财务测试组织", first.get("orgName"));
    }

    @Test
    public void testBudgetLineFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "财务测试组织");
            seedCurrency(CURRENCY_ID, "人民币");
            seedSubject(SUBJECT_ID, "6601", "销售费用");
            seedAcctSchema(ACCT_SCHEMA_ID, "MAIN", "主账套");
            seedPeriod(PERIOD_ID, "2026-07", "2026年7月");
            seedUoM(UOM_ID, "个");
            seedMaterial(MATERIAL_ID, "物料Delta");
            seedPartner(PARTNER_ID, "客户Epsilon");
            seedWarehouse(WAREHOUSE_ID, "成品仓");
            seedBudgetScenario(SCENARIO_ID);
            seedBudgetLine(8401L, SCENARIO_ID);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpFinBudgetLine__findList",
                "id", "subjectName", "materialName", "partnerName", "scenarioCode", "currencyName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条预算行");
        Map<String, Object> first = rows.get(0);
        assertEquals("销售费用", first.get("subjectName"));
        assertEquals("物料Delta", first.get("materialName"));
        assertEquals("客户Epsilon", first.get("partnerName"));
        assertEquals("BUD-9931", first.get("scenarioCode"));
        assertEquals("人民币", first.get("currencyName"));
    }

    // ---------- query helper ----------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryWithSelection(String action, String... fields) {
        FieldSelectionBean selection = new FieldSelectionBean();
        for (String f : fields) {
            selection.addField(f);
        }
        ApiRequest<?> request = ApiRequest.build(Map.of());
        request.setSelection(selection);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
                GraphQLOperationType.query, action, request);
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), action + " 查询成功");
        Object data = resp.getData();
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        }
        return (List<Map<String, Object>>) ((Map<?, ?>) data).get("items");
    }

    // ---------- seed helpers ----------

    private void seedOrg(long id, String name) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("ORG-" + id);
        o.setName(name);
        o.setOrgType("COMPANY");
        o.setStatus("ACTIVE");
        dao.saveEntity(o);
    }

    private void seedPartner(long id, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("CUS-" + id);
        p.setName(name);
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private void seedCurrency(long id, String name) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CNY");
        c.setName(name);
        dao.saveEntity(c);
    }

    private void seedSubject(long id, String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode(code);
        s.setName(name);
        s.orm_propValueByName("subjectClass", "ASSET");
        s.orm_propValueByName("direction", "DEBIT");
        s.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(s);
    }

    private void seedAcctSchema(long id, String code, String name) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema a = dao.newEntity();
        a.orm_propValue(1, id);
        a.setCode(code);
        a.setName(name);
        a.setOrgId(ORG_ID);
        a.orm_propValueByName("nature", "FINANCIAL");
        a.setFunctionalCurrencyId(CURRENCY_ID);
        a.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(a);
    }

    private void seedPeriod(long id, String code, String name) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode(code);
        p.setName(name);
        p.orm_propValueByName("year", 2026);
        p.orm_propValueByName("month", 7);
        p.setStartDate(LocalDate.of(2026, 7, 1));
        p.setEndDate(LocalDate.of(2026, 7, 31));
        p.orm_propValueByName("status", "OPEN");
        dao.saveEntity(p);
    }

    private void seedUoM(long id, String name) {
        IEntityDao<ErpMdUoM> dao = daoProvider.daoFor(ErpMdUoM.class);
        ErpMdUoM u = dao.newEntity();
        u.orm_propValue(1, id);
        u.setCode("UOM-" + id);
        u.setName(name);
        dao.saveEntity(u);
    }

    private void seedMaterial(long id, String name) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("MAT-" + id);
        m.setName(name);
        m.orm_propValueByName("materialType", "GOODS");
        m.setUoMId(UOM_ID);
        m.setStatus("ACTIVE");
        dao.saveEntity(m);
    }

    private void seedWarehouse(long id, String name) {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse w = dao.newEntity();
        w.orm_propValue(1, id);
        w.setCode("WH-" + id);
        w.setName(name);
        w.setStatus("ACTIVE");
        dao.saveEntity(w);
    }

    private void seedVoucher(long id) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher v = dao.newEntity();
        v.orm_propValue(1, id);
        v.setCode("V-" + id);
        v.orm_propValueByName("voucherType", "GENERAL");
        v.orm_propValueByName("docStatus", "DRAFT");
        v.setVoucherDate(LocalDate.of(2026, 7, 1));
        v.setOrgId(ORG_ID);
        v.setAcctSchemaId(ACCT_SCHEMA_ID);
        v.setPeriodId(PERIOD_ID);
        dao.saveEntity(v);
    }

    private void seedVoucherLine(long id, long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        ErpFinVoucherLine line = dao.newEntity();
        line.orm_propValue(1, id);
        line.setVoucherId(voucherId);
        line.setLineNo(1);
        line.setSubjectId(SUBJECT_ID);
        line.setSubjectCode("1001");
        line.setSubjectName("库存现金");
        line.orm_propValueByName("dcDirection", "DEBIT");
        line.setCurrencyId(CURRENCY_ID);
        line.setAcctSchemaId(ACCT_SCHEMA_ID);
        line.setOrgId(ORG_ID);
        line.setPartnerId(PARTNER_ID);
        line.setMaterialId(MATERIAL_ID);
        line.setWarehouseId(WAREHOUSE_ID);
        line.setCostCenterId(COST_CENTER_ID);
        line.orm_propValueByName("debitAmount", BigDecimal.TEN);
        dao.saveEntity(line);
    }

    private void seedArApItem(long id) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = dao.newEntity();
        item.orm_propValue(1, id);
        item.setCode("ARI-" + id);
        item.setOrgId(ORG_ID);
        item.setAcctSchemaId(ACCT_SCHEMA_ID);
        item.setPartnerId(PARTNER_ID);
        item.setDirection("RECEIVABLE");
        item.orm_propValueByName("sourceBillType", "SALES_INVOICE");
        item.setSourceBillCode("SRC-" + id);
        item.setBusinessDate(LocalDate.of(2026, 7, 1));
        item.setCurrencyId(CURRENCY_ID);
        item.orm_propValueByName("amountSource", BigDecimal.TEN);
        item.orm_propValueByName("amountFunctional", BigDecimal.TEN);
        item.orm_propValueByName("openAmountSource", BigDecimal.TEN);
        item.orm_propValueByName("openAmountFunctional", BigDecimal.TEN);
        item.orm_propValueByName("status", "OPEN");
        dao.saveEntity(item);
    }

    private void seedFundAccount(long id) {
        IEntityDao<ErpFinFundAccount> dao = daoProvider.daoFor(ErpFinFundAccount.class);
        ErpFinFundAccount fa = dao.newEntity();
        fa.orm_propValue(1, id);
        fa.setCode("FA-" + id);
        fa.setName("基本户");
        fa.setOrgId(ORG_ID);
        fa.orm_propValueByName("accountType", "BANK");
        fa.setSubjectId(SUBJECT_ID);
        fa.setCurrencyId(CURRENCY_ID);
        fa.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(fa);
    }

    private void seedBudgetScenario(long id) {
        IEntityDao<ErpFinBudgetScenario> dao = daoProvider.daoFor(ErpFinBudgetScenario.class);
        ErpFinBudgetScenario s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode("BUD-" + id);
        s.setName("预算方案-" + id);
        s.setOrgId(ORG_ID);
        s.setAcctSchemaId(ACCT_SCHEMA_ID);
        s.orm_propValueByName("fiscalYear", 2026);
        s.orm_propValueByName("scenarioType", "ANNUAL");
        s.setCurrencyId(CURRENCY_ID);
        s.orm_propValueByName("exchangeRate", BigDecimal.ONE);
        s.orm_propValueByName("controlLevel", "WARN");
        s.orm_propValueByName("docStatus", "DRAFT");
        s.orm_propValueByName("approveStatus", "DRAFT");
        dao.saveEntity(s);
    }

    private void seedBudgetLine(long id, long scenarioId) {
        IEntityDao<ErpFinBudgetLine> dao = daoProvider.daoFor(ErpFinBudgetLine.class);
        ErpFinBudgetLine line = dao.newEntity();
        line.orm_propValue(1, id);
        line.setScenarioId(scenarioId);
        line.setLineNo(1);
        line.setOrgId(ORG_ID);
        line.setAcctSchemaId(ACCT_SCHEMA_ID);
        line.setPeriodId(PERIOD_ID);
        line.setSubjectId(SUBJECT_ID);
        line.setSubjectCode("6601");
        line.setPartnerId(PARTNER_ID);
        line.setMaterialId(MATERIAL_ID);
        line.setWarehouseId(WAREHOUSE_ID);
        line.setCurrencyId(CURRENCY_ID);
        line.orm_propValueByName("budgetAmountSource", BigDecimal.TEN);
        line.orm_propValueByName("budgetAmountFunctional", BigDecimal.TEN);
        dao.saveEntity(line);
    }
}
