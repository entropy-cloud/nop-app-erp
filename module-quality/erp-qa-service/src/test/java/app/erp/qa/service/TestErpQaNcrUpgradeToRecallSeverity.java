package app.erp.qa.service;

import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaRecall;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * P2-CK-qa-026-r3 负路径回归：NCR→召回升级路径 severity 字典污染。
 *
 * <p>缺陷面：{@code ErpQaNonConformanceUpgradeToRecallProcessor} 将 NCR severity=NORMAL
 * 直写 recall.severityLevel，而 {@code erp-qa/recall-severity} 字典值域为 LOW/MEDIUM/HIGH/CRITICAL
 * （无 NORMAL）——非法字典值污染召回单 UI 映射与报表聚合。
 *
 * <p>断言契约（索引修复方向）：NORMAL 显式映射 MEDIUM；合法 severity（LOW/HIGH/CRITICAL）直通保持。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaNcrUpgradeToRecallSeverity extends JunitAutoTestCase {

    @RegisterExtension
    static QaFrozenClockExtension frozenClock = new QaFrozenClockExtension();

    static final String MATERIAL_ID = "28201";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    /** 负路径：severity=NORMAL 不得直写召回，须映射为 recall-severity 字典合法值 MEDIUM。 */
    @Test
    public void normalSeverityMappedToMediumNotPoisoned() {
        String ncrId = seedNcr(30401L, "NCR-SEV-NORMAL", ErpQaConstants.NCR_SEVERITY_NORMAL);

        String recallId = upgradeToRecallAndGetId(ncrId);
        ErpQaRecall recall = daoProvider.daoFor(ErpQaRecall.class).getEntityById(recallId);

        assertEquals(ErpQaConstants.RECALL_SEVERITY_MEDIUM, recall.getSeverityLevel(),
                "NORMAL 须映射为 recall-severity 字典合法值 MEDIUM");
        assertNotEquals(ErpQaConstants.NCR_SEVERITY_NORMAL, recall.getSeverityLevel(),
                "非法字典值 NORMAL 不得落入 recall.severityLevel");
    }

    /** 控制组：合法 severity（LOW/HIGH/CRITICAL）直通行为保持。 */
    @Test
    public void legalSeveritiesPassThrough() {
        assertPassThrough(30411L, "NCR-SEV-LOW", ErpQaConstants.NCR_SEVERITY_LOW, ErpQaConstants.RECALL_SEVERITY_LOW);
        assertPassThrough(30412L, "NCR-SEV-HIGH", ErpQaConstants.NCR_SEVERITY_HIGH, ErpQaConstants.RECALL_SEVERITY_HIGH);
        assertPassThrough(30413L, "NCR-SEV-CRIT", ErpQaConstants.NCR_SEVERITY_CRITICAL, ErpQaConstants.RECALL_SEVERITY_CRITICAL);
    }

    private void assertPassThrough(long id, String code, String ncrSeverity, String expectedRecallSeverity) {
        String ncrId = seedNcr(id, code, ncrSeverity);
        String recallId = upgradeToRecallAndGetId(ncrId);
        ErpQaRecall recall = daoProvider.daoFor(ErpQaRecall.class).getEntityById(recallId);
        assertEquals(expectedRecallSeverity, recall.getSeverityLevel(),
                "合法 severity " + ncrSeverity + " 应直通");
    }

    // ---------- helpers ----------

    private String seedNcr(long id, String code, String severity) {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
            ErpQaNonConformance ncr = new ErpQaNonConformance();
            ncr.orm_propValueByName("id", String.valueOf(id));
            ncr.setCode(code);
            ncr.setNcrDate(CoreMetrics.currentDate());
            ncr.setMaterialId(MATERIAL_ID);
            ncr.setSeverity(severity);
            ncr.setStatus(ErpQaConstants.NCR_STATUS_IN_REVIEW);
            ncr.setSourceType(ErpQaConstants.NCR_SOURCE_TYPE_INSPECTION);
            ncr.setSourceCode("INS-" + code);
            ncr.setDescription("severity 映射回归 NCR");
            dao.saveEntity(ncr);
            return null;
        });
        return String.valueOf(id);
    }

    private String upgradeToRecallAndGetId(String ncrId) {
        rpcOk(mutation, "ErpQaNonConformance__upgradeToRecall", Map.of("ncrId", ncrId));
        return recallIdByCode("RC-FROM-NCR-" + ncrId);
    }

    private String recallIdByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpQaRecall> list = daoProvider.daoFor(ErpQaRecall.class).findAllByQuery(q);
        assertEquals(1, list.size(), "应存在 1 条召回 " + code);
        return list.get(0).getId();
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), action + " 应成功，但返回: " + resp);
    }
}
