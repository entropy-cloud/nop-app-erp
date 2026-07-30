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
 * stageId 单向递增守卫测试（R1.24 / P1-MA2-075）— STRICT 默认（{@code erp-crm.allow-stage-backward}=false）。
 *
 * <p>覆盖：(1) 回退抛 {@code ERR_STAGE_BACKWARD_MOVE}；(2) 前移成功 + convLog 写入（行为不变）；
 * (3) fromStageId=null（首次入漏斗）跳过方向校验成功。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmStageDirectionGuard extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long STAGE_LOW = 5101L;   // sequence=20
    static final Long STAGE_HIGH = 5102L;  // sequence=30

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testBackwardMoveRejectedByDefault() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_LOW, "STG-LOW", "早期", 20, 10);
            seedStage(STAGE_HIGH, "STG-HIGH", "后期", 30, 60);
            // QUALIFIED 线索已处于高阶段
            seedLead(5001L, "LEAD-BACK-001", ErpCrmConstants.DOC_STATUS_QUALIFIED, STAGE_HIGH);
        });
        // 回退 HIGH(seq=30) → LOW(seq=20)：STRICT 默认拦截
        ApiResponse<?> bad = moveStage(5001L, STAGE_LOW);
        assertEquals(ErpCrmErrors.ERR_STAGE_BACKWARD_MOVE.getErrorCode(), bad.getCode(),
                "STRICT 默认阶段回退 → ERR_STAGE_BACKWARD_MOVE");
        // stageId 未变
        assertEquals(STAGE_HIGH, reloadLead(5001L).getStageId(), "拒绝后 stageId 保持 HIGH");
    }

    @Test
    public void testForwardMoveSucceedsWithConvLog() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_LOW, "STG-LOW", "早期", 20, 10);
            seedStage(STAGE_HIGH, "STG-HIGH", "后期", 30, 60);
            seedLead(5002L, "LEAD-FWD-001", ErpCrmConstants.DOC_STATUS_QUALIFIED, STAGE_LOW);
        });
        // 前移 LOW(seq=20) → HIGH(seq=30)：成功（行为不变）
        assertEquals(0, moveStage(5002L, STAGE_HIGH).getStatus(), "前移应成功");
        ErpCrmLead moved = reloadLead(5002L);
        assertEquals(STAGE_HIGH, moved.getStageId(), "stageId 前移到 HIGH");
        List<ErpCrmLeadConvLog> logs = loadConvLogs(5002L);
        assertFalse(logs.isEmpty(), "前移写入 convLog 审计行");
        assertEquals(STAGE_LOW, logs.get(0).getFromStageId(), "convLog fromStageId=LOW");
        assertEquals(STAGE_HIGH, logs.get(0).getToStageId(), "convLog toStageId=HIGH");
    }

    @Test
    public void testFirstFunnelEntrySkipsDirectionCheck() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_LOW, "STG-LOW", "早期", 20, 10);
            // QUALIFIED 线索 stageId=null（首次入漏斗）
            seedLead(5003L, "LEAD-FIRST-001", ErpCrmConstants.DOC_STATUS_QUALIFIED, null);
        });
        // fromStageId=null 跳过方向校验：无论目标阶段 sequence 如何都应成功
        assertEquals(0, moveStage(5003L, STAGE_LOW).getStatus(), "首次入漏斗（fromStageId=null）应跳过方向校验");
        assertEquals(STAGE_LOW, reloadLead(5003L).getStageId(), "首次入漏斗设 stageId=LOW");
    }

    @Test
    public void testEqualSequenceForwardSucceeds() {
        // 同 sequence 的不同阶段视为非回退（toSeq 不小于 fromSeq），应放行。
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_LOW, "STG-LOW", "早期", 20, 10);
            seedStage(STAGE_HIGH, "STG-HIGH", "后期", 20, 60);
            seedLead(5004L, "LEAD-EQ-001", ErpCrmConstants.DOC_STATUS_QUALIFIED, STAGE_LOW);
        });
        assertEquals(0, moveStage(5004L, STAGE_HIGH).getStatus(), "同 sequence 非回退应成功");
        assertEquals(STAGE_HIGH, reloadLead(5004L).getStageId(), "stageId 移到 HIGH");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> moveStage(Long leadId, Long toStageId) {
        return rpc(mutation, "ErpCrmLead__moveStage", Map.of("leadId", leadId, "toStageId", toStageId));
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, Map<String, Object> data) {
        return graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(opType, action, ApiRequest.build(data)));
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
