package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtConsumptionLine;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import app.erp.pur.dao.entity.ErpPurInvoice;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合同计费族测试（RC-R1.33，P1-RC-074 + P1-RC-075，UC-CT-03/04）。
 *
 * <p>覆盖（对齐 plan 2026-08-15-0456-3 Phase 4 测试矩阵，沿用 R1.32 直断言范式——`_cases/` 无快照）：
 * <ol>
 *   <li>批量生成：2 行 × 2 term 生成 4 plan + 非 ACTIVE 拒绝 + 幂等拒绝 + 行归属拒绝；</li>
 *   <li>isInvoiced 锁：已开票改金额拒绝（错误码断言）+ 未开票可改 + 改 remark 放行；</li>
 *   <li>periodSummarize 汇总正确（Σ 计算 + line.quantity 对比）；</li>
 *   <li>超量生成（Σ&gt;预估 → 额外 plan + 发票草稿落库）；</li>
 *   <li>120% 通知（seed 模板 → notify 落库 + recipient 断言；未超量零通知；无模板静默跳过）。</li>
 * </ol>
 *
 * <p>沿用 {@link TestErpCtContractPosting} 样板（JunitAutoTestCase + @NopTestConfig + 直接 DAO 断言）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtBillingFamily extends JunitAutoTestCase {

    static final String CONSUME_APPROVER = "ct-consume-approver";

    @RegisterExtension
    static CtFrozenClockExtension frozenClock = new CtFrozenClockExtension();

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ---------- ① 批量生成（generateInvoicePlansByTerm） ----------

    @Test
    public void testGenerateByTermMultiLineMultiTerm() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long contractId = setup[0];
        long line1 = setup[1];
        long line2 = addLine(contractId, new BigDecimal("50"), new BigDecimal("20"));

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item(line1, "ADVANCE", "2026-03-01", "300"));
        items.add(item(line1, "COMPLETION", "2026-12-31", "700"));
        items.add(item(line2, "ADVANCE", "2026-03-01", "400"));
        items.add(item(line2, "MONTHLY", "2026-04-01", "600"));

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtInvoicePlan__generateInvoicePlansByTerm",
                ApiRequest.build(Map.of("contractId", contractId, "items", items)));
        assertEquals(0, resp.getStatus(), "generateInvoicePlansByTerm 应成功: " + resp);

        List<ErpCtInvoicePlan> plans = findPlansByLine(line1);
        plans.addAll(findPlansByLine(line2));
        assertEquals(4, plans.size(), "应生成 4 个 InvoicePlan");
        for (ErpCtInvoicePlan plan : plans) {
            assertFalse(Boolean.TRUE.equals(plan.getIsInvoiced()), "新生成计划 isInvoiced 应为 false");
            assertNotNull(plan.getInvoiceTerm(), "invoiceTerm 必填");
            assertNotNull(plan.getPlanDate(), "planDate 必填");
        }
    }

    @Test
    public void testGenerateByTermRejectedForNonActive() {
        long[] setup = setupContractWithLine("NEGOTIATION");
        long contractId = setup[0];
        long lineId = setup[1];

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtInvoicePlan__generateInvoicePlansByTerm",
                ApiRequest.build(Map.of("contractId", contractId,
                        "items", List.of(item(lineId, "ADVANCE", "2026-03-01", "300")))));
        assertNotEquals(0, resp.getStatus(), "非 ACTIVE 合同应拒绝生成");
        assertTrue(resp.getMsg().contains("非执行中"), "应报 ERR_CT_CONTRACT_NOT_ACTIVE: " + resp.getMsg());
        assertEquals(0, findPlansByLine(lineId).size(), "拒绝时零落库");
    }

    @Test
    public void testGenerateByTermDuplicateRejected() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long contractId = setup[0];
        long lineId = setup[1];

        ApiResponse<?> ok = executeRpc(mutation, "ErpCtInvoicePlan__generateInvoicePlansByTerm",
                ApiRequest.build(Map.of("contractId", contractId,
                        "items", List.of(item(lineId, "MILESTONE", "2026-06-01", "500")))));
        assertEquals(0, ok.getStatus(), "首次生成应成功: " + ok);

        ApiResponse<?> dup = executeRpc(mutation, "ErpCtInvoicePlan__generateInvoicePlansByTerm",
                ApiRequest.build(Map.of("contractId", contractId,
                        "items", List.of(item(lineId, "MILESTONE", "2026-06-01", "500")))));
        assertNotEquals(0, dup.getStatus(), "幂等查重应拒绝重复生成");
        assertTrue(dup.getMsg().contains("重复生成"), "应报 ERR_CT_INVOICE_PLAN_DUPLICATE: " + dup.getMsg());
        assertEquals(1, findPlansByLine(lineId).size(), "重复生成不落库");
    }

    @Test
    public void testGenerateByTermLineNotInContractRejected() {
        long[] setup1 = setupActiveContract("PURCHASE", "INBOUND");
        long contractId1 = setup1[0];
        long[] setup2 = setupActiveContract("PURCHASE", "INBOUND");
        long lineOfOther = setup2[1];

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtInvoicePlan__generateInvoicePlansByTerm",
                ApiRequest.build(Map.of("contractId", contractId1,
                        "items", List.of(item(lineOfOther, "ADVANCE", "2026-03-01", "300")))));
        assertNotEquals(0, resp.getStatus(), "跨合同行应拒绝");
        assertTrue(resp.getMsg().contains("不属于合同"), "应报 ERR_CT_INVOICE_PLAN_LINE_NOT_IN_CONTRACT: " + resp.getMsg());
        assertEquals(0, findPlansByLine(lineOfOther).size(), "拒绝时零落库");
    }

    // ---------- ② isInvoiced 锁（defaultPrepareUpdate 守卫） ----------

    @Test
    public void testInvoicedPlanAmountUpdateRejected() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long lineId = setup[1];

        long planId = saveInvoicePlan(lineId, new BigDecimal("1000"));
        ApiResponse<?> trigger = executeRpc(mutation, "ErpCtInvoicePlan__triggerInvoice",
                ApiRequest.build(Map.of("planId", planId)));
        assertEquals(0, trigger.getStatus(), "triggerInvoice 应成功: " + trigger);

        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", planId);
        upd.put("amount", new BigDecimal("2000"));
        ApiResponse<?> bad = executeRpc(mutation, "ErpCtInvoicePlan__update", ApiRequest.build(Map.of("data", upd)));
        assertNotEquals(0, bad.getStatus(), "已开票计划改金额应拒绝");
        assertTrue(bad.getMsg().contains("不可修改"), "应报 ERR_CT_INVOICE_PLAN_INVOICED_IMMUTABLE: " + bad.getMsg());

        ErpCtInvoicePlan plan = reloadPlan(planId);
        assertEquals(0, new BigDecimal("1000").compareTo(plan.getAmount()), "金额不应被修改");
    }

    @Test
    public void testNotInvoicedPlanAmountUpdatable() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long lineId = setup[1];

        long planId = saveInvoicePlan(lineId, new BigDecimal("1000"));
        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", planId);
        upd.put("amount", new BigDecimal("2000"));
        ApiResponse<?> ok = executeRpc(mutation, "ErpCtInvoicePlan__update", ApiRequest.build(Map.of("data", upd)));
        assertEquals(0, ok.getStatus(), "未开票计划改金额应成功: " + ok);

        ErpCtInvoicePlan plan = reloadPlan(planId);
        assertEquals(0, new BigDecimal("2000").compareTo(plan.getAmount()), "金额应已更新");
    }

    @Test
    public void testInvoicedPlanRemarkUpdateAllowed() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long lineId = setup[1];

        long planId = saveInvoicePlan(lineId, new BigDecimal("1000"));
        ApiResponse<?> trigger = executeRpc(mutation, "ErpCtInvoicePlan__triggerInvoice",
                ApiRequest.build(Map.of("planId", planId)));
        assertEquals(0, trigger.getStatus());

        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", planId);
        upd.put("remark", "已开票备注补充");
        ApiResponse<?> ok = executeRpc(mutation, "ErpCtInvoicePlan__update", ApiRequest.build(Map.of("data", upd)));
        assertEquals(0, ok.getStatus(), "已开票计划改 remark（非守卫字段）应放行: " + ok);

        assertEquals("已开票备注补充", reloadPlan(planId).getRemark());
    }

    // ---------- ③/④/⑤ periodSummarize（消耗汇总 + 超量 + 120% 通知） ----------

    @Test
    public void testSummarizeNoOverageNoAction() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long lineId = setup[1];
        // line.quantity=100（预估），Σ=40 < 预估
        seedConsumption(lineId, "2026-06-01", "20", "10");
        seedConsumption(lineId, "2026-06-15", "20", "10");

        ApiResponse<?> resp = summarize(lineId, "2026-06-01", "2026-06-30");
        assertEquals(0, resp.getStatus(), "periodSummarize 应成功: " + resp);
        Map<?, ?> r = (Map<?, ?>) resp.getData();
        assertEquals(0, new BigDecimal("40").compareTo(toDecimal(r.get("totalConsumedQuantity"))),
                "Σ 应为 40");
        assertEquals(0, new BigDecimal("100").compareTo(toDecimal(r.get("estimatedQuantity"))),
                "预估应为 100");
        assertEquals(0, new BigDecimal("0").compareTo(toDecimal(r.get("overQuantity"))), "未超量 overQuantity=0");
        assertNull(r.get("overagePlanId"), "未超量不生成计划");
        assertFalse(Boolean.TRUE.equals(r.get("notificationSent")), "未超量零通知");
        assertEquals(0, findPlansByLine(lineId).size(), "未超量零 InvoicePlan");
    }

    @Test
    public void testSummarizeOverageGeneratesPlanAndDraft() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long lineId = setup[1];
        // line.quantity=100 预估，Σ=150 > 预估（超量生成计划+发票草稿；无模板时 120% 通知静默跳过）
        seedConsumption(lineId, "2026-06-01", "80", "10");
        seedConsumption(lineId, "2026-06-15", "70", "10");

        ApiResponse<?> resp = summarize(lineId, "2026-06-01", "2026-06-30");
        assertEquals(0, resp.getStatus(), "periodSummarize 应成功: " + resp);
        Map<?, ?> r = (Map<?, ?>) resp.getData();
        assertEquals(0, new BigDecimal("150").compareTo(toDecimal(r.get("totalConsumedQuantity"))));
        assertEquals(0, new BigDecimal("50").compareTo(toDecimal(r.get("overQuantity"))), "超量 50");
        assertEquals(0, new BigDecimal("500").compareTo(toDecimal(r.get("overAmount"))),
                "超量金额 = 50 × unitPrice(10)");
        assertNotNull(r.get("overagePlanId"), "超量应生成 InvoicePlan");

        ErpCtInvoicePlan plan = reloadPlan(toLong(r.get("overagePlanId")));
        assertTrue(Boolean.TRUE.equals(plan.getIsInvoiced()), "超量计划应已触发发票");
        assertNotNull(plan.getInvoiceBillCode(), "应回写 invoiceBillCode");
        assertEquals(0, new BigDecimal("500").compareTo(plan.getAmount()), "超量计划金额 = 超量金额");

        ErpPurInvoice invoice = daoProvider.daoFor(ErpPurInvoice.class)
                .findFirstByQuery(eqQuery("code", plan.getInvoiceBillCode()));
        assertNotNull(invoice, "应生成 AP 发票草稿");
        assertEquals("DRAFT", invoice.getDocStatus());
        assertFalse(invoice.getPosted());
        assertEquals(0, new BigDecimal("500").compareTo(invoice.getTotalAmount()), "发票 header 金额 = 超量金额");
    }

    @Test
    public void testSummarizeOver120Notifies() {
        seedConsumptionTemplate();
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long lineId = setup[1];
        // line.quantity=100 预估，Σ=180 > 预估 × 1.2（120）
        seedConsumption(lineId, "2026-06-01", "100", "10");
        seedConsumption(lineId, "2026-06-15", "80", "10");

        ApiResponse<?> resp = summarize(lineId, "2026-06-01", "2026-06-30");
        assertEquals(0, resp.getStatus(), "periodSummarize 应成功: " + resp);
        Map<?, ?> r = (Map<?, ?>) resp.getData();
        assertEquals(0, new BigDecimal("80").compareTo(toDecimal(r.get("overQuantity"))), "超量 80");
        assertTrue(Boolean.TRUE.equals(r.get("notificationSent")), "Σ>120% 应派发通知");

        List<ErpSysNotification> list = notificationsOf(CONSUME_APPROVER,
                ErpCtConstants.NOTIFY_EVENT_CONSUMPTION_OVER_120);
        assertEquals(1, list.size(), "审批通知应落库 1 条");
        ErpSysNotification n = list.get(0);
        assertTrue(n.getBody().contains("180"), "通知 body 应含总量: " + n.getBody());
    }

    @Test
    public void testSummarizeOver120NoTemplateSilentSkip() {
        // 不 seed 模板 → notify best-effort 静默跳过（R1.4 范式，不阻断业务事实）
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long lineId = setup[1];
        seedConsumption(lineId, "2026-06-01", "100", "10");
        seedConsumption(lineId, "2026-06-15", "80", "10");

        ApiResponse<?> resp = summarize(lineId, "2026-06-01", "2026-06-30");
        assertEquals(0, resp.getStatus(), "无模板静默跳过不应失败: " + resp);
        Map<?, ?> r = (Map<?, ?>) resp.getData();
        assertFalse(Boolean.TRUE.equals(r.get("notificationSent")), "无模板 → notificationSent=false");
        assertNotNull(r.get("overagePlanId"), "超量计划生成不受通知影响");
    }

    @Test
    public void testSummarizeNonExistentLineRejected() {
        ApiResponse<?> resp = summarize(999999L, "2026-06-01", "2026-06-30");
        assertNotEquals(0, resp.getStatus(), "行不存在应拒绝");
        assertTrue(resp.getMsg().contains("不存在"), "应报 ERR_CT_CONSUMPTION_LINE_NOT_FOUND: " + resp.getMsg());
    }

    // ---------- helpers ----------

    private Map<String, Object> item(long contractLineId, String invoiceTerm, String planDate, String amount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractLineId", contractLineId);
        m.put("invoiceTerm", invoiceTerm);
        m.put("planDate", planDate);
        m.put("amount", new BigDecimal(amount));
        return m;
    }

    private ApiResponse<?> summarize(long contractLineId, String fromDate, String toDate) {
        return executeRpc(mutation, "ErpCtConsumptionLine__periodSummarize",
                ApiRequest.build(Map.of("contractLineId", contractLineId,
                        "fromDate", fromDate, "toDate", toDate,
                        "invoiceTerm", "MONTHLY", "planDate", toDate)));
    }

    private void seedConsumption(long contractLineId, String date, String quantity, String unitPrice) {
        ormTemplate.runInSession(() -> {
            ErpCtConsumptionLine c = daoProvider.daoFor(ErpCtConsumptionLine.class).newEntity();
            c.setContractLineId(contractLineId);
            c.setConsumptionDate(LocalDate.parse(date));
            c.setQuantity(new BigDecimal(quantity));
            c.setUnitPrice(new BigDecimal(unitPrice));
            c.setAmount(new BigDecimal(quantity).multiply(new BigDecimal(unitPrice)));
            daoProvider.daoFor(ErpCtConsumptionLine.class).saveEntity(c);
        });
    }

    private void seedConsumptionTemplate() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", 8331L);
            t.setNotificationType(ErpCtConstants.NOTIFY_EVENT_CONSUMPTION_OVER_120);
            t.setName("TPL-CONSUMPTION-OVER-120");
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("消耗超量预警 ${contractCode}");
            t.setBodyTpl("合同 ${contractCode} 行 ${contractLineId} 期间消耗 ${totalConsumedQuantity}，预估 ${estimatedQuantity}，超量 ${overQuantity}");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig("{\"userIds\":[\"" + CONSUME_APPROVER + "\"]}");
            t.setMergeWindowSeconds(300);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            dao.saveEntity(t);
        });
    }

    private List<ErpSysNotification> notificationsOf(String userId, String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private long[] setupActiveContract(String contractType, String direction) {
        long[] setup = setupContractWithLine("NEGOTIATION");
        long contractId = setup[0];
        createVersion(contractId, 1, true, "FINALIZED");
        ApiResponse<?> act = executeRpc(mutation, "ErpCtContract__activate",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertEquals(0, act.getStatus(), "setup activate 应成功: " + act);
        return setup;
    }

    private long[] setupContractWithLine(String status) {
        long[] ids = new long[3];
        ormTemplate.runInSession(session -> {
            ids[0] = createPartner();
            ids[1] = createCurrency();
            ids[2] = createMaterial(createUoM());
            return null;
        });
        long contractId = createContract(ids[0], ids[1], status);
        long lineId = saveLine(contractId, ids[2], new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("1000"));
        return new long[]{contractId, lineId};
    }

    private long createPartner() {
        ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
        p.setCode("CT-BILL-PARTNER-" + System.nanoTime());
        p.setName("计费测试伙伴");
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
        return p.getId();
    }

    private long createCurrency() {
        ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
        c.setCode("CNY-BILL");
        c.setName("人民币");
        daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
        return c.getId();
    }

    private long createUoM() {
        ErpMdUoM u = daoProvider.daoFor(ErpMdUoM.class).newEntity();
        u.setCode("PCS-CT-BILL");
        u.setName("个");
        daoProvider.daoFor(ErpMdUoM.class).saveEntity(u);
        return u.getId();
    }

    private long createMaterial(long uomId) {
        ErpMdMaterial m = daoProvider.daoFor(ErpMdMaterial.class).newEntity();
        m.setCode("MAT-CT-BILL-" + System.nanoTime());
        m.setName("计费测试物料");
        m.setMaterialType("GOODS");
        m.setUoMId(uomId);
        m.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdMaterial.class).saveEntity(m);
        return m.getId();
    }

    private long createContract(long partnerId, long currencyId, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "CT-BILL-" + System.nanoTime());
        data.put("contractName", "计费测试合同");
        data.put("contractType", "PURCHASE");
        data.put("contractDirection", "INBOUND");
        data.put("partnerId", partnerId);
        data.put("currencyId", currencyId);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("totalAmount", new BigDecimal("1000"));
        data.put("status", status);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContract__save 应成功: " + resp);
        return toLong(((Map<?, ?>) resp.getData()).get("id"));
    }

    private long saveLine(long contractId, long materialId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lineNo", 1);
        data.put("contractId", contractId);
        data.put("materialId", materialId);
        data.put("quantity", quantity);
        data.put("unitPrice", unitPrice);
        data.put("amount", amount);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContractLine__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContractLine__save 应成功: " + resp);
        return toLong(((Map<?, ?>) resp.getData()).get("id"));
    }

    private long addLine(long contractId, BigDecimal quantity, BigDecimal unitPrice) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lineNo", 2);
        data.put("contractId", contractId);
        data.put("quantity", quantity);
        data.put("unitPrice", unitPrice);
        data.put("amount", quantity.multiply(unitPrice));
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContractLine__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "addLine 应成功: " + resp);
        return toLong(((Map<?, ?>) resp.getData()).get("id"));
    }

    private void createVersion(long contractId, int versionNo, boolean isCurrent, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractId", contractId);
        data.put("versionNo", versionNo);
        data.put("versionDate", "2026-01-01");
        data.put("isCurrent", isCurrent);
        data.put("status", status);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContractVersion__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContractVersion__save 应成功: " + resp);
    }

    private long saveInvoicePlan(long contractLineId, BigDecimal amount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractLineId", contractLineId);
        data.put("planDate", "2026-06-01");
        data.put("amount", amount);
        data.put("invoiceTerm", "MILESTONE");
        data.put("isInvoiced", false);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtInvoicePlan__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtInvoicePlan__save 应成功: " + resp);
        return toLong(((Map<?, ?>) resp.getData()).get("id"));
    }

    private ErpCtInvoicePlan reloadPlan(long planId) {
        return daoProvider.daoFor(ErpCtInvoicePlan.class).getEntityById(planId);
    }

    private List<ErpCtInvoicePlan> findPlansByLine(long contractLineId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractLineId", contractLineId));
        return daoProvider.daoFor(ErpCtInvoicePlan.class).findAllByQuery(q);
    }

    private BigDecimal toDecimal(Object o) {
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        }
        return new BigDecimal(String.valueOf(o));
    }

    private long toLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return Long.parseLong(String.valueOf(o));
    }

    private QueryBean eqQuery(String field, Object value) {
        QueryBean q = new QueryBean();
        q.addFilter(eq(field, value));
        return q;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
