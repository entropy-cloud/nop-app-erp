package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import app.erp.crm.dao.entity.ErpCrmStage;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * stageId 单向递增守卫测试（R1.24 / P1-MA2-075）— allow-backward 模式
 * （{@code erp-crm.allow-stage-backward}=true，LOG.warn 放行）。
 *
 * <p>覆盖：config=true 时回退放行（LOG.warn）+ convLog 写入（保留审计留痕）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:allow-stage-backward-test.yaml")
public class TestErpCrmStageDirectionGuardAllowBackward extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long STAGE_LOW = 5201L;   // sequence=20
    static final Long STAGE_HIGH = 5202L;  // sequence=30

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testBackwardMoveAllowedWithConvLog() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_LOW, "STG-LOW2", "早期", 20, 10);
            seedStage(STAGE_HIGH, "STG-HIGH2", "后期", 30, 60);
            seedLead(5301L, "LEAD-ALLOW-BACK-001", ErpCrmConstants.DOC_STATUS_QUALIFIED, STAGE_HIGH);
        });
        // 回退 HIGH(seq=30) → LOW(seq=20)：allow-backward=true 放行（LOG.warn）
        assertEquals(0, moveStage(5301L, STAGE_LOW).getStatus(), "allow-backward=true 时回退应放行");
        ErpCrmLead moved = reloadLead(5301L);
        assertEquals(STAGE_LOW, moved.getStageId(), "stageId 回退到 LOW");
        // convLog 审计留痕（保留审计不丢）
        List<ErpCrmLeadConvLog> logs = loadConvLogs(5301L);
        assertFalse(logs.isEmpty(), "回退放行仍写 convLog 审计行");
        assertEquals(STAGE_HIGH, logs.get(0).getFromStageId(), "convLog fromStageId=HIGH");
        assertEquals(STAGE_LOW, logs.get(0).getToStageId(), "convLog toStageId=LOW");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> moveStage(Long leadId, Long toStageId) {
        return graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(mutation, "ErpCrmLead__moveStage",
                        ApiRequest.build(Map.of("leadId", leadId, "toStageId", toStageId))));
    }

    // ---------- seed helpers ----------

    private void seedStage(Long id, String code, String name, int sequence, int defaultProbability) {
        IEntityDao<ErpCrmStage> dao = daoProvider.daoFor(ErpCrmStage.class);
        ErpCrmStage stage = new ErpCrmStage();
        stage.setId(id);
        stage.setCode(code);
        stage.setStageName(name);
        stage.setSequence(sequence);
        stage.setDefaultProbability(defaultProbability);
        dao.saveEntity(stage);
    }

    private void seedLead(Long id, String code, String docStatus, Long stageId) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(ORG_ID);
        lead.setLeadType(ErpCrmConstants.LEAD_TYPE_LEAD);
        lead.setDocStatus(docStatus);
        lead.setStageId(stageId);
        lead.setContactName("联系人" + id);
        daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
    }

    // ---------- reload helpers ----------

    private ErpCrmLead reloadLead(Long id) {
        return daoProvider.daoFor(ErpCrmLead.class).getEntityById(id);
    }

    private List<ErpCrmLeadConvLog> loadConvLogs(Long leadId) {
        IEntityDao<ErpCrmLeadConvLog> dao = daoProvider.daoFor(ErpCrmLeadConvLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        return dao.findAllByQuery(q);
    }
}
