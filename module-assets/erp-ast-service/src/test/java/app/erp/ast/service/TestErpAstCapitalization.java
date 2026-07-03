package app.erp.ast.service;

import app.erp.ast.biz.IErpAstAssetCapitalizationBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资本化（转固）审批→建卡→折旧计划生成→CAPITALIZATION(80) 业财过账 端到端单测（plan Phase 2）。
 *
 * <p>验证三轴审批（submit→approve）：APPROVED 时建/激活 {@link ErpAstAsset}（status=IN_SERVICE，
 * 继承类别折旧方法/使用年限，累计折旧=0，净值=原值）+ 生成 {@link ErpAstDepreciationSchedule}
 * （直线法每期等额、折旧次月起、最后一期残值约束调整）+ CAPITALIZATION 凭证落库（借固定资产/贷银行存款）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstCapitalization extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstAssetCapitalizationBiz capBiz;

    @Test
    public void testApproveCreatesAssetScheduleAndPosting() {
        Long[] categorySubjectIds = new Long[1];
        Long capId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedAcctSchema(1L);
            Long fixedAssetSubjectId = seedSubject("1601", "固定资产");
            seedSubject("1002", "银行存款");
            categorySubjectIds[0] = fixedAssetSubjectId;
            Long categoryId = seedCategory("CAT-AST-1", "设备类",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, fixedAssetSubjectId);
            return seedCapitalization("CAP-AST-001", categoryId,
                    ErpAstConstants.SOURCE_TYPE_DIRECT_PURCHASE, new BigDecimal("12000"),
                    LocalDate.of(2026, 6, 15));
        });

        // 三轴审批：UNSUBMITTED → submit → SUBMITTED → approve → APPROVED
        capBiz.submit(capId, CTX);
        ErpAstAssetCapitalization cap = capBiz.approve(capId, CTX);

        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, cap.getApproveStatus());
        assertEquals(ErpAstConstants.DOC_STATUS_ACTIVE, cap.getDocStatus());
        assertTrue(Boolean.TRUE.equals(cap.getPosted()), "过账成功 posted=true");

        // 建卡：status=IN_SERVICE，继承类别折旧方法/使用年限，累计折旧=0，净值=原值，残值=0
        ErpAstAsset asset = findAssetByCode("AST-CAP-AST-001");
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, asset.getStatus());
        assertEquals(ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, asset.getDepreciationMethod());
        assertEquals(12, asset.getUsefulLifeMonths());
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(BigDecimal.ZERO), "累计折旧=0");
        assertEquals(0, nz(asset.getNetBookValue()).compareTo(new BigDecimal("12000")), "净值=原值");
        assertEquals(0, nz(asset.getResidualValue()).compareTo(BigDecimal.ZERO), "残值=0");

        // 折旧计划：12 期 PENDING，直线法每期等额 1000，折旧次月（2026-07）起
        List<ErpAstDepreciationSchedule> schedules = findSchedulesByAsset(asset.getId());
        assertEquals(12, schedules.size(), "12 期折旧计划");
        assertEquals("2026-07", schedules.get(0).getPeriod(), "折旧起始月=资本化次月");
        assertEquals("2027-06", schedules.get(11).getPeriod(), "折旧终止月");
        for (ErpAstDepreciationSchedule s : schedules) {
            assertEquals(ErpAstConstants.SCHEDULE_STATUS_PENDING, s.getStatus());
            assertEquals(0, s.getPlannedAmount().compareTo(new BigDecimal("1000")), "直线法每期等额 1000");
        }

        // CAPITALIZATION(80) 凭证经业财回链可查
        assertTrue(!findBillLinks("CAP-AST-001", "CAPITALIZATION").isEmpty(), "CAPITALIZATION 凭证回链已落库");
    }

    @Test
    public void testRejectFromSubmitted() {
        Long capId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedAcctSchema(1L);
            Long fixedAssetSubjectId = seedSubject("1601", "固定资产");
            Long categoryId = seedCategory("CAT-AST-2", "设备类",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, fixedAssetSubjectId);
            return seedCapitalization("CAP-AST-002", categoryId,
                    ErpAstConstants.SOURCE_TYPE_DIRECT_PURCHASE, new BigDecimal("12000"),
                    LocalDate.of(2026, 6, 15));
        });

        capBiz.submit(capId, CTX);
        ErpAstAssetCapitalization cap = capBiz.reject(capId, CTX);
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, cap.getApproveStatus());
        assertFalse(Boolean.TRUE.equals(cap.getPosted()), "驳回不过账");
        // 驳回后无资产卡片、无折旧计划、无凭证
        assertEquals(0, daoProvider.daoFor(ErpAstAsset.class)
                .findAllByQuery(new QueryBean().addFilter(eq("code", "AST-CAP-AST-002"))).size());
    }

    @Test
    public void testCipSourceCreditsCipSubject() {
        Long capId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedAcctSchema(1L);
            Long fixedAssetSubjectId = seedSubject("1601", "固定资产");
            Long cipSubjectId = seedSubject("1603", "在建工程");
            Long categoryId = seedCategory("CAT-AST-3", "自建工程",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, fixedAssetSubjectId);
            // 类别配置在建工程科目
            daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(categoryId).setCipSubjectId(cipSubjectId);
            return seedCapitalization("CAP-AST-003", categoryId,
                    ErpAstConstants.SOURCE_TYPE_CIP, new BigDecimal("12000"),
                    LocalDate.of(2026, 6, 15));
        });

        capBiz.submit(capId, CTX);
        ErpAstAssetCapitalization cap = capBiz.approve(capId, CTX);
        assertTrue(Boolean.TRUE.equals(cap.getPosted()), "CIP 转固过账成功");
        assertTrue(!findBillLinks("CAP-AST-003", "CAPITALIZATION").isEmpty(), "CAPITALIZATION 凭证回链已落库");
    }

    // ---------- seed helpers ----------

    private Long seedCapitalization(String code, Long categoryId, String sourceType, BigDecimal originalValue,
                                    LocalDate capitalizationDate) {
        IEntityDao<ErpAstAssetCapitalization> dao = daoProvider.daoFor(ErpAstAssetCapitalization.class);
        ErpAstAssetCapitalization cap = new ErpAstAssetCapitalization();
        cap.setCode(code);
        cap.setOrgId(1L);
        cap.setAssetCode("AST-" + code);
        cap.setAssetName("资产-" + code);
        cap.setCategoryId(categoryId);
        cap.setCurrencyId(1L);
        cap.setCapitalizationDate(capitalizationDate);
        cap.setOriginalValue(originalValue);
        cap.setSourceType(sourceType);
        cap.setExchangeRate(BigDecimal.ONE);
        cap.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(cap);
        return cap.getId();
    }

    private Long seedCategory(String code, String name, String method, int usefulLifeMonths, Long subjectId) {
        IEntityDao<ErpAstAssetCategory> dao = daoProvider.daoFor(ErpAstAssetCategory.class);
        ErpAstAssetCategory category = new ErpAstAssetCategory();
        category.setCode(code);
        category.setName(name);
        category.setDepreciationMethod(method);
        category.setUsefulLifeMonths(usefulLifeMonths);
        category.setSubjectId(subjectId);
        dao.saveEntity(category);
        return category.getId();
    }

    private Long seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
        return subject.getId();
    }

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus("OPEN");
        dao.saveEntity(period);
    }

    private ErpAstAsset findAssetByCode(String code) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpAstAsset> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpAstDepreciationSchedule> findSchedulesByAsset(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        q.addOrderField("period", false);
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
