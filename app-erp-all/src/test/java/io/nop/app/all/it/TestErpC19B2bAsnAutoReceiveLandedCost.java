package io.nop.app.all.it;

import app.erp.b2b.dao.entity.ErpB2bAsn;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.dao.entity.ErpInvLandedCostLine;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B9 C19：B2B ASN 自动收货与物流到岸成本（按 {@code docs/design/integration-testing.md §6 C19} 规格）。
 *
 * <p>全链（引用部署 seed：组织 2 / SUP-001 供应商 3 / MAT-001 物料 1 / WH-RAW 仓库 2 / CNY 币种 1 /
 * 2026-07 OPEN 期间）：自包含已审批采购链（{@code ErpPurOrder__save}+行（100×5=500）→
 * submitForApproval → approve [DIRECT]，C05 先例）→ 自包含 ASN（{@code ErpB2bAsn__save} status=RECEIVED
 * + relatedBillType=PO_ORDER + AsnLine materialId 显式置——webhook 解析路径已知 gap 以 save 直建规避，
 * {@code TestErpB2bAsnInventoryIntegration.seedMatchedAsnDirectly} 先例）→ {@code matchPurchaseOrder}
 * （RECEIVED→MATCHED，PO code 匹配 + 行物料/超量校验）→ {@code createReceiveFromAsn}
 * （@NopTestProperty 开 {@code erp-b2b.asn-auto-create-receive}；MATCHED→RECEIVED_TO_STOCK + 采购收货
 * 草稿 {@code RCV-FROM-ASN-{asnCode}} + 行级回填（materialId 透传 / uoMId 反查 / unitPrice·orderLineId
 * 反查 PO 行 / amount=5×100））→ 收货草稿 orgId 补全（ASN 创建链不落 orgId → 过账账套不可解析，
 * fixture 补全先例）→ {@code ErpPurReceive__submitForApproval} → {@code approve} [DIRECT]（APPROVED +
 * posted=true + INCOMING 移动 DONE，B1/B3 先例）→ 自包含物流（承运商 + 发运单 DISPATCHED /
 * relatedBillType=PURCHASE_RECEIPT / freightAmount=120 / freightSettlementStatus=PENDING）→
 * {@code ErpLogShipment__handleTrackingWebhook}（@NopTestProperty 关 {@code erp-log.webhook-signature-required}，
 * payload eventType=DELIVERED → DELIVERED + freightSettlementStatus=SETTLED）→ path-2 到岸成本自动创建
 * （@NopTestProperty 开 {@code erp-log.path2-landed-cost-auto-create} → DRAFT 单 LC-FRT-* + FREIGHT 行）→
 * {@code ErpInvLandedCost__findPage}（receiveId 过滤单断言，C07 findPage 模式）。
 *
 * <p>Phase 2 Decision（四项裁决，落盘设计文档 §6 C19 勘误）：
 * <ol>
 *   <li><b>ASN 状态机推进序列</b>：自包含 save 直建 RECEIVED（非 webhook 路径——设计「状态机推进
 *       （RECEIVED 等）」实仓落地 = save 置 RECEIVED 后 matchPurchaseOrder RECEIVED→MATCHED→
 *       createReceiveFromAsn→RECEIVED_TO_STOCK 两跳推进）；createReceiveFromAsn 前置状态 = MATCHED
 *       （非 MATCHED 抛 ERR_B2B_ASN_ILLEGAL_TRANSITION）。</li>
 *   <li><b>收货草稿→过账路径</b>：approve 内建过账（triggerIncomingMove → stock move DONE →
 *       PURCHASE_INPUT 过账 → posted=true 回写），无显式过账动作（B1/B3 先例口径复认）；ASN 创建链
 *       不落收货头 orgId/exchangeRate（exchangeRate 列 defaultValue=1 兜底；orgId 须 fixture 补全——
 *       null 时账套解析为 null、过账零凭证 posted 悬挂，{@code fixAsnLineMaterialId} 同型补全先例）。</li>
 *   <li><b>path-2 到岸成本口径</b>：触发条件 = relatedBillType=PURCHASE_RECEIPT + DELIVERED 事件 +
 *       config 开 + freightAmount&gt;0；单据内容 = DRAFT/UNSUBMITTED、totalCostAmount=freightAmount、
 *       BY_AMOUNT、code=LC-FRT-{receiveCode}-{millis}、receiveId 引用收货单；FREIGHT 行
 *       apPartnerId=receive.supplierId（无发运单强引用列——发运关联经 relatedBillCode 语义链）。</li>
 *   <li><b>webhook 签名键</b>：{@code erp-log.webhook-signature-required}（ErpLogConfigs，缺省 true，
 *       {@code ErpLogShipmentHandleTrackingWebhookProcessor} 消费）关；{@code erp-b2b.webhook-signature-required}
 *       为 ASN 入站 webhook 独立键（{@code ErpB2bAsnHandleInboundWebhookProcessor} 消费）——C19 流程
 *       不经 ASN webhook 无需关（设计文档键名正确无漂移复认）。</li>
 * </ol>
 *
 * <p>冻结时钟 {@code B9FrozenClockExtension}（2026-07-17）：ASN→收货链 businessDate =
 * CoreMetrics.today() 落库、到岸成本 code 尾缀 currentTimeMillis、发运送达 actualDeliveryDate 同源——
 * 跨日漂移按 C15C16C17 先例冻结取确定值。
 *
 * <p>三层验证：层 1 = JUnit 关键断言（ASN 状态机两跳终态 RECEIVED_TO_STOCK + 收货草稿行级回填 +
 * 审批 posted + 发运 DELIVERED/SETTLED + 到岸成本 DRAFT 单 FREIGHT 行/引用/金额）；层 2 = 每步
 * response 快照；层 3 = output/tables 变更行（asn 族 + pur_order/receive 族 + inv 移动/成本/到岸成本族 +
 * log 承运商/发运/webhook 日志族）。RECORDING→CHECKING 往返按 M0.2 试点范式完成。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-b2b.asn-auto-create-receive", value = "true")
