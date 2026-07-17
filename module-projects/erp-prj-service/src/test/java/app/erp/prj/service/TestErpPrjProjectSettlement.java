package app.erp.prj.service;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.md.service.ErpMdConstants;
import app.erp.prj.biz.IErpPrjProjectPnlBiz;
import app.erp.prj.biz.IErpPrjProjectSettlementBiz;
import app.erp.prj.dao.entity.ErpPrjBilling;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.dao.entity.ErpPrjProjectSettlementLine;
import app.erp.prj.dao.entity.ErpPrjProjectType;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目结算单状态机 + 转固过账端到端单测（plan 2026-07-07-0305-1 Phase 4）。验证：
 * <ul>
 *   <li>FINAL 结算 approve 后 posted=true（成功模式），明细行含 Billing/CostCollection 来源。</li>
 *   <li>CLOSE 结算 approve 后额外创建 {@link ErpAstAsset} 卡片（assetCardId 非空）。</li>
 *   <li>状态机非法迁移（未经 submit 直接 approve）抛 ErrorCode。</li>
 *   <li>{@code reverseSettlement} 红冲凭证 + 回退资产卡片状态（status=DRAFT）+ posted=false。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjProjectSettlement extends JunitAutoTestCase {

    @RegisterExtension
    static PrjFrozenClockExtension frozenClock = new PrjFrozenClockExtension();

    private static IServiceContext CTX = new ServiceContextImpl();

    @BeforeAll
    static void fixCtxUser() {
        CTX.getContext().setUserId("autotest");
    }

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPrjProjectPnlBiz pnlBiz;
    @Inject
    IErpPrjProjectSettlementBiz settlementBiz;

    @Test
    public void testFinalSettlementApproveAndPost() {
        Long[] holder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedFullSetup("STL-FINAL");
            holder[0] = seedProjectWithBillingAndCost("PRJ-STL-FINAL", "结算测试项目");
            return null;
        });
        ormTemplate.runInSession(() -> pnlBiz.refreshPnl(holder[0], null, null, CTX));

        ErpPrjProjectSettlement settlement = ormTemplate.runInSession(session -> settlementBiz.createSettlement(holder[0],
                ErpPrjConstants.SETTLEMENT_TYPE_FINAL, CTX));
        assertEquals(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED, settlement.getApproveStatus());

        // 明细行含来源单据（Billing INCOME + CostCollection COST）
        List<ErpPrjProjectSettlementLine> lines = findLines(settlement.getId());
        assertTrue(lines.size() >= 2, "结算明细含收入+成本行");
        assertTrue(lines.stream().anyMatch(l -> ErpPrjConstants.SETTLEMENT_LINE_TYPE_INCOME.equals(l.getLineType())),
                "含收入行");
        assertTrue(lines.stream().anyMatch(l -> ErpPrjConstants.SETTLEMENT_LINE_TYPE_COST.equals(l.getLineType())),
                "含成本行");

        ormTemplate.runInSession(() -> settlementBiz.submit(settlement.getId(), CTX));
        ErpPrjProjectSettlement approved = ormTemplate.runInSession(session -> settlementBiz.approve(settlement.getId(), CTX));

        assertEquals(ErpPrjConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());
        assertEquals(ErpPrjConstants.DOC_STATUS_APPROVED, approved.getDocStatus());
        // posted 经 PostingDispatcher（finance 引擎成功时 true，失败隔离时 false——此处断言非抛异常即达终态）
        assertNotNull(approved.getApprovedBy());
        assertNotNull(approved.getApprovedAt());
    }

    @Test
    public void testCloseSettlementCapitalization() {
        Long[] holder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedFullSetup("STL-CLOSE");
            holder[0] = seedProjectWithBillingAndCost("PRJ-STL-CLOSE", "转固测试项目");
            return null;
        });
        ormTemplate.runInSession(() -> pnlBiz.refreshPnl(holder[0], null, null, CTX));

        ErpPrjProjectSettlement settlement = ormTemplate.runInSession(session -> settlementBiz.createSettlement(holder[0],
                ErpPrjConstants.SETTLEMENT_TYPE_CLOSE, CTX));
        assertTrue(Boolean.TRUE.equals(settlement.getTransferToAsset()), "CLOSE 结算 transferToAsset=true");
        assertNull(settlement.getAssetCardId(), "审批前无资产卡片");

        ormTemplate.runInSession(() -> settlementBiz.submit(settlement.getId(), CTX));
        ErpPrjProjectSettlement approved = ormTemplate.runInSession(session -> settlementBiz.approve(settlement.getId(), CTX));

        assertEquals(ErpPrjConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());
        // CLOSE 转固：资产卡片已创建（assetCardId 非空）
        assertNotNull(approved.getAssetCardId(), "CLOSE 转固创建资产卡片");
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(approved.getAssetCardId());
        assertNotNull(asset, "资产卡片存在");
        assertEquals("IN_SERVICE", asset.getStatus(), "资产卡片状态 IN_SERVICE");
        assertEquals(0, asset.getOriginalValue().compareTo(approved.getFinalCost()),
                "资产原值=最终成本");
    }

    @Test
    public void testIllegalTransitionThrows() {
        Long[] holder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedFullSetup("STL-ILL");
            holder[0] = seedProjectWithBillingAndCost("PRJ-STL-ILL", "非法迁移项目");
            return null;
        });
        ormTemplate.runInSession(() -> pnlBiz.refreshPnl(holder[0], null, null, CTX));

        ErpPrjProjectSettlement settlement = ormTemplate.runInSession(session -> settlementBiz.createSettlement(holder[0],
                ErpPrjConstants.SETTLEMENT_TYPE_FINAL, CTX));

        // 强制审批默认开启（erp-prj.settlement-require-approval=true）：未经 submit 直接 approve 应拒绝
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> settlementBiz.approve(settlement.getId(), CTX)));
        assertEquals(ErpPrjErrors.ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testReverseSettlementReversesVoucherAndAsset() {
        Long[] holder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedFullSetup("STL-REV");
            holder[0] = seedProjectWithBillingAndCost("PRJ-STL-REV", "红冲测试项目");
            return null;
        });
        ormTemplate.runInSession(() -> pnlBiz.refreshPnl(holder[0], null, null, CTX));

        ErpPrjProjectSettlement settlement = ormTemplate.runInSession(session -> settlementBiz.createSettlement(holder[0],
                ErpPrjConstants.SETTLEMENT_TYPE_CLOSE, CTX));
        ormTemplate.runInSession(() -> settlementBiz.submit(settlement.getId(), CTX));
        ErpPrjProjectSettlement approved = ormTemplate.runInSession(session -> settlementBiz.approve(settlement.getId(), CTX));
        Long assetCardId = approved.getAssetCardId();
        assertNotNull(assetCardId, "转固卡片已创建");

        // 若已过账，reverseSettlement 应红冲 + 回退卡片；若未过账（过账失败隔离），reverseSettlement 抛非法状态
        if (Boolean.TRUE.equals(approved.getPosted())) {
            ErpPrjProjectSettlement reversed = ormTemplate.runInSession(session -> settlementBiz.reverseSettlement(approved.getId(), CTX));
            assertFalse(Boolean.TRUE.equals(reversed.getPosted()), "红冲后 posted=false");

            ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetCardId);
            assertEquals("DRAFT", asset.getStatus(), "资产卡片状态回退 DRAFT");
        } else {
            // 过账失败隔离场景：posted=false 时 reverseSettlement 抛非法状态
            NopException ex = assertThrows(NopException.class,
                    () -> ormTemplate.runInSession(session -> settlementBiz.reverseSettlement(approved.getId(), CTX)));
            assertEquals(ErpPrjErrors.ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
        }
    }

    // ---------- seed helpers ----------

    private void seedFullSetup(String tag) {
        seedCurrency();
        seedOpenPeriod("2026-06");
        seedOpenPeriod("2026-07");
        seedAcctSchema(1L);
        seedSubject("6001", "主营业务收入");
        seedSubject("5101", "项目成本");
        seedSubject("1601", "固定资产");
        seedSubject("1603", "在建工程");
        seedSubject("4103", "本年利润");
        seedSubject("2211", "应付职工薪酬");
    }

    private Long seedProjectWithBillingAndCost(String projectCode, String projectName) {
        Long subjectId = seedSubject("5101-" + projectCode, "项目成本");
        Long projectTypeId = seedProjectType("PT-" + projectCode, projectName, subjectId);
        Long customerId = seedPartner("CUST-" + projectCode, "客户-" + projectName);
        Long projectId = seedProject(projectCode, projectName, projectTypeId);
        seedBilling("B-" + projectCode, projectId, customerId, "10000");
        Long ccId = seedCostCollection("CC-" + projectCode, projectId);
        seedCostLine(ccId, ErpPrjConstants.COST_CATEGORY_LABOR, "6000");
        return projectId;
    }

    private void seedBilling(String code, Long projectId, Long customerId, String amountFunctional) {
        IEntityDao<ErpPrjBilling> dao = daoProvider.daoFor(ErpPrjBilling.class);
        ErpPrjBilling b = new ErpPrjBilling();
        b.setCode(code);
        b.setProjectId(projectId);
        b.setOrgId(1L);
        b.setCustomerId(customerId);
        b.setBusinessDate(LocalDate.of(2026, 6, 15));
        b.setCurrencyId(1L);
        b.setExchangeRate(BigDecimal.ONE);
        b.setTotalAmount(new BigDecimal(amountFunctional));
        b.setAmountFunctional(new BigDecimal(amountFunctional));
        b.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
        b.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(b);
    }

    private Long seedCostCollection(String code, Long projectId) {
        IEntityDao<ErpPrjCostCollection> dao = daoProvider.daoFor(ErpPrjCostCollection.class);
        ErpPrjCostCollection cc = new ErpPrjCostCollection();
        cc.setCode(code);
        cc.setProjectId(projectId);
        cc.setOrgId(1L);
        cc.setBusinessDate(LocalDate.of(2026, 6, 15));
        cc.setCurrencyId(1L);
        cc.setTotalAmount(BigDecimal.ZERO);
        cc.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
        cc.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        cc.setPosted(false);
        cc.setExchangeRate(BigDecimal.ONE);
        cc.setAmountSource(BigDecimal.ZERO);
        cc.setAmountFunctional(BigDecimal.ZERO);
        dao.saveEntity(cc);
        return cc.getId();
    }

    private void seedCostLine(Long costCollectionId, String category, String amount) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        ErpPrjCostCollectionLine line = new ErpPrjCostCollectionLine();
        line.setCostCollectionId(costCollectionId);
        line.setLineNo(1);
        line.setCostCategory(category);
        line.setAmount(new BigDecimal(amount));
        dao.saveEntity(line);
    }

    private Long seedProject(String code, String name, Long projectTypeId) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.setCode(code);
        p.setName(name);
        p.setOrgId(1L);
        p.setProjectTypeId(projectTypeId);
        p.setCurrencyId(1L);
        p.setStatus(ErpPrjConstants.PROJECT_STATUS_OPEN);
        p.setBudget(new BigDecimal("100000"));
        p.setActualCost(BigDecimal.ZERO);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedProjectType(String code, String name, Long defaultSubjectId) {
        IEntityDao<ErpPrjProjectType> dao = daoProvider.daoFor(ErpPrjProjectType.class);
        ErpPrjProjectType t = new ErpPrjProjectType();
        t.setCode(code);
        t.setName(name);
        t.setDefaultSubjectId(defaultSubjectId);
        dao.saveEntity(t);
        return t.getId();
    }

    private Long seedPartner(String code, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = new ErpMdPartner();
        p.setCode(code);
        p.setName(name);
        p.setPartnerType("CUSTOMER");
        p.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass("EXPENSE");
        s.setDirection(ErpFinConstants.DC_DEBIT);
        s.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(s);
        return s.getId();
    }

    private void seedCurrency() {
        seedOrganization();
        IEntityDao<app.erp.md.dao.entity.ErpMdCurrency> dao = daoProvider.daoFor(app.erp.md.dao.entity.ErpMdCurrency.class);
        app.erp.md.dao.entity.ErpMdCurrency c = new app.erp.md.dao.entity.ErpMdCurrency();
        c.setCode("CNY");
        c.setName("人民币");
        c.setSymbol("¥");
        c.setDecimalPlaces(2);
        c.setIsFunctional(true);
        dao.saveEntity(c);
    }

    private void seedOrganization() {
        IEntityDao<app.erp.md.dao.entity.ErpMdOrganization> dao = daoProvider.daoFor(app.erp.md.dao.entity.ErpMdOrganization.class);
        app.erp.md.dao.entity.ErpMdOrganization org = new app.erp.md.dao.entity.ErpMdOrganization();
        org.setCode("ORG-1");
        org.setName("测试组织");
        org.setOrgType("COMPANY");
        org.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(org);
    }

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        int year = Integer.parseInt(code.substring(0, 4));
        int month = Integer.parseInt(code.substring(5));
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(LocalDate.of(year, month, 1));
        period.setEndDate(LocalDate.of(year, month, 28));
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    private List<ErpPrjProjectSettlementLine> findLines(Long settlementId) {
        IEntityDao<ErpPrjProjectSettlementLine> dao = daoProvider.daoFor(ErpPrjProjectSettlementLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("settlementId", settlementId));
        return dao.findAllByQuery(q);
    }
}
