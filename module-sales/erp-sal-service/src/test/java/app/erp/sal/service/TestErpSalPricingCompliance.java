package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialCategory;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.service.ErpMdErrors;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalPricingRule;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 销售促销定价族合规测试矩阵（RC-R1.14 + RC-R1.15）：
 *
 * <p><b>P1-RC-022 价税分离（RC-R1.15）</b>：{@code recomputeLineAmount} 按 L1 公式重算
 * taxAmount = net × rate / (1+rate) + amountWithTax = net + taxAmount；头级 totalAmountWithTax = totalAmount + totalTaxAmount 恒等式。
 * <ol>
 *   <li>单档折扣（13%，net=900 → tax≈103.5398）</li>
 *   <li>零税率行 taxAmount=0</li>
 *   <li>多档混合（13%/9%/6%）+ 行级 PERCENT_DISCOUNT + 头级 AMOUNT_OFF → 逐行 + Σ 恒等式</li>
 *   <li>促销前后 taxAmount 更新（A4.2.49 反转断言）</li>
 *   <li>无折扣行净额不变回归</li>
 * </ol>
 *
 * <p><b>P1-RC-021 最低价校验（RC-R1.14）</b>：{@code validatePromotionPrices} 复用 master-data
 * {@code IErpMdMaterialSkuBiz.validatePrice} 三级语义（OFF/WARN/HARD），赠品行（amount==0）跳过。
 * <ol>
 *   <li>OFF 级别促销后低于底线放行（零干预回归）</li>
 *   <li>WARN 级别低于底线放行 + warning 标记</li>
 *   <li>HARD 级别低于底线拒绝（ERR_PRICE_BELOW_MIN + 事务回滚）</li>
 *   <li>无 SKU 行（skuId null）跳过</li>
 *   <li>赠品行跳过（HARD 级别含赠品促销成功生成）</li>
 * </ol>
 *
 * <p>种子数据经 DAO 直接写入 H2（对齐 {@link TestErpSalOrderAvailabilityCheck} 范式），
 * 经 {@code ErpSalOrder__applyPricingRules} RPC 触发促销应用 + 价税分离 + 最低价校验。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalPricingCompliance extends JunitAutoTestCase {

    static final Long CUSTOMER_ID = 7401L;
    static final Long CURRENCY_ID = 7402L;
    static final Long UOM_ID = 7403L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ==================== P1-RC-022 价税分离 ====================

    @Test
    public void testTaxSeparation_SinglePercentDiscount() {
        Long matId = 7101L;
        Long orderId = 7102L;
        ormTemplate.runInSession(() -> {
            seedPrereqs();
            seedMaterial(matId, UOM_ID, null);
            seedRule(percentDiscountRule(100, matId, "10"));
            saveOrder(orderId, "SO-TAX-001");
            saveLine(orderId, 1, matId, null, UOM_ID, "100", "10", "13");
        });

        ApiResponse<?> resp = applyPricingRules(orderId);
        assertEquals(0, resp.getStatus(), "促销应用成功");

        ErpSalOrderLine line = reloadLine(orderId, 1);
        assertEquals(0, new BigDecimal("900.0000").compareTo(line.getAmount()));
        assertEquals(0, new BigDecimal("103.5398").compareTo(line.getTaxAmount()),
                "单档折扣后税额按 L1 公式重算");
        assertEquals(0, new BigDecimal("1003.5398").compareTo(line.getAmountWithTax()));

        ErpSalOrder order = reloadOrder(orderId);
        assertEquals(0, new BigDecimal("103.5398").compareTo(order.getTotalTaxAmount()));
        assertEquals(0, order.getTotalAmountWithTax().compareTo(order.getTotalAmount().add(order.getTotalTaxAmount())),
                "头级恒等式 totalAmountWithTax = totalAmount + totalTaxAmount");
    }

    @Test
    public void testTaxSeparation_ZeroRate() {
        Long matId = 7201L;
        Long orderId = 7202L;
        ormTemplate.runInSession(() -> {
            seedPrereqs();
            seedMaterial(matId, UOM_ID, null);
            seedRule(percentDiscountRule(100, matId, "10"));
            saveOrder(orderId, "SO-TAX-002");
            saveLine(orderId, 1, matId, null, UOM_ID, "100", "10", "0");
        });

        ApiResponse<?> resp = applyPricingRules(orderId);
        assertEquals(0, resp.getStatus());

        ErpSalOrderLine line = reloadLine(orderId, 1);
        assertEquals(0, BigDecimal.ZERO.compareTo(line.getTaxAmount()), "零税率行 taxAmount=0");
        assertEquals(0, new BigDecimal("900.0000").compareTo(line.getAmountWithTax()),
                "零税率行 amountWithTax = net");
    }

    @Test
    public void testTaxSeparation_MultiRateMixedWithHeaderDiscount() {
        Long mat1 = 7301L, mat2 = 7302L, mat3 = 7303L;
        Long orderId = 7304L;
        ormTemplate.runInSession(() -> {
            seedPrereqs();
            seedMaterial(mat1, UOM_ID, null);
            seedMaterial(mat2, UOM_ID, null);
            seedMaterial(mat3, UOM_ID, null);
            seedRule(percentDiscountRule(100, mat1, "10"));
            ErpSalPricingRule amtOff = newRule(200, "AMOUNT_OFF", "ORDER");
            amtOff.setMinOrderAmount(BigDecimal.ZERO);
            amtOff.setDiscountAmount(new BigDecimal("50"));
            amtOff.setStackable(true);
            daoProvider.daoFor(ErpSalPricingRule.class).saveEntity(amtOff);

            saveOrder(orderId, "SO-TAX-003");
            saveLine(orderId, 1, mat1, null, UOM_ID, "100", "10", "13");
            saveLine(orderId, 2, mat2, null, UOM_ID, "100", "10", "9");
            saveLine(orderId, 3, mat3, null, UOM_ID, "100", "10", "6");
        });

        ApiResponse<?> resp = applyPricingRules(orderId);
        assertEquals(0, resp.getStatus());

        ErpSalOrderLine l1 = reloadLine(orderId, 1);
        ErpSalOrderLine l2 = reloadLine(orderId, 2);
        ErpSalOrderLine l3 = reloadLine(orderId, 3);
        assertEquals(0, new BigDecimal("103.5398").compareTo(l1.getTaxAmount()));
        assertEquals(0, new BigDecimal("82.5688").compareTo(l2.getTaxAmount()));
        assertEquals(0, new BigDecimal("56.6038").compareTo(l3.getTaxAmount()));

        ErpSalOrder order = reloadOrder(orderId);
        BigDecimal expectedTotalTax = new BigDecimal("103.5398")
                .add(new BigDecimal("82.5688")).add(new BigDecimal("56.6038"));
        assertEquals(0, expectedTotalTax.compareTo(order.getTotalTaxAmount()),
                "头级 totalTaxAmount = Σ 行 taxAmount");
        assertEquals(0, order.getTotalAmountWithTax().compareTo(order.getTotalAmount().add(order.getTotalTaxAmount())),
                "头级恒等式 totalAmountWithTax = totalAmount + totalTaxAmount");
    }

    @Test
    public void testTaxSeparation_PromotionReversesTaxAmount() {
        Long matId = 7401L;
        Long orderId = 7402L;
        ormTemplate.runInSession(() -> {
            seedPrereqs();
            seedMaterial(matId, UOM_ID, null);
            seedRule(percentDiscountRule(100, matId, "50"));
            saveOrder(orderId, "SO-TAX-004");
            saveLine(orderId, 1, matId, null, UOM_ID, "100", "1", "13");
        });

        ApiResponse<?> resp = applyPricingRules(orderId);
        assertEquals(0, resp.getStatus());

        ErpSalOrderLine line = reloadLine(orderId, 1);
        // gross=100, discount=50, net=50 → tax = 50×0.13/1.13 = 5.7522
        BigDecimal prePromotionTax = new BigDecimal("100").multiply(new BigDecimal("0.13"))
                .divide(BigDecimal.ONE.add(new BigDecimal("0.13")), 4, RoundingMode.HALF_UP);
        assertEquals(0, new BigDecimal("5.7522").compareTo(line.getTaxAmount()),
                "促销后 taxAmount 按折扣后金额重算（A4.2.49 反转：应≈5.75 非 11.50）");
        assertNotEquals(0, prePromotionTax.compareTo(line.getTaxAmount()),
                "taxAmount 不再沿用促销前旧值");
    }

    @Test
    public void testTaxSeparation_NoDiscountNetUnchanged() {
        Long matId = 7501L;
        Long orderId = 7502L;
        ormTemplate.runInSession(() -> {
            seedPrereqs();
            seedMaterial(matId, UOM_ID, null);
            saveOrder(orderId, "SO-TAX-005");
            saveLine(orderId, 1, matId, null, UOM_ID, "100", "10", "13");
        });

        ApiResponse<?> resp = applyPricingRules(orderId);
        assertEquals(0, resp.getStatus());

        ErpSalOrderLine line = reloadLine(orderId, 1);
        assertEquals(0, new BigDecimal("1000.0000").compareTo(line.getAmount()),
                "无折扣行净额不变");
        assertEquals(0, new BigDecimal("115.0442").compareTo(line.getTaxAmount()),
                "无折扣行 taxAmount = 1000×0.13/1.13");
    }

    // ==================== P1-RC-021 最低价校验 ====================

    @Test
    public void testMinPrice_OffLevelBelowMinAllows() {
        runMinPriceScenario(7601L, 7601L, 8601L, "SO-MIN-OFF", "OFF");
    }

    @Test
    public void testMinPrice_WarnLevelBelowMinAllows() {
        runMinPriceScenario(7602L, 7602L, 8602L, "SO-MIN-WARN", "WARN");
    }

    @Test
    public void testMinPrice_HardLevelBelowMinRejects() {
        Long catId = 7603L;
        Long matId = 8603L;
        Long skuId = 9603L;
        Long orderId = 7604L;
        ormTemplate.runInSession(() -> {
            seedPrereqs();
            seedCategory(catId, "HARD");
            seedMaterial(matId, UOM_ID, catId);
            seedSku(skuId, matId, "50");
            seedRule(percentDiscountRule(100, matId, "60"));
            saveOrder(orderId, "SO-MIN-HARD");
            saveLine(orderId, 1, matId, skuId, UOM_ID, "100", "1", "13");
        });

        ApiResponse<?> resp = applyPricingRules(orderId);
        assertNotEquals(0, resp.getStatus(), "HARD 级别促销后低于底线 → 拒绝");
        assertEquals(ErpMdErrors.ERR_PRICE_BELOW_MIN.getErrorCode(), resp.getCode(),
                "错误码 = ERR_PRICE_BELOW_MIN（master-data propagate）");

        ErpSalOrderLine line = reloadLine(orderId, 1);
        BigDecimal discount = line.getDiscountAmount();
        assertTrue(discount == null || discount.signum() == 0,
                "HARD 拒绝事务回滚 → 行折扣不落库");
    }

    @Test
    public void testMinPrice_NoSkuLineSkips() {
        Long matId = 7701L;
        Long orderId = 7702L;
        ormTemplate.runInSession(() -> {
            seedPrereqs();
            seedMaterial(matId, UOM_ID, null);
            seedRule(percentDiscountRule(100, matId, "60"));
            saveOrder(orderId, "SO-MIN-NOSKU");
            saveLine(orderId, 1, matId, null, UOM_ID, "100", "1", "13");
        });

        ApiResponse<?> resp = applyPricingRules(orderId);
        assertEquals(0, resp.getStatus(), "无 SKU 行跳过最低价校验（促销正常应用）");
    }

    @Test
    public void testMinPrice_GiftLineSkippedUnderHard() {
        Long catId = 7801L;
        Long matId = 7802L;
        Long giftMatId = 7803L;
        Long skuId = 7804L;
        Long giftSkuId = 7805L;
        Long orderId = 7806L;
        ormTemplate.runInSession(() -> {
            seedPrereqs();
            seedCategory(catId, "HARD");
            seedMaterial(matId, UOM_ID, catId);
            seedMaterial(giftMatId, UOM_ID, catId);
            seedSku(skuId, matId, "50");
            seedSku(giftSkuId, giftMatId, "50");
            ErpSalPricingRule gift = newRule(100, "GIFT", "LINE");
            gift.setMaterialId(matId);
            gift.setGiftMaterialId(giftMatId);
            gift.setGiftSkuId(giftSkuId);
            gift.setGiftQuantity(BigDecimal.ONE);
            daoProvider.daoFor(ErpSalPricingRule.class).saveEntity(gift);

            saveOrder(orderId, "SO-MIN-GIFT");
            saveLine(orderId, 1, matId, skuId, UOM_ID, "100", "1", "13");
        });

        ApiResponse<?> resp = applyPricingRules(orderId);
        assertEquals(0, resp.getStatus(),
                "赠品行（amount==0）跳过最低价校验 → HARD 级别含赠品促销成功生成（防误拒回归）");

        List<ErpSalOrderLine> lines = reloadLines(orderId);
        assertEquals(2, lines.size(), "赠品行成功追加");
        ErpSalOrderLine giftLine = lines.stream()
                .filter(l -> nullSafe(l.getAmount()).signum() == 0)
                .findFirst().orElse(null);
        assertNotNull(giftLine, "存在 amount==0 的赠品行");
    }

    // ==================== helpers ====================

    private void runMinPriceScenario(Long catId, Long orderId, Long matId, String orderCode, String level) {
        Long skuId = matId + 10000L;
        ormTemplate.runInSession(() -> {
            seedPrereqs();
            seedCategory(catId, level);
            seedMaterial(matId, UOM_ID, catId);
            seedSku(skuId, matId, "50");
            seedRule(percentDiscountRule(100, matId, "60"));
            saveOrder(orderId, orderCode);
            saveLine(orderId, 1, matId, skuId, UOM_ID, "100", "1", "13");
        });

        ApiResponse<?> resp = applyPricingRules(orderId);
        assertEquals(0, resp.getStatus(),
                level + " 级别促销后低于底线 → 放行（OFF/WARN 不阻断）");
    }

    private ApiResponse<?> applyPricingRules(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__applyPricingRules",
                ApiRequest.build(Map.of("orderId", String.valueOf(orderId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpSalOrder reloadOrder(Long orderId) {
        return daoProvider.daoFor(ErpSalOrder.class).getEntityById(orderId);
    }

    private ErpSalOrderLine reloadLine(Long orderId, int lineNo) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        q.addFilter(eq("lineNo", lineNo));
        List<ErpSalOrderLine> list = daoProvider.daoFor(ErpSalOrderLine.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpSalOrderLine> reloadLines(Long orderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return new ArrayList<>(daoProvider.daoFor(ErpSalOrderLine.class).findAllByQuery(q));
    }

    // ---------- seed helpers ----------

    private void seedPrereqs() {
        IEntityDao<ErpMdPartner> partnerDao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = partnerDao.newEntity();
        partner.setId(CUSTOMER_ID);
        partner.setCode("CUS-7PC");
        partner.setName("合规测试客户");
        partner.setPartnerType("CUSTOMER");
        partner.setStatus("ACTIVE");
        partnerDao.saveEntity(partner);

        IEntityDao<ErpMdUoM> uomDao = daoProvider.daoFor(ErpMdUoM.class);
        ErpMdUoM uom = uomDao.newEntity();
        uom.setId(UOM_ID);
        uom.setCode("PCS7");
        uom.setName("个");
        uomDao.saveEntity(uom);
    }

    private void seedCategory(Long id, String priceValidationLevel) {
        IEntityDao<ErpMdMaterialCategory> dao = daoProvider.daoFor(ErpMdMaterialCategory.class);
        ErpMdMaterialCategory cat = dao.newEntity();
        cat.setId(id);
        cat.setCode("CAT-" + id);
        cat.setName("分类" + id);
        cat.setPriceValidationLevel(priceValidationLevel);
        dao.saveEntity(cat);
    }

    private void seedMaterial(Long id, Long uomId, Long categoryId) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial mat = dao.newEntity();
        mat.setId(id);
        mat.setCode("MAT-" + id);
        mat.setName("物料" + id);
        mat.setMaterialType("GOODS");
        mat.setUoMId(uomId);
        mat.setStatus("ACTIVE");
        mat.setCategoryId(categoryId);
        dao.saveEntity(mat);
    }

    private void seedSku(Long id, Long materialId, String salePrice) {
        IEntityDao<ErpMdMaterialSku> dao = daoProvider.daoFor(ErpMdMaterialSku.class);
        ErpMdMaterialSku sku = dao.newEntity();
        sku.setId(id);
        sku.setMaterialId(materialId);
        sku.setSkuCode("SKU-" + id);
        sku.setUoMId(UOM_ID);
        sku.setSalePrice(new BigDecimal(salePrice));
        sku.setIsDefault(true);
        dao.saveEntity(sku);
    }

    private void seedRule(ErpSalPricingRule rule) {
        daoProvider.daoFor(ErpSalPricingRule.class).saveEntity(rule);
    }

    private ErpSalPricingRule percentDiscountRule(int priority, Long materialId, String percent) {
        ErpSalPricingRule rule = newRule(priority, "PERCENT_DISCOUNT", "LINE");
        rule.setMaterialId(materialId);
        rule.setDiscountPercent(new BigDecimal(percent));
        return rule;
    }

    private ErpSalPricingRule newRule(int priority, String ruleType, String targetType) {
        ErpSalPricingRule rule = daoProvider.daoFor(ErpSalPricingRule.class).newEntity();
        rule.setRuleCode("R-" + ruleType + "-" + System.nanoTime());
        rule.setRuleName("Test " + ruleType);
        rule.setRuleType(ruleType);
        rule.setTargetType(targetType);
        rule.setPriority(priority);
        rule.setIsActive(true);
        rule.setStackable(false);
        return rule;
    }

    private void saveOrder(Long id, String code) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder order = dao.newEntity();
        order.setId(id);
        order.setCode(code);
        order.setCustomerId(CUSTOMER_ID);
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(BigDecimal.ONE);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        order.setPosted(false);
        dao.saveEntity(order);
    }

    private void saveLine(Long orderId, int lineNo, Long materialId, Long skuId, Long uomId,
                          String unitPrice, String qty, String taxRate) {
        IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = dao.newEntity();
        line.setOrderId(orderId);
        line.setLineNo(lineNo);
        line.setMaterialId(materialId);
        line.setSkuId(skuId);
        line.setUoMId(uomId);
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setQuantity(new BigDecimal(qty));
        line.setTaxRate(new BigDecimal(taxRate));
        line.setAmount(new BigDecimal(unitPrice).multiply(new BigDecimal(qty)));
        dao.saveEntity(line);
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
