package app.erp.prj.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
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
import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目结算质保金（RC-R1.63 / P1-RC-052，UC-PRJ-07 ④⑤，plan 2026-08-17-0142-1）端到端测试。
 * 验证 D1 留存填充 + D2 质保金凭证 + D3 到期返还 mutation 守卫链/幂等 + reverseSettlement/cancel 未返还守卫。
 *
 * <ul>
 *   <li>FINAL 结算 createSettlement 自动填充 retentionAmount（ratio×finalRevenue）与 retentionDueDate（businessDate+N月）；</li>
 *   <li>INTERIM/CLOSE 不填充（零静默留存）；</li>
 *   <li>主结算凭证含留存腿（借 1122 应收账款-质保金 / 贷 2241 其他应付款-质保金，金额=retentionAmount，标 projectId）；</li>
 *   <li>到期返还生成镜像对冲凭证（借 2241 / 贷 1122，billHeadCode=结算单号#RETURN）且幂等 no-op；</li>
 *   <li>守卫链（非 APPROVED / 未过账 / 未到期 / retentionAmount=0）拒绝并抛 ERR_RETENTION_RETURN_NOT_ALLOWED；</li>
 *   <li>已返还后 reverseSettlement 拒绝（避免返还凭证悬挂，Explore ⑥）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjProjectSettlementRetention extends JunitAutoTestCase {

    @RegisterExtension
    static PrjFrozenClockExtension frozenClock = new PrjFrozenClockExtension();

    private static IServiceContext CTX = new ServiceContextImpl();

    @BeforeAll
    static void fixCtxUser() {
        CTX.getContext().setUserId("autotest");
    }

    @AfterEach
    void clearConfig() {
        System.clearProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO);
        System.clearProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_DUE_MONTHS);
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
    public void testCreateSettlementFillsRetentionForFinal() {
        System.setProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO, "0.05");
        System.setProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_DUE_MONTHS, "12");

        Long[] holder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedFullSetup("STL-RET-FILL");
            holder[0] = seedProjectWithBillingAndCost("PRJ-RET-FILL", "质保金填充项目");
            return null;
        });
        ormTemplate.runInSession(() -> pnlBiz.refreshPnl(holder[0], null, null, CTX));

        ErpPrjProjectSettlement settlement = ormTemplate.runInSession(session -> settlementBiz.createSettlement(holder[0],
                ErpPrjConstants.SETTLEMENT_TYPE_FINAL, CTX));

        // D1 选项 A：retentionAmount = finalRevenue × ratio（Billing 10000 × 0.05 = 500.0000，scale 4 HALF_UP）
        assertNotNull(settlement.getRetentionAmount(), "FINAL 结算应自动填充 retentionAmount");
        assertEquals(0, settlement.getRetentionAmount().compareTo(new BigDecimal("500.0000")),
                "retentionAmount = 10000 × 0.05 = 500.0000");
        // D1 选项 A：retentionDueDate = businessDate(2026-07-17 冻结时钟) + 12 月 = 2027-07-17
        assertEquals(LocalDate.of(2027, 7, 17), settlement.getRetentionDueDate(),
                "retentionDueDate = businessDate + 12 月");
    }

    @Test
    public void testCreateSettlementDoesNotFillForInterimAndClose() {
        System.setProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO, "0.05");

        Long[] interimHolder = new Long[1];
        Long[] closeHolder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedFullSetup("STL-RET-IC");
            interimHolder[0] = seedProjectWithBillingAndCost("PRJ-RET-IC-INTERIM", "阶段结算项目");
            closeHolder[0] = seedProjectWithBillingAndCost("PRJ-RET-IC-CLOSE", "关闭转固项目");
            return null;
        });
        ormTemplate.runInSession(() -> {
            pnlBiz.refreshPnl(interimHolder[0], null, null, CTX);
            pnlBiz.refreshPnl(closeHolder[0], null, null, CTX);
        });

        ErpPrjProjectSettlement interim = ormTemplate.runInSession(session -> settlementBiz.createSettlement(interimHolder[0],
                ErpPrjConstants.SETTLEMENT_TYPE_INTERIM, CTX));
        ErpPrjProjectSettlement close = ormTemplate.runInSession(session -> settlementBiz.createSettlement(closeHolder[0],
                ErpPrjConstants.SETTLEMENT_TYPE_CLOSE, CTX));

        assertTrue(interim.getRetentionAmount() == null || interim.getRetentionAmount().signum() == 0,
                "INTERIM 阶段结算不填质保金（无尾款留存语义，ORM defaultValue=0 非留存）");
        assertNull(interim.getRetentionDueDate(), "INTERIM 不填到期日");
        assertTrue(close.getRetentionAmount() == null || close.getRetentionAmount().signum() == 0,
                "CLOSE 自建转固非应收，不填质保金");
        assertNull(close.getRetentionDueDate(), "CLOSE 不填到期日");
    }

    @Test
    public void testReturnRetentionSuccess() {
        System.setProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO, "0.05");

        Long settlementId = seedApprovedPostedSettlement("STL-RET-OK", "PRJ-RET-OK", "质保金返还成功项目");
        // 手工覆盖路径（D1 保留）：把到期日改到过去（冻结时钟 2026-07-17），使到期条件成立
        // 注意：Nop ORM 自动脏检查，session 提交时 flush 持久化 MANAGED 实体的字段变更，无需（也不能）再 updateEntity。
        ormTemplate.runInSession(session -> {
            ErpPrjProjectSettlement s = daoProvider.daoFor(ErpPrjProjectSettlement.class).getEntityById(settlementId);
            s.setRetentionDueDate(LocalDate.of(2026, 7, 1));
            return null;
        });

        ErpPrjProjectSettlement returned = ormTemplate.runInSession(session -> settlementBiz.returnRetention(settlementId, CTX));
        assertNotNull(returned, "返还返回结算单");
        assertEquals(0, returned.getRetentionAmount().compareTo(new BigDecimal("500.0000")),
                "结算单质保金留存不变");

        // D3 选项 A：返还标记 = ErpFinVoucherBillR(billCode=结算单号#RETURN, businessType=PROJECT_SETTLEMENT)
        List<ErpFinVoucherBillR> links = findBillLinks(returned.getCode() + ErpPrjConstants.RETENTION_RETURN_BILL_SUFFIX);
        assertEquals(1, links.size(), "返还凭证回链恰 1 条");

        // D2：返还凭证行级断言——镜像对冲 借 2241 / 贷 1122，金额=retentionAmount，标 projectId
        Long voucherId = links.get(0).getVoucherId();
        List<ErpFinVoucherLine> lines = findVoucherLines(voucherId);
        assertTrue(hasLine(lines, "2241", ErpFinConstants.DC_DEBIT, new BigDecimal("500.0000")),
                "返还凭证含 借 2241 其他应付款-质保金 500");
        assertTrue(hasLine(lines, "1122", ErpFinConstants.DC_CREDIT, new BigDecimal("500.0000")),
                "返还凭证含 贷 1122 应收账款-质保金 500");
        assertEquals(2, lines.size(), "返还凭证恰 2 行（镜像对冲，无主结算腿）");
        assertTrue(lines.stream().allMatch(l -> l.getProjectId() != null
                        && l.getProjectId().equals(returned.getProjectId())),
                "返还凭证行均标 projectId 辅助核算");
    }

    @Test
    public void testReturnRetentionIdempotent() {
        System.setProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO, "0.05");

        Long settlementId = seedApprovedPostedSettlement("STL-RET-IDEM", "PRJ-RET-IDEM", "质保金幂等项目");
        // 手工覆盖路径（D1 保留）：把到期日改到过去，使到期条件成立（Nop 脏检查 + session flush 持久化）
        String[] codeHolder = new String[1];
        ormTemplate.runInSession(session -> {
            ErpPrjProjectSettlement s = daoProvider.daoFor(ErpPrjProjectSettlement.class).getEntityById(settlementId);
            s.setRetentionDueDate(LocalDate.of(2026, 7, 1));
            codeHolder[0] = s.getCode();
            return null;
        });
        String code = codeHolder[0];

        ormTemplate.runInSession(session -> settlementBiz.returnRetention(settlementId, CTX));
        List<ErpFinVoucherBillR> afterFirst = findBillLinks(
                code + ErpPrjConstants.RETENTION_RETURN_BILL_SUFFIX);
        assertEquals(1, afterFirst.size(), "首次返还生成 1 条回链");

        // 幂等：重复调用 no-op 零副作用（D3 选项 A——已返还重复调用非错误）
        ErpPrjProjectSettlement again = ormTemplate.runInSession(session -> settlementBiz.returnRetention(settlementId, CTX));
        assertNotNull(again, "幂等返回结算单");
        List<ErpFinVoucherBillR> afterSecond = findBillLinks(
                code + ErpPrjConstants.RETENTION_RETURN_BILL_SUFFIX);
        assertEquals(1, afterSecond.size(), "重复返还不新增凭证回链（no-op 零副作用）");
    }

    @Test
    public void testReturnRetentionGuardNotApproved() {
        System.setProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO, "0.05");

        Long settlementId = seedSettlement("STL-RET-G-APP", "PRJ-RET-G-APP", "守卫-未审批项目");
        // 保持 DRAFT/UNSUBMITTED，手工补质保金字段到可返还形态
        ormTemplate.runInSession(session -> {
            ErpPrjProjectSettlement s = daoProvider.daoFor(ErpPrjProjectSettlement.class).getEntityById(settlementId);
            s.setRetentionAmount(new BigDecimal("500.0000"));
            s.setRetentionDueDate(LocalDate.of(2026, 7, 1));
            return null;
        });

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> settlementBiz.returnRetention(settlementId, CTX)));
        assertEquals(ErpPrjErrors.ERR_RETENTION_RETURN_NOT_ALLOWED.getErrorCode(), ex.getErrorCode());
        assertNotNull(ex.getParam(ErpPrjErrors.ARG_REASON), "携带 reason 参数");
    }

    @Test
    public void testReturnRetentionGuardNotPosted() {
        System.setProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO, "0.05");

        Long settlementId = seedSettlement("STL-RET-G-POST", "PRJ-RET-G-POST", "守卫-未过账项目");
        // 构造 APPROVED 但 posted=false（过账失败隔离场景：留存凭证未生成不可返还）
        ormTemplate.runInSession(session -> {
            ErpPrjProjectSettlement s = daoProvider.daoFor(ErpPrjProjectSettlement.class).getEntityById(settlementId);
            s.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
            s.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
            s.setRetentionAmount(new BigDecimal("500.0000"));
            s.setRetentionDueDate(LocalDate.of(2026, 7, 1));
            return null;
        });

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> settlementBiz.returnRetention(settlementId, CTX)));
        assertEquals(ErpPrjErrors.ERR_RETENTION_RETURN_NOT_ALLOWED.getErrorCode(), ex.getErrorCode());
        assertNotNull(ex.getParam(ErpPrjErrors.ARG_REASON), "携带 reason 参数");
    }

    @Test
    public void testReturnRetentionGuardNotDue() {
        System.setProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO, "0.05");

        Long settlementId = seedApprovedPostedSettlement("STL-RET-G-DUE", "PRJ-RET-G-DUE", "守卫-未到期项目");
        // retentionDueDate 保持 createSettlement 推演值 2027-07-17（> 冻结时钟 2026-07-17）→ 未到期拒绝
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> settlementBiz.returnRetention(settlementId, CTX)));
        assertEquals(ErpPrjErrors.ERR_RETENTION_RETURN_NOT_ALLOWED.getErrorCode(), ex.getErrorCode());
        assertNotNull(ex.getParam(ErpPrjErrors.ARG_REASON), "携带 reason 参数");
    }

    @Test
    public void testReturnRetentionGuardZeroRetention() {
        // ratio 默认 0（设计性 opt-in）→ createSettlement 不填留存 → retentionAmount 空/0 → 无质保金拒绝
        Long settlementId = seedApprovedPostedSettlement("STL-RET-G-ZERO", "PRJ-RET-G-ZERO", "守卫-零质保金项目");
        ErpPrjProjectSettlement s = daoProvider.daoFor(ErpPrjProjectSettlement.class).getEntityById(settlementId);
        assertTrue(s.getRetentionAmount() == null || s.getRetentionAmount().signum() <= 0,
                "ratio=0 默认不填留存");

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> settlementBiz.returnRetention(settlementId, CTX)));
        assertEquals(ErpPrjErrors.ERR_RETENTION_RETURN_NOT_ALLOWED.getErrorCode(), ex.getErrorCode());
        assertNotNull(ex.getParam(ErpPrjErrors.ARG_REASON), "携带 reason 参数");
    }

    @Test
    public void testMainVoucherContainsRetentionLegs() {
        System.setProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO, "0.05");

        Long settlementId = seedApprovedPostedSettlement("STL-RET-MAIN", "PRJ-RET-MAIN", "主凭证留存腿项目");
        ErpPrjProjectSettlement s = daoProvider.daoFor(ErpPrjProjectSettlement.class).getEntityById(settlementId);
        assertTrue(Boolean.TRUE.equals(s.getPosted()), "已过账（留存凭证随主结算生成）");

        // D2 选项 A：留存腿在主结算凭证内（billHeadCode=结算单号，红冲自动覆盖）
        List<ErpFinVoucherBillR> links = findBillLinks(s.getCode());
        assertEquals(1, links.size(), "主结算凭证回链恰 1 条");
        List<ErpFinVoucherLine> lines = findVoucherLines(links.get(0).getVoucherId());

        assertTrue(hasLine(lines, "1122", ErpFinConstants.DC_DEBIT, new BigDecimal("500.0000")),
                "主结算凭证含 借 1122 应收账款-质保金 500");
        assertTrue(hasLine(lines, "2241", ErpFinConstants.DC_CREDIT, new BigDecimal("500.0000")),
                "主结算凭证含 贷 2241 其他应付款-质保金 500");
        assertTrue(lines.stream().allMatch(l -> l.getProjectId() != null
                        && l.getProjectId().equals(s.getProjectId())),
                "留存腿行标 projectId 辅助核算");

        // GL 平衡：Dr Σ = Cr Σ（借项目成本 6000 + 借本年利润 4000 + 借质保金 500 = 贷项目收入 10000 + 贷质保金 500）
        BigDecimal dr = lines.stream().map(l -> l.getDebitAmount() == null ? BigDecimal.ZERO : l.getDebitAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cr = lines.stream().map(l -> l.getCreditAmount() == null ? BigDecimal.ZERO : l.getCreditAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, dr.compareTo(cr), "主结算凭证借贷平衡");
    }

    @Test
    public void testReverseSettlementRejectedAfterReturn() {
        System.setProperty(ErpPrjConstants.CONFIG_SETTLEMENT_RETENTION_RATIO, "0.05");

        Long settlementId = seedApprovedPostedSettlement("STL-RET-REV", "PRJ-RET-REV", "红冲守卫项目");
        // 手工覆盖路径：把到期日改到过去，使到期条件成立（Nop 脏检查 + session flush 持久化）
        ormTemplate.runInSession(session -> {
            ErpPrjProjectSettlement s = daoProvider.daoFor(ErpPrjProjectSettlement.class).getEntityById(settlementId);
            s.setRetentionDueDate(LocalDate.of(2026, 7, 1));
            return null;
        });

        ormTemplate.runInSession(session -> settlementBiz.returnRetention(settlementId, CTX));

        // Explore ⑥：已返还后红冲主结算会悬挂独立返还凭证 → 拒绝
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> settlementBiz.reverseSettlement(settlementId, CTX)));
        assertEquals(ErpPrjErrors.ERR_RETENTION_RETURN_NOT_ALLOWED.getErrorCode(), ex.getErrorCode(),
                "已返还后 reverseSettlement 拒绝");
    }

    // ---------- seed & query helpers ----------

    /** 创建 FINAL 结算 + submit + approve（过账成功 posted=true），返回结算单 ID。 */
    private Long seedApprovedPostedSettlement(String tag, String projectCode, String projectName) {
        Long[] holder = new Long[1];
        Long[] settlementId = new Long[1];
        ormTemplate.runInSession(session -> {
            seedFullSetup(tag);
            holder[0] = seedProjectWithBillingAndCost(projectCode, projectName);
            return null;
        });
        ormTemplate.runInSession(() -> pnlBiz.refreshPnl(holder[0], null, null, CTX));
        ErpPrjProjectSettlement settlement = ormTemplate.runInSession(session -> settlementBiz.createSettlement(holder[0],
                ErpPrjConstants.SETTLEMENT_TYPE_FINAL, CTX));
        settlementId[0] = settlement.getId();
        ormTemplate.runInSession(() -> settlementBiz.submit(settlementId[0], CTX));
        ErpPrjProjectSettlement approved = ormTemplate.runInSession(session -> settlementBiz.approve(settlementId[0], CTX));
        assertEquals(ErpPrjConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());
        return settlementId[0];
    }

    /** 仅创建 FINAL 结算单（DRAFT/UNSUBMITTED/posted=false），不提交。 */
    private Long seedSettlement(String tag, String projectCode, String projectName) {
        Long[] holder = new Long[1];
        Long[] settlementId = new Long[1];
        ormTemplate.runInSession(session -> {
            seedFullSetup(tag);
            holder[0] = seedProjectWithBillingAndCost(projectCode, projectName);
            return null;
        });
        ormTemplate.runInSession(() -> pnlBiz.refreshPnl(holder[0], null, null, CTX));
        ErpPrjProjectSettlement settlement = ormTemplate.runInSession(session -> settlementBiz.createSettlement(holder[0],
                ErpPrjConstants.SETTLEMENT_TYPE_FINAL, CTX));
        settlementId[0] = settlement.getId();
        return settlementId[0];
    }

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
        // RC-R1.63 质保金科目（D2 残留风险：部署须预置 1122/2241，否则过账抛 ERR_SUBJECT_NOT_FOUND）
        seedSubject("1122", "应收账款-质保金");
        seedSubject("2241", "其他应付款-质保金");
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

    private List<ErpFinVoucherBillR> findBillLinks(String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", ErpFinBusinessType.PROJECT_SETTLEMENT.name()));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherLine> findVoucherLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q);
    }

    private boolean hasLine(List<ErpFinVoucherLine> lines, String subjectCode, String direction, BigDecimal amount) {
        return lines.stream().anyMatch(l -> subjectCode.equals(l.getSubjectCode())
                && direction.equals(l.getDcDirection())
                && amount.compareTo(direction.equals(ErpFinConstants.DC_DEBIT)
                ? (l.getDebitAmount() == null ? BigDecimal.ZERO : l.getDebitAmount())
                : (l.getCreditAmount() == null ? BigDecimal.ZERO : l.getCreditAmount())) == 0);
    }
}
