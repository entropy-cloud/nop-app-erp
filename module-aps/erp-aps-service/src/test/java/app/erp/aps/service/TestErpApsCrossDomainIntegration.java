package app.erp.aps.service;

import app.erp.aps.service.loadsource.ApsLoadSourceProvider;
import app.erp.mfg.biz.ApsLoadSlot;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsCrossDomainIntegration extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    ApsLoadSourceProvider loadSourceProvider;

    static final Long MACHINE_A = 100L;
    static final Long WORK_ORDER_1 = 2001L;
    static final Long WORK_ORDER_2 = 2002L;
    static final LocalDate PERIOD_FROM = LocalDate.of(2026, 7, 10);
    static final LocalDate PERIOD_TO = LocalDate.of(2026, 7, 20);

    @Test
    public void testLoadSourceProviderReturnsScheduledSlots() {
        createPlannedOp("OP-CD-1", WORK_ORDER_1, 10, MACHINE_A, 10,
                "0", "30", "1", "2026-07-12T09:00:00", "2026-07-12T10:00:00");

        List<ApsLoadSlot> slots = loadSourceProvider.findScheduledSlots(
                List.of(WORK_ORDER_1), PERIOD_FROM, PERIOD_TO);

        assertNotNull(slots);
        assertEquals(1, slots.size());
        ApsLoadSlot slot = slots.get(0);
        assertEquals(WORK_ORDER_1, slot.getWorkOrderId());
        assertEquals(MACHINE_A, slot.getWorkcenterId());
        assertNotNull(slot.getPlannedStartT());
        assertNotNull(slot.getPlannedEndT());
    }

    @Test
    public void testLoadSourceProviderExcludesNonPlannedOps() {
        createOp("OP-CD-DRAFT", WORK_ORDER_1, 10, MACHINE_A, 10,
                "0", "30", "1", "2026-07-12T09:00:00", "DRAFT");

        List<ApsLoadSlot> slots = loadSourceProvider.findScheduledSlots(
                List.of(WORK_ORDER_1), PERIOD_FROM, PERIOD_TO);

        assertNotNull(slots);
        assertTrue(slots.isEmpty());
    }

    @Test
    public void testLoadSourceProviderFiltersByWorkOrderId() {
        createPlannedOp("OP-CD-W1", WORK_ORDER_1, 10, MACHINE_A, 10,
                "0", "30", "1", "2026-07-12T09:00:00", "2026-07-12T10:00:00");
        createPlannedOp("OP-CD-W2", WORK_ORDER_2, 10, MACHINE_A, 10,
                "0", "30", "1", "2026-07-13T09:00:00", "2026-07-13T10:00:00");

        List<ApsLoadSlot> slots = loadSourceProvider.findScheduledSlots(
                List.of(WORK_ORDER_1), PERIOD_FROM, PERIOD_TO);

        assertNotNull(slots);
        assertEquals(1, slots.size());
        assertEquals(WORK_ORDER_1, slots.get(0).getWorkOrderId());
    }

    private Long createOp(String code, Long workOrderId, int sequence, Long machineId, int priority,
                          String setup, String perUnit, String qty, String earliestStart, String status) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("workOrderId", workOrderId);
        d.put("operationName", code);
        d.put("sequence", sequence);
        d.put("machineId", machineId);
        d.put("priority", priority);
        d.put("setupTime", new BigDecimal(setup));
        d.put("runtimePerUnit", new BigDecimal(perUnit));
        d.put("qty", new BigDecimal(qty));
        d.put("status", status);
        d.put("earliestStartDateT", earliestStart);
        ApiResponse<?> r = executeRpc(mutation, "ErpApsOperationOrder__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus());
        return idOf(r.getData());
    }

    private Long createPlannedOp(String code, Long workOrderId, int sequence, Long machineId, int priority,
                                 String setup, String perUnit, String qty,
                                 String plannedStart, String plannedEnd) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("workOrderId", workOrderId);
        d.put("operationName", code);
        d.put("sequence", sequence);
        d.put("machineId", machineId);
        d.put("priority", priority);
        d.put("setupTime", new BigDecimal(setup));
        d.put("runtimePerUnit", new BigDecimal(perUnit));
        d.put("qty", new BigDecimal(qty));
        d.put("status", "PLANNED");
        d.put("earliestStartDateT", plannedStart);
        d.put("plannedStartDateT", plannedStart);
        d.put("plannedEndDateT", plannedEnd);
        ApiResponse<?> r = executeRpc(mutation, "ErpApsOperationOrder__save", ApiRequest.build(Map.of("data", d)));
        assertEquals(0, r.getStatus());
        return idOf(r.getData());
    }

    private Long idOf(Object data) {
        Object id = ((Map<?, ?>) data).get("id");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return Long.parseLong(String.valueOf(id));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