@NopTestProperty(name = "erp-log.webhook-signature-required", value = "false")
@NopTestProperty(name = "erp-log.path2-landed-cost-auto-create", value = "true")
public class TestErpC19B2bAsnAutoReceiveLandedCost extends ErpIntegrationTestCase {

    static final String ASN_CODE = "IT-C19-ASN-001";
    static final String RECEIVE_CODE = "RCV-FROM-ASN-" + ASN_CODE;
    static final String FREIGHT = new BigDecimal("120").toPlainString();

    static final BigDecimal PO_AMOUNT = new BigDecimal("500"); // 100 × 5
    static final BigDecimal FREIGHT_AMOUNT = new BigDecimal("120");

    @RegisterExtension
    static B9FrozenClockExtension frozenClock = new B9FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testB2bAsnAutoReceiveLandedCostClosedLoop() {
        // ---------- 1. 自包含已审批采购链（C05 先例：PO → submit → approve） ----------
        ApiResponse<?> poSave = rpcMutation("ErpPurOrder__save", request("1_order_save.json5", Map.class));
        output("1_order_save_response.json5", poSave);
        assertEquals(0, poSave.getStatus(), "PO 保存应成功");
        String poId = idOf(poSave);
        addVar("poId", poId);

        ApiResponse<?> poLine = rpcMutation("ErpPurOrderLine__save", request("2_order_line_save.json5", Map.class));
        output("2_order_line_save_response.json5", poLine);
        assertEquals(0, poLine.getStatus(), "PO 行保存应成功");

        ApiResponse<?> poSubmit = rpcMutation("ErpPurOrder__submitForApproval", request("3_order_submit.json5", Map.class));
        output("3_order_submit_response.json5", poSubmit);
        assertEquals(0, poSubmit.getStatus(), "PO 提交审批应成功");

        ApiResponse<?> poApprove = rpcMutation("ErpPurOrder__approve", request("4_order_approve.json5", Map.class));
        output("4_order_approve_response.json5", poApprove);
        assertEquals(0, poApprove.getStatus(), "PO 审批应成功");

        // ---------- 2. 自包含 ASN（RECEIVED + PO_ORDER 弱指针）+ 行（materialId 显式置） ----------
        ApiResponse<?> asnSave = rpcMutation("ErpB2bAsn__save", request("5_asn_save.json5", Map.class));
        output("5_asn_save_response.json5", asnSave);
        assertEquals(0, asnSave.getStatus(), "ASN 保存应成功");
        String asnId = idOf(asnSave);
        addVar("asnId", asnId);
        assertEquals("RECEIVED", reload(ErpB2bAsn.class, asnId).getStatus(), "ASN 初始 RECEIVED");

        ApiResponse<?> asnLine = rpcMutation("ErpB2bAsnLine__save", request("6_asn_line_save.json5", Map.class));
        output("6_asn_line_save_response.json5", asnLine);
        assertEquals(0, asnLine.getStatus(), "ASN 行保存应成功");

        // ---------- 3. matchPurchaseOrder（RECEIVED→MATCHED） ----------
        ApiResponse<?> match = rpcMutation("ErpB2bAsn__matchPurchaseOrder", request("7_asn_match_po.json5", Map.class));
        output("7_asn_match_po_response.json5", match);
        assertEquals(0, match.getStatus(), "matchPurchaseOrder 应成功");
        assertEquals("MATCHED", reload(ErpB2bAsn.class, asnId).getStatus(), "匹配后 MATCHED");

        // ---------- 4. createReceiveFromAsn（config 门控开 → RECEIVED_TO_STOCK + 收货草稿） ----------
        ApiResponse<?> createReceive = rpcMutation("ErpB2bAsn__createReceiveFromAsn",
                request("8_asn_create_receive.json5", Map.class));
        output("8_asn_create_receive_response.json5", createReceive);
        assertEquals(0, createReceive.getStatus(), "createReceiveFromAsn 应成功");
        assertEquals("RECEIVED_TO_STOCK", reload(ErpB2bAsn.class, asnId).getStatus(), "ASN 终态 RECEIVED_TO_STOCK");

        ErpPurReceive draft = findReceiveByCode(RECEIVE_CODE);
        assertNotNull(draft, "ASN 应自动创建采购收货草稿 " + RECEIVE_CODE);
        assertEquals(poId, String.valueOf(draft.getOrderId()), "收货草稿关联 PO");
        assertEquals("3", String.valueOf(draft.getSupplierId()), "收货草稿供应商透传 PO");
        assertEquals("2", String.valueOf(draft.getWarehouseId()), "收货草稿仓库透传 PO");
        assertEquals("UNSUBMITTED", draft.getDocStatus(), "收货草稿 docStatus=UNSUBMITTED");
        assertEquals("NOT_RECEIVED", draft.getReceiveStatus(), "收货草稿 receiveStatus=NOT_RECEIVED");
        addVar("receiveId", draft.getId());
        addVar("receiveCode", RECEIVE_CODE);

        List<ErpPurReceiveLine> draftLines = findReceiveLines(draft.getId());
        assertEquals(1, draftLines.size(), "行级回填 1 行");
        ErpPurReceiveLine draftLine = draftLines.get(0);
        assertEquals("1", String.valueOf(draftLine.getMaterialId()), "行物料透传 AsnLine");
        assertEquals("1", String.valueOf(draftLine.getUoMId()), "行 uoMId 反查 ErpMdMaterial");
        assertEquals(0, new BigDecimal("100").compareTo(draftLine.getQuantity()), "行数量=AsnLine.shippedQty");
        assertEquals(0, new BigDecimal("5").compareTo(draftLine.getUnitPrice()), "行单价反查 PO 行");
        assertEquals(0, PO_AMOUNT.compareTo(draftLine.getAmount()), "行金额=5×100=500");

        // ASN 创建链不落收货头 orgId（null 时过账账套不可解析 → PURCHASE_INPUT 零凭证 posted 悬挂 false）——
        // fixture 补全（fixAsnLineMaterialId 同型先例；GraphQL save-with-id 走建新路径不可用）
        ormTemplate.runInSession(() -> {
            ErpPurReceive r = daoProvider.daoFor(ErpPurReceive.class).getEntityById(draft.getId());
            r.setOrgId("2");
            daoProvider.daoFor(ErpPurReceive.class).saveOrUpdateEntity(r);
        });

        // ---------- 5. 收货审批（DIRECT 轴：submit → approve 内建过账，posted=true） ----------
        ApiResponse<?> rcvSubmit = rpcMutation("ErpPurReceive__submitForApproval",
                request("9_receive_submit.json5", Map.class));
        output("9_receive_submit_response.json5", rcvSubmit);
        assertEquals(0, rcvSubmit.getStatus(), "收货提交审批应成功");

        ApiResponse<?> rcvApprove = rpcMutation("ErpPurReceive__approve", request("10_receive_approve.json5", Map.class));
        output("10_receive_approve_response.json5", rcvApprove);
        assertEquals(0, rcvApprove.getStatus(), "收货审批应成功");
        ErpPurReceive approved = reload(ErpPurReceive.class, draft.getId());
        assertEquals("APPROVED", approved.getApproveStatus(), "收货 approveStatus=APPROVED");
        assertEquals("RECEIVED", approved.getReceiveStatus(), "收货 receiveStatus=RECEIVED");
        assertEquals(Boolean.TRUE, approved.getPosted(), "收货审批后 posted=true（入库移动+过账）");

        ErpInvStockMove inMove = findStockMove("ERP_PUR_RECEIVE", RECEIVE_CODE);
        assertNotNull(inMove, "收货审批应触发入库移动");
        assertEquals("INCOMING", inMove.getMoveType(), "入库移动 MOVE_TYPE=INCOMING");
        assertEquals("DONE", inMove.getDocStatus(), "入库移动 docStatus=DONE");

        // ---------- 7. 自包含物流：承运商 + 发运单（DISPATCHED / PURCHASE_RECEIPT 弱指针 / 运费 120） ----------
        ApiResponse<?> carrierSave = rpcMutation("ErpLogCarrier__save", request("11_carrier_save.json5", Map.class));
        output("11_carrier_save_response.json5", carrierSave);
        assertEquals(0, carrierSave.getStatus(), "承运商保存应成功");
        addVar("carrierId", idOf(carrierSave));

        ApiResponse<?> shipSave = rpcMutation("ErpLogShipment__save", request("12_shipment_save.json5", Map.class));
        output("12_shipment_save_response.json5", shipSave);
        assertEquals(0, shipSave.getStatus(), "发运单保存应成功");
        String shipmentId = idOf(shipSave);
        addVar("shipmentId", shipmentId);
        assertEquals("DISPATCHED", reload(ErpLogShipment.class, shipmentId).getStatus(), "发运单初始 DISPATCHED");

        // ---------- 8. handleTrackingWebhook（DELIVERED → path-2 到岸成本自动创建，SETTLED） ----------
        ApiResponse<?> webhook = rpcMutation("ErpLogShipment__handleTrackingWebhook",
                request("13_tracking_webhook.json5", Map.class));
        output("13_tracking_webhook_response.json5", webhook);
        assertEquals(0, webhook.getStatus(), "tracking webhook 应成功");
        ErpLogShipment delivered = reload(ErpLogShipment.class, shipmentId);
        assertEquals("DELIVERED", delivered.getStatus(), "送达事件后 DELIVERED");
        assertEquals("SETTLED", delivered.getFreightSettlementStatus(), "path-2 自动创建后运费结算 SETTLED");

        // ---------- 9. 到岸成本单断言（findPage 单查 + FREIGHT 行层 1 锚点） ----------
        ApiResponse<?> lcPage = executeRpc(io.nop.graphql.core.ast.GraphQLOperationType.query,
                "ErpInvLandedCost__findPage", request("14_landed_cost_find_page.json5", Map.class));
        output("14_landed_cost_find_page_response.json5", lcPage);
        assertEquals(0, lcPage.getStatus(), "到岸成本 findPage 应成功");
        assertEquals(1, ((Number) ((Map<?, ?>) lcPage.getData()).get("total")).intValue(),
                "该收货单应恰有 1 张自动创建的到岸成本单");

        ErpInvLandedCost landedCost = findLandedCostByReceive(draft.getId());
        assertNotNull(landedCost, "path-2 自动创建到岸成本单应落库");
        assertTrue(landedCost.getCode().startsWith("LC-FRT-" + RECEIVE_CODE), "到岸成本单号 LC-FRT-{receiveCode} 派生");
        assertEquals("DRAFT", landedCost.getDocStatus(), "自动创建到岸成本单 DRAFT");
        assertEquals("UNSUBMITTED", landedCost.getApproveStatus(), "到岸成本单 UNSUBMITTED");
        assertEquals(0, FREIGHT_AMOUNT.compareTo(landedCost.getTotalCostAmount()), "到岸成本总额=运费 120");
        assertEquals("BY_AMOUNT", landedCost.getAllocationMethod(), "默认分摊方法 BY_AMOUNT");
        assertEquals(draft.getId(), String.valueOf(landedCost.getReceiveId()), "到岸成本单引用 ASN 自动收货单");

        ErpInvLandedCostLine freightLine = findFreightLine(landedCost.getId());
        assertNotNull(freightLine, "FREIGHT 费用行应已创建");
        assertEquals("FREIGHT", freightLine.getCostElement(), "费用行成本元素=FREIGHT");
        assertEquals(0, FREIGHT_AMOUNT.compareTo(freightLine.getAmount()), "FREIGHT 行金额=运费 120");
        assertEquals("3", String.valueOf(freightLine.getApPartnerId()), "AP partner 默认取 receive.supplierId");
    }

    // ---------- helpers ----------

    private ErpPurReceive findReceiveByCode(String code) {
        List<ErpPurReceive> list = daoProvider.daoFor(ErpPurReceive.class).findAllByQuery(eqFilterQuery("code", code));
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpPurReceiveLine> findReceiveLines(String receiveId) {
        QueryBean q = eqFilterQuery("receiveId", receiveId);
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpPurReceiveLine.class).findAllByQuery(q);
    }

    private ErpInvLandedCost findLandedCostByReceive(String receiveId) {
        List<ErpInvLandedCost> list = daoProvider.daoFor(ErpInvLandedCost.class)
                .findAllByQuery(eqFilterQuery("receiveId", receiveId));
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvLandedCostLine findFreightLine(String landedCostId) {
        QueryBean q = eqFilterQuery("landedCostId", landedCostId);
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("costElement", "FREIGHT"));
        List<ErpInvLandedCostLine> list = daoProvider.daoFor(ErpInvLandedCostLine.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
