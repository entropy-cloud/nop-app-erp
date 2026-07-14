package app.erp.ast.service;

import app.erp.ast.biz.IErpAstCipBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstCipCostItem;
import app.erp.ast.dao.entity.ErpAstCipProgressBilling;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CIP 在建工程：成本归集 + 进度付款 + 完工转固（全部/部分）+ reverseTransfer 端到端单测（plan 0930-1 Phase 4）。
 *
 * <p>直接调用 BizModel Java API（CIP 转固是内部服务编排逻辑，断言实体状态而非响应 JSON）。
 * 覆盖：三态状态机 + 成本累加 + INTEREST_CAPITALIZATION config-gated + 全部/部分转固 + 重复转固防护 +
 * 进度款不参与转固 + reverseTransfer 红冲。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstCipTransfer extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstCipBiz cipBiz;

    // ---------- 场景 1：全部转固 happy path ----------

    @Test
    public void testFullTransferCreatesAssetVoucherAndTransitionsCip() {
        Long[] holders = new Long[2];
        ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithCipSubject("CAT-CIP-FULL", "自建工程类");
            holders[0] = categoryId;
            Long cipId = seedCip("CIP-FULL-001", categoryId,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
            holders[1] = cipId;
            return null;
        });
        Long categoryId = holders[0];
        Long cipId = holders[1];

        ormTemplate.runInSession(() -> cipBiz.startConstruction(cipId, CTX));
        ErpAstCipCostItem item1 = ormTemplate.runInSession(session -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_PURCHASE,
                new BigDecimal("8000"), null, null, "采购归集", CTX));
        ErpAstCipCostItem item2 = ormTemplate.runInSession(session -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_LABOR,
                new BigDecimal("4000"), null, null, "人工归集", CTX));
        ormTemplate.runInSession(() -> cipBiz.addProgressBilling(cipId, LocalDate.of(2026, 3, 15), "基础完工",
                new BigDecimal("5000"), "PAY-001", CTX));

        ErpAstCip beforeTransfer = daoProvider.daoFor(ErpAstCip.class).getEntityById(cipId);
        assertEquals(0, nz(beforeTransfer.getAccumulatedCost()).compareTo(new BigDecimal("12000")),
                "累计归集成本=12000");

        ormTemplate.runInSession(() -> cipBiz.transferToAsset(cipId, null, LocalDate.of(2026, 7, 1), CTX));

        ErpAstCip afterTransfer = daoProvider.daoFor(ErpAstCip.class).getEntityById(cipId);
        assertEquals(ErpAstConstants.CIP_STATUS_TRANSFERRED, afterTransfer.getStatus(),
                "CIP 终态=TRANSFERRED");
        assertTrue(Boolean.TRUE.equals(afterTransfer.getIsCompleted()), "isCompleted=true");
        assertNotNull(afterTransfer.getCompletedAssetId(), "completedAssetId 已回写");

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class)
                .getEntityById(afterTransfer.getCompletedAssetId());
        assertNotNull(asset, "转固资产卡片已创建");
        assertEquals(0, nz(asset.getOriginalValue()).compareTo(new BigDecimal("12000")),
                "资产原值=Σ CostItem amountFunctional");
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, asset.getStatus());

        List<ErpAstCipCostItem> items = ormTemplate.runInSession(session -> cipBiz.findCostItems(cipId, false, CTX));
        for (ErpAstCipCostItem item : items) {
            assertTrue(Boolean.TRUE.equals(item.getPostedTransferFlag()),
                    "CostItem " + item.getLineNo() + " postedTransferFlag=true");
        }

        List<ErpAstDepreciationSchedule> schedules = findSchedulesByAsset(asset.getId());
        assertFalse(schedules.isEmpty(), "折旧计划已生成");

        ErpAstAssetCapitalization cap = findCapitalizationEntityByCip(afterTransfer.getCode());
        assertNotNull(cap, "资本化单已创建");
        assertTrue(Boolean.TRUE.equals(cap.getPosted()), "资本化单 posted=true（CAPITALIZATION 凭证已生成）");
    }

    // ---------- 场景 2：部分转固 ----------

    @Test
    public void testPartialTransferKeepsInConstruction() {
        Long[] holders = new Long[2];
        ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithCipSubject("CAT-CIP-PART", "部分转固类");
            holders[0] = categoryId;
            Long cipId = seedCip("CIP-PART-001", categoryId,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
            holders[1] = cipId;
            return null;
        });
        Long cipId = holders[1];

        ormTemplate.runInSession(() -> cipBiz.startConstruction(cipId, CTX));
        ErpAstCipCostItem item1 = ormTemplate.runInSession(session -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_PURCHASE,
                new BigDecimal("5000"), null, null, null, CTX));
        ErpAstCipCostItem item2 = ormTemplate.runInSession(session -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_LABOR,
                new BigDecimal("3000"), null, null, null, CTX));

        ormTemplate.runInSession(() -> cipBiz.transferToAsset(cipId, Collections.singletonList(item1.getId()),
                LocalDate.of(2026, 6, 1), CTX));

        ErpAstCip cip = daoProvider.daoFor(ErpAstCip.class).getEntityById(cipId);
        assertEquals(ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION, cip.getStatus(),
                "部分转固后 CIP 保持 IN_CONSTRUCTION");
        assertFalse(Boolean.TRUE.equals(cip.getIsCompleted()), "isCompleted=false");

        ErpAstAssetCapitalization cap = findCapitalizationEntityByCip(cip.getCode());
        assertNotNull(cap, "部分转固也创建资本化单");
        assertEquals(0, nz(cap.getOriginalValue()).compareTo(new BigDecimal("5000")),
                "资本化单原值=部分汇总 5000");

        ErpAstCipCostItem reloadedItem1 = daoProvider.daoFor(ErpAstCipCostItem.class)
                .getEntityById(item1.getId());
        ErpAstCipCostItem reloadedItem2 = daoProvider.daoFor(ErpAstCipCostItem.class)
                .getEntityById(item2.getId());
        assertTrue(Boolean.TRUE.equals(reloadedItem1.getPostedTransferFlag()),
                "已转固 CostItem postedTransferFlag=true");
        assertFalse(Boolean.TRUE.equals(reloadedItem2.getPostedTransferFlag()),
                "未转固 CostItem postedTransferFlag=false");
    }

    // ---------- 场景 3：非法状态迁移 ----------

    @Test
    public void testIllegalStateTransitionDirectTransferFromDraft() {
        Long[] holders = new Long[1];
        ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithCipSubject("CAT-CIP-ILL", "非法迁移类");
            Long cipId = seedCip("CIP-ILL-001", categoryId,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
            holders[0] = cipId;
            return null;
        });
        Long cipId = holders[0];

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> cipBiz.transferToAsset(cipId, null, LocalDate.of(2026, 7, 1), CTX)));
        assertEquals(ErpAstErrors.ERR_CIP_NOT_IN_CONSTRUCTION.getErrorCode(), ex.getErrorCode(),
                "DRAFT 直接 transferToAsset 抛 ERR_CIP_NOT_IN_CONSTRUCTION");
    }

    @Test
    public void testIllegalStateTransferOnAlreadyCompleted() {
        Long[] holders = new Long[1];
        ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithCipSubject("CAT-CIP-COMP", "终态防护类");
            Long cipId = seedCip("CIP-COMP-001", categoryId,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
            holders[0] = cipId;
            return null;
        });
        Long cipId = holders[0];

        ormTemplate.runInSession(() -> cipBiz.startConstruction(cipId, CTX));
        ormTemplate.runInSession(() -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_PURCHASE,
                new BigDecimal("1000"), null, null, null, CTX));
        ormTemplate.runInSession(() -> cipBiz.transferToAsset(cipId, null, LocalDate.of(2026, 7, 1), CTX));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> cipBiz.transferToAsset(cipId, null, LocalDate.of(2026, 7, 2), CTX)));
        assertEquals(ErpAstErrors.ERR_CIP_ALREADY_COMPLETED.getErrorCode(), ex.getErrorCode(),
                "终态再 transferToAsset 抛 ERR_CIP_ALREADY_COMPLETED");
    }

    // ---------- 场景 4：重复转固防护 ----------

    @Test
    public void testReTransferAlreadyTransferredCostItem() {
        Long[] holders = new Long[2];
        ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithCipSubject("CAT-CIP-RE", "重复转固类");
            Long cipId = seedCip("CIP-RE-001", categoryId,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
            holders[0] = categoryId;
            holders[1] = cipId;
            return null;
        });
        Long cipId = holders[1];

        ormTemplate.runInSession(() -> cipBiz.startConstruction(cipId, CTX));
        ErpAstCipCostItem item1 = ormTemplate.runInSession(session -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_PURCHASE,
                new BigDecimal("2000"), null, null, null, CTX));
        ErpAstCipCostItem item2 = ormTemplate.runInSession(session -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_LABOR,
                new BigDecimal("3000"), null, null, null, CTX));

        ormTemplate.runInSession(() -> cipBiz.transferToAsset(cipId, Collections.singletonList(item1.getId()),
                LocalDate.of(2026, 6, 1), CTX));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> cipBiz.transferToAsset(cipId, Arrays.asList(item1.getId(), item2.getId()),
                        LocalDate.of(2026, 7, 1), CTX)));
        assertEquals(ErpAstErrors.ERR_CIP_COST_ITEM_ALREADY_TRANSFERRED.getErrorCode(), ex.getErrorCode(),
                "重复转固已转固 CostItem 抛 ERR_CIP_COST_ITEM_ALREADY_TRANSFERRED");
    }

    // ---------- 场景 5：INTEREST_CAPITALIZATION config-gated ----------

    @Test
    public void testInterestCapitalizationDisabledByDefault() {
        Long[] holders = new Long[1];
        ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithCipSubject("CAT-CIP-INT", "利息资本化类");
            Long cipId = seedCip("CIP-INT-001", categoryId,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
            holders[0] = cipId;
            return null;
        });
        Long cipId = holders[0];

        ormTemplate.runInSession(() -> cipBiz.startConstruction(cipId, CTX));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_INTEREST_CAPITALIZATION,
                        new BigDecimal("500"), null, null, null, CTX)));
        assertEquals(ErpAstErrors.ERR_CIP_INTEREST_CAPITALIZATION_DISABLED.getErrorCode(), ex.getErrorCode(),
                "config=false 时 INTEREST_CAPITALIZATION 拒收");

        ErpAstCipCostItem other = ormTemplate.runInSession(session -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_PURCHASE,
                new BigDecimal("1000"), null, null, null, CTX));
        assertNotNull(other, "其他类型不受 config 影响");
    }

    // ---------- 场景 6：进度付款不影响转固成本 ----------

    @Test
    public void testProgressBillingDoesNotAffectTransferCost() {
        Long[] holders = new Long[1];
        ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithCipSubject("CAT-CIP-BILL", "进度付款类");
            Long cipId = seedCip("CIP-BILL-001", categoryId,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
            holders[0] = cipId;
            return null;
        });
        Long cipId = holders[0];

        ormTemplate.runInSession(() -> cipBiz.startConstruction(cipId, CTX));
        ormTemplate.runInSession(() -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_PURCHASE,
                new BigDecimal("6000"), null, null, null, CTX));
        ormTemplate.runInSession(() -> cipBiz.addProgressBilling(cipId, LocalDate.of(2026, 2, 15), "里程碑1",
                new BigDecimal("9000"), "PAY-BIG", CTX));

        ormTemplate.runInSession(() -> cipBiz.transferToAsset(cipId, null, LocalDate.of(2026, 7, 1), CTX));

        ErpAstCip cip = daoProvider.daoFor(ErpAstCip.class).getEntityById(cipId);
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(cip.getCompletedAssetId());
        assertEquals(0, nz(asset.getOriginalValue()).compareTo(new BigDecimal("6000")),
                "资产原值=Σ CostItem（不含 ProgressBilling 9000）");
    }

    // ---------- 场景 7：reverseTransfer 红冲 ----------

    @Test
    public void testReverseTransferRollsBackCipStatus() {
        Long[] holders = new Long[2];
        ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithCipSubject("CAT-CIP-REV", "红冲类");
            Long cipId = seedCip("CIP-REV-001", categoryId,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
            holders[0] = categoryId;
            holders[1] = cipId;
            return null;
        });
        Long cipId = holders[1];

        ormTemplate.runInSession(() -> cipBiz.startConstruction(cipId, CTX));
        ormTemplate.runInSession(() -> cipBiz.addCostItem(cipId, ErpAstConstants.CIP_COST_TYPE_PURCHASE,
                new BigDecimal("7000"), null, null, null, CTX));
        ormTemplate.runInSession(() -> cipBiz.transferToAsset(cipId, null, LocalDate.of(2026, 7, 1), CTX));

        ErpAstCip cipBeforeReverse = daoProvider.daoFor(ErpAstCip.class).getEntityById(cipId);
        Long capitalizationId = findCapitalizationByCip(cipBeforeReverse.getCode());
        assertNotNull(capitalizationId, "找到资本化单");

        ormTemplate.runInSession(() -> cipBiz.reverseTransfer(cipId, capitalizationId, CTX));

        ErpAstCip cipAfterReverse = daoProvider.daoFor(ErpAstCip.class).getEntityById(cipId);
        assertEquals(ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION, cipAfterReverse.getStatus(),
                "红冲后 CIP 状态回 IN_CONSTRUCTION");
        assertFalse(Boolean.TRUE.equals(cipAfterReverse.getIsCompleted()), "isCompleted=false");
        assertEquals(null, cipAfterReverse.getCompletedAssetId(), "completedAssetId 清空");

        List<ErpAstCipCostItem> items = ormTemplate.runInSession(session -> cipBiz.findCostItems(cipId, false, CTX));
        for (ErpAstCipCostItem item : items) {
            assertFalse(Boolean.TRUE.equals(item.getPostedTransferFlag()),
                    "CostItem postedTransferFlag 回 false");
        }

        ErpAstAssetCapitalization cap = daoProvider.daoFor(ErpAstAssetCapitalization.class)
                .getEntityById(capitalizationId);
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, cap.getApproveStatus(),
                "资本化单已 reverseApprove");
    }

    // ---------- seed helpers ----------

    private void seedCoreBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, 1L);
        AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
        AstTestSupport.seedSubject(daoProvider, "1601", "固定资产");
        AstTestSupport.seedSubject(daoProvider, "1603", "在建工程");
    }

    private Long seedCategoryWithCipSubject(String code, String name) {
        IEntityDao<ErpAstAssetCategory> dao = daoProvider.daoFor(ErpAstAssetCategory.class);
        ErpAstAssetCategory category = new ErpAstAssetCategory();
        category.setCode(code);
        category.setName(name);
        category.setDepreciationMethod(ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE);
        category.setUsefulLifeMonths(12);
        Long fixedAssetSubjectId = findSubjectIdByCode("1601");
        Long cipSubjectId = findSubjectIdByCode("1603");
        category.setSubjectId(fixedAssetSubjectId);
        category.setCipSubjectId(cipSubjectId);
        dao.saveEntity(category);
        return category.getId();
    }

    private Long findSubjectIdByCode(String code) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    private Long seedCip(String code, Long categoryId, LocalDate businessDate,
                         LocalDate estimatedCompletionDate) {
        IEntityDao<ErpAstCip> dao = daoProvider.daoFor(ErpAstCip.class);
        ErpAstCip cip = new ErpAstCip();
        cip.setCode(code);
        cip.setName("CIP-" + code);
        cip.setOrgId(1L);
        cip.setCategoryId(categoryId);
        cip.setCurrencyId(1L);
        cip.setBusinessDate(businessDate);
        cip.setEstimatedCompletionDate(estimatedCompletionDate);
        cip.setAccumulatedCost(BigDecimal.ZERO);
        cip.setIsCompleted(false);
        cip.setStatus(ErpAstConstants.CIP_STATUS_DRAFT);
        cip.setExchangeRate(BigDecimal.ONE);
        cip.setAmountSource(BigDecimal.ZERO);
        cip.setAmountFunctional(BigDecimal.ZERO);
        dao.saveEntity(cip);
        return cip.getId();
    }

    // ---------- query helpers ----------

    private List<ErpAstDepreciationSchedule> findSchedulesByAsset(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherBillR> findCapitalizationBillLinks(Long cipId) {
        ErpAstCip cip = daoProvider.daoFor(ErpAstCip.class).getEntityById(cipId);
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", cip.getCode()), eq("businessType", "CAPITALIZATION")));
        return dao.findAllByQuery(q);
    }

    private ErpAstAssetCapitalization findCapitalizationEntityByCip(String cipCode) {
        IEntityDao<ErpAstAssetCapitalization> dao = daoProvider.daoFor(ErpAstAssetCapitalization.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceCode", cipCode));
        q.addFilter(eq("sourceType", ErpAstConstants.SOURCE_TYPE_CIP));
        q.setLimit(1);
        List<ErpAstAssetCapitalization> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long findCapitalizationByCip(String cipCode) {
        ErpAstAssetCapitalization cap = findCapitalizationEntityByCip(cipCode);
        return cap == null ? null : cap.getId();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
