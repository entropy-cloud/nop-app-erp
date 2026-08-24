package io.nop.app.all.it;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B8 C15：CRM 线索转化与销售预测（按 {@code docs/design/integration-testing.md §6 C15} 规格）。
 *
 * <p>全链（引用部署 seed：组织 2 / CUST-001 客户 1 / CNY 币种 1 / 阶段 STAGE-001 验证 30%（id 1）/
 * STAGE-002 报价 60%（id 2）/ forecast id=1（commit 50000 / weighted 45000 / bestCase 80000）+
 * forecast_line 2 行（加权 15000+48000=63000））：自包含线索 {@code ErpCrmLead__save}（LEAD/NEW）→
 * {@code qualify}（入漏斗 NEW→QUALIFIED，默认 stage=STAGE-001 + probability 30）→
 * {@code moveStage}（STAGE-001→STAGE-002 前移，stageId 翻转断言）→
 * {@code convertToOpportunity}（原地升格 LEAD→OPPORTUNITY，docStatus 保持 QUALIFIED——实仓无独立
 * ErpCrmOpportunity 实体，设计文档旧动作名 {@code ErpCrmOpportunity__save} 勘误）→
 * 自包含赢单阶段（id 9001，sequence 30，isWonStage=true）→ {@code moveStage}（STAGE-002→赢单前移，
 * 满足 UC-CRM-03 won-stage 前置）→ {@code convertToQuotation}（OPPORTUNITY→报价单 SQ-*，
 * customerId=商机 partnerId，弱指针回写 + CONVERTED）→ {@code ErpSalOrder__save}（quotationId 回链，
 * 50 × 100 = 5000/650/5650）→ submit/approve（DIRECT，B2 C03 先例链）→
 * {@code ErpCrmReport__renderHtml(reportName="forecast-accuracy")}（commitAmount/weightedAmount/
 * bestCaseAmount/lineWeightedRevenue token 断言 = seed 派生 50,000.00/45,000.00/80,000.00/63,000.00）。
 *
 * <p>Phase 1 Decision（四项裁决，落盘设计文档 §6 C15 勘误）：
 * <ol>
 *   <li><b>机会转化动作</b>：锚定 {@code convertToOpportunity}（原地升格，UC-CRM-02「不创建客户」分支，
 *       {@code ErpCrmConversionConvertToOpportunityProcessor} 实仓行为）——设计文档步骤 2 旧动作名
 *       {@code ErpCrmOpportunity__save} 为漂移（实仓无该实体，B2 C03 已裁决商机 = ErpCrmLead
 *       leadType=OPPORTUNITY）。</li>
 *   <li><b>报价单生成动作</b>：{@code ErpCrmLead__convertToQuotation}（TestErpCrmLeadConversion 先例，
 *       轻量无 CPQ 配置依赖）而非 {@code ErpCrmProductConfigurator__generateQuote}（需配置器/定价规则
 *       全套自包含建数，语义同为生成 ErpSalQuotation）。</li>
 *   <li><b>forecast-accuracy 期望值口径</b>：数据集 = seed forecast 头三值（50000/45000/80000）+
 *       forecast_line 聚合（lineCount=2 / lineWeightedRevenue=15000+48000=63000）——设计文档期望
 *       50000/45000/80000/63000 中 63000 即行加权收入合计（{@code buildForecastAccuracyDataset} 聚合，
 *       {@code TestErpCrmReportRendering} 先例）；自包含线索不写 forecast 表，期望值不受本用例数据影响；
 *       报表单元格 numberFormat {@code #,##0.00} → token 形如 "50,000.00"。</li>
 *   <li><b>冻结时钟</b>：经 {@code C15C16C17FrozenClockExtension}（2026-07-17）冻结日期——报表本身
 *       无日期路径，时钟主要保证单据 createTime/approvedAt 等快照列确定性；响应快照跨 run 不稳定
 *       时间戳按 B4-B7 先例以 {@code *} 通配处置。</li>
 * </ol>
 *
 * <p>三层验证：层 1 = JUnit 关键断言（qualify/moveStage/转化状态翻转 + 报价单/订单回链 + 订单 APPROVED +
 * 报表 token）；层 2 = 每步 response 快照；层 3 = output/tables 变更行（lead + conv_log + stage +
 * quotation + order/order_line）。RECORDING→CHECKING 往返按 M0.2 试点范式完成。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC15CrmLeadForecast extends ErpIntegrationTestCase {

    static final String LEAD_CODE = "IT-C15-LEAD-001";
    static final String ORDER_CODE = "IT-C15-SO-001";
    static final String WON_STAGE_ID = "9001";
    static final BigDecimal TOTAL = new BigDecimal("5000");      // 不含税
    static final BigDecimal TAX = new BigDecimal("650");         // 13%
    static final BigDecimal TOTAL_WITH_TAX = new BigDecimal("5650");

    @RegisterExtension
    static C15C16C17FrozenClockExtension frozenClock = new C15C16C17FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testCrmLeadForecastClosedLoop() {
        // ---------- 1. 自包含线索（LEAD/NEW，客户 1）----------
        ApiResponse<?> leadSave = rpcMutation("ErpCrmLead__save", request("1_lead_save.json5", Map.class));
        output("1_lead_save_response.json5", leadSave);
        assertEquals(0, leadSave.getStatus(), "线索保存应成功");
        String leadId = idOf(leadSave);
        addVar("leadId", leadId);
        assertEquals(ErpCrmConstants.LEAD_TYPE_LEAD, reloadLead(leadId).getLeadType(), "初始 leadType=LEAD");

        // ---------- 2. qualify：入漏斗 NEW→QUALIFIED（默认 stage=STAGE-001 + probability 30） ----------
        ApiResponse<?> qualify = rpcMutation("ErpCrmLead__qualify", request("2_lead_qualify.json5", Map.class));
        output("2_lead_qualify_response.json5", qualify);
        assertEquals(0, qualify.getStatus(), "qualify 应成功");
        ErpCrmLead qualified = reloadLead(leadId);
        assertEquals(ErpCrmConstants.DOC_STATUS_QUALIFIED, qualified.getDocStatus(), "qualify 后 QUALIFIED");
        assertEquals("1", qualified.getStageId(), "qualify 设默认 stage=STAGE-001");
        assertEquals(30, qualified.getProbability(), "probability 取阶段默认 30");

        // ---------- 3. moveStage：STAGE-001→STAGE-002 前移（stageId 翻转锚点） ----------
        ApiResponse<?> moveStage = rpcMutation("ErpCrmLead__moveStage", request("3_lead_move_stage.json5", Map.class));
        output("3_lead_move_stage_response.json5", moveStage);
        assertEquals(0, moveStage.getStatus(), "moveStage 应成功");
        assertEquals("2", reloadLead(leadId).getStageId(), "moveStage 后 stageId 翻转 STAGE-001→STAGE-002");

        // ---------- 4. convertToOpportunity：原地升格 LEAD→OPPORTUNITY（docStatus 保持 QUALIFIED） ----------
        ApiResponse<?> convertOpp = rpcMutation("ErpCrmLead__convertToOpportunity",
                request("4_lead_convert_to_opportunity.json5", Map.class));
        output("4_lead_convert_to_opportunity_response.json5", convertOpp);
        assertEquals(0, convertOpp.getStatus(), "convertToOpportunity 应成功");
        ErpCrmLead promoted = reloadLead(leadId);
        assertEquals(ErpCrmConstants.LEAD_TYPE_OPPORTUNITY, promoted.getLeadType(),
                "原地升格后 leadType=OPPORTUNITY（实仓无独立 ErpCrmOpportunity 实体）");
        assertEquals(ErpCrmConstants.DOC_STATUS_QUALIFIED, promoted.getDocStatus(), "升格保持 QUALIFIED");

        // ---------- 5. 自包含赢单阶段 + moveStage 前移（UC-CRM-03 won-stage 前置） ----------
        seedWonStage();
        ApiResponse<?> moveWon = rpcMutation("ErpCrmLead__moveStage", request("5_lead_move_won_stage.json5", Map.class));
        output("5_lead_move_won_stage_response.json5", moveWon);
        assertEquals(0, moveWon.getStatus(), "moveStage 至赢单阶段应成功");
        assertEquals(WON_STAGE_ID, reloadLead(leadId).getStageId(), "stageId 前移至赢单阶段");

        // ---------- 6. convertToQuotation：OPPORTUNITY→报价单（弱指针回写 + CONVERTED） ----------
        ApiResponse<?> convertQuote = rpcMutation("ErpCrmLead__convertToQuotation",
                request("6_lead_convert_to_quotation.json5", Map.class));
        output("6_lead_convert_to_quotation_response.json5", convertQuote);
        assertEquals(0, convertQuote.getStatus(), "convertToQuotation 应成功");
        String quotationId = idOf(convertQuote);
        addVar("quotationId", quotationId);
        String quotationCode = String.valueOf(((Map<?, ?>) convertQuote.getData()).get("code"));
        addVar("quotationCode", quotationCode);

        ErpSalQuotation quotation = reload(ErpSalQuotation.class, quotationId);
        assertNotNull(quotation, "报价单已创建");
        assertEquals("1", quotation.getCustomerId(), "报价单 customerId=商机 partnerId（客户 1）");
        ErpCrmLead converted = reloadLead(leadId);
        assertEquals(ErpCrmConstants.DOC_STATUS_CONVERTED, converted.getDocStatus(), "商机转化后 CONVERTED");
        assertEquals(ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION, converted.getRelatedBillType(),
                "弱指针 relatedBillType=SALES_QUOTATION");
        assertEquals(quotationCode, converted.getRelatedBillCode(), "弱指针 relatedBillCode=报价单号");

        // ---------- 7. 报价→订单转化：ErpSalOrder__save（quotationId 回链）+ 行 ----------
        ApiResponse<?> orderSave = rpcMutation("ErpSalOrder__save", request("7_order_save.json5", Map.class));
        output("7_order_save_response.json5", orderSave);
        assertEquals(0, orderSave.getStatus(), "销售订单保存应成功");
        String orderId = idOf(orderSave);
        addVar("orderId", orderId);
        assertEquals(quotationId, reload(ErpSalOrder.class, orderId).getQuotationId(),
                "订单 quotationId 回链报价单（报价→订单转化）");

        ApiResponse<?> orderLine = rpcMutation("ErpSalOrderLine__save", request("8_order_line_save.json5", Map.class));
        output("8_order_line_save_response.json5", orderLine);
        assertEquals(0, orderLine.getStatus(), "订单行保存应成功");
        addVar("orderLineId", idOf(orderLine));

        // ---------- 8. DIRECT 审批（B2 C03 先例链）：submit → approve ----------
        ApiResponse<?> orderSubmit = rpcMutation("ErpSalOrder__submitForApproval",
                request("9_order_submit.json5", Map.class));
        output("9_order_submit_response.json5", orderSubmit);
        assertEquals(0, orderSubmit.getStatus(), "销售订单提交审批应成功");

        ApiResponse<?> orderApprove = rpcMutation("ErpSalOrder__approve", request("10_order_approve.json5", Map.class));
        output("10_order_approve_response.json5", orderApprove);
        assertEquals(0, orderApprove.getStatus(), "销售订单审批应成功");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(ErpSalOrder.class, orderId).getApproveStatus(),
                "订单 approveStatus=APPROVED（DIRECT）");

        // ---------- 9. forecast-accuracy 报表（seed 派生口径 token 断言） ----------
        ApiResponse<?> report = executeRpc(GraphQLOperationType.query, "ErpCrmReport__renderHtml",
                request("11_report_forecast_accuracy.json5", Map.class));
        output("11_report_forecast_accuracy_response.json5", report);
        assertEquals(0, report.getStatus(), "forecast-accuracy 渲染应成功");
        String html = String.valueOf(report.getData());
        assertTrue(html.contains("50,000.00"), "报表含 commitAmount=50,000.00（seed 派生）");
        assertTrue(html.contains("45,000.00"), "报表含 weightedAmount=45,000.00（seed 派生）");
        assertTrue(html.contains("80,000.00"), "报表含 bestCaseAmount=80,000.00（seed 派生）");
        assertTrue(html.contains("63,000.00"), "报表含 lineWeightedRevenue=63,000.00（15000+48000 行加权合计）");
    }

    // ---------- helpers ----------

    private ErpCrmLead reloadLead(String leadId) {
        return daoProvider.daoFor(ErpCrmLead.class).getEntityById(leadId);
    }

    /** 自包含赢单阶段（seed 仅验证/报价两阶段，无 isWonStage=true 行——转化前置须自包含建数）。 */
    private void seedWonStage() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCrmStage> dao = daoProvider.daoFor(ErpCrmStage.class);
            ErpCrmStage stage = new ErpCrmStage();
            stage.orm_propValueByName("id", WON_STAGE_ID);
            stage.setCode("STG-C15-WON");
            stage.setOrgId("2");
            stage.setStageName("赢单");
            stage.orm_propValueByName("sequence", 30);
            stage.orm_propValueByName("defaultProbability", 90);
            stage.setIsWonStage(Boolean.TRUE);
            dao.saveEntity(stage);
        });
    }
}
