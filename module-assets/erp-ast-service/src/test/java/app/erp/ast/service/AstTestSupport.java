package app.erp.ast.service;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产折旧/处置端到端测试共享种子助手（程序化建数据，对齐 TestErpFinExpenseClaimPosting 范式）。
 */
final class AstTestSupport {

    private AstTestSupport() {
    }

    static Long seedSubject(IDaoProvider daoProvider, String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(10);
        subject.setDirection(10);
        subject.setStatus(10);
        dao.saveEntity(subject);
        return subject.getId();
    }

    static void seedAcctSchema(IDaoProvider daoProvider, long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature(10);
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus(10);
        dao.saveEntity(schema);
    }

    static void seedPeriod(IDaoProvider daoProvider, String code, int year, int month, int status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(LocalDate.of(year, month, 1));
        period.setEndDate(LocalDate.of(year, month, 1).withDayOfMonth(
                LocalDate.of(year, month, 1).lengthOfMonth()));
        period.setStatus(status);
        dao.saveEntity(period);
    }

    static Long seedCategory(IDaoProvider daoProvider, String code, String name, int method, int months,
                             Long subjectId, Long depreciationSubjectId, Long expenseSubjectId) {
        IEntityDao<ErpAstAssetCategory> dao = daoProvider.daoFor(ErpAstAssetCategory.class);
        ErpAstAssetCategory category = new ErpAstAssetCategory();
        category.setCode(code);
        category.setName(name);
        category.setDepreciationMethod(method);
        category.setUsefulLifeMonths(months);
        category.setSubjectId(subjectId);
        category.setDepreciationSubjectId(depreciationSubjectId);
        category.setExpenseSubjectId(expenseSubjectId);
        dao.saveEntity(category);
        return category.getId();
    }

    static Long seedAsset(IDaoProvider daoProvider, String code, String name, Long categoryId, long orgId,
                          BigDecimal originalValue, BigDecimal residualValue, int method, int months,
                          int status) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        ErpAstAsset asset = new ErpAstAsset();
        asset.setCode(code);
        asset.setName(name);
        asset.setOrgId(orgId);
        asset.setCategoryId(categoryId);
        asset.setAcquisitionDate(LocalDate.of(2026, 6, 1));
        asset.setCurrencyId(1L);
        asset.setOriginalValue(originalValue);
        asset.setCurrentValue(originalValue);
        asset.setResidualValue(residualValue);
        asset.setDepreciationMethod(method);
        asset.setUsefulLifeMonths(months);
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setNetBookValue(originalValue);
        asset.setStatus(status);
        dao.saveEntity(asset);
        return asset.getId();
    }

    static void seedPendingSchedule(IDaoProvider daoProvider, Long assetId, Long orgId, String period) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        ErpAstDepreciationSchedule s = new ErpAstDepreciationSchedule();
        s.setAssetId(assetId);
        s.setOrgId(orgId);
        s.setPeriod(period);
        s.setPlannedAmount(BigDecimal.ZERO);
        s.setStatus(ErpAstConstants.SCHEDULE_STATUS_PENDING);
        dao.saveEntity(s);
    }
}
