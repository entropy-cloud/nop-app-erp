package app.erp.inv.service;

import app.erp.inv.service.costing.StandardCostResolver;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.auth.IUserContext;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * E3.2 取值豁免保径 Proof（plan 2026-08-25-1956-1 Phase 3）：mfg {@code findLatestFirmedStandardCost}
 * @BizQuery 读取面脱敏落地后，inv {@link StandardCostResolver} 自有 DAO 直读路径继续取**原始** FIRMED 成本
 * （架构注释见 {@code StandardCostResolver.java}「架构不变量（E3.2 / Q4 取值豁免）」）。
 *
 * <p>对照设计：本测试在**非授权用户上下文**（无成本面角色）下断言 resolver 返回原始值——即便用户上下文存在
 * 且在 GraphQL 面会被 mask，DAO 直读也不经 BizModel 边界（对齐
 * {@code TestErpInvStandardCostResolverValueExemptionInvariant} 反射守卫 + 本测试行为守卫双层防护）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvResolverRawValueAfterReadPathMasking extends JunitAutoTestCase {

    private static final String MATERIAL_ID = "3401";
    private static final String HEADER_ID = "340101";
    private static final String LINE_ID = "340102";
    private static final BigDecimal UNIT_COST = new BigDecimal("12.34");

    @Inject
    StandardCostResolver standardCostResolver;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    private IUserContext prevCtx;

    @BeforeEach
    void saveContext() {
        prevCtx = IUserContext.get();
    }

    @AfterEach
    void restoreContext() {
        IUserContext.set(prevCtx);
    }

    @Test
    public void resolverReturnsRawFirmedCostUnderUnauthorizedContext() {
        seedFirmedRollup();
        loginAs("STAFF");

        BigDecimal resolved = standardCostResolver.resolve(MATERIAL_ID);
        assertEquals(0, UNIT_COST.compareTo(resolved),
                "E3.2 豁免保径：非授权上下文下 resolver DAO 直读仍取原始 FIRMED 成本（读取面 masking 不影响服务端取值）");
    }

    @Test
    public void resolverReturnsRawFirmedCostUnderNoContext() {
        seedFirmedRollup();
        IUserContext.set(null);

        BigDecimal resolved = standardCostResolver.resolve(MATERIAL_ID);
        assertEquals(0, UNIT_COST.compareTo(resolved),
                "E3.2 豁免保径：无用户上下文（GraphQL 面 fail-closed null）下 resolver 仍取原始值");
    }

    // ---------- helpers ----------

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("inv-resolver-exempt-test");
        ctx.setUserName("inv-resolver-exempt-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private void seedFirmedRollup() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
            ErpMfgCostRollup header = headerDao.newEntity();
            header.orm_propValueByName("id", HEADER_ID);
            header.setCode("ROLLUP-" + HEADER_ID);
            header.setOrgId("1");
            header.setBusinessDate(LocalDate.of(2026, 6, 1));
            header.orm_propValueByName("status", "FIRMED");
            headerDao.saveEntity(header);

            IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
            ErpMfgCostRollupLine line = lineDao.newEntity();
            line.orm_propValueByName("id", LINE_ID);
            line.setCostRollupId(HEADER_ID);
            line.setLineNo(1);
            line.setMaterialId(MATERIAL_ID);
            line.setUoMId("1");
            line.setUnitCost(UNIT_COST);
            line.setTotalCost(UNIT_COST);
            line.setMaterialCost(UNIT_COST);
            line.setCurrencyId("1");
            lineDao.saveEntity(line);
        });
    }
}
