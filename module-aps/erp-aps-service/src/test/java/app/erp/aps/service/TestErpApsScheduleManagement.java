package app.erp.aps.service;

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

import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsScheduleManagement extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    static final String HORIZON_START = "2026-07-10T00:00:00";
    static final String HORIZON_END = "2026-07-20T00:00:00";

    @Test
    public void testPublishDraftSucceeds() {
        Long scheduleId = createSchedule("S-PUB", "FORWARD");

        ApiResponse<?> resp = executeRpc(mutation, "ErpApsSchedule__publish",
                ApiRequest.build(Map.of("id", scheduleId)));

        assertEquals(0, resp.getStatus());
        assertEquals("PUBLISHED", ((Map<?, ?>) resp.getData()).get("status"));
    }

    @Test
    public void testArchiveFromPublishedSucceeds() {
        Long scheduleId = createSchedule("S-ARC-P", "FORWARD");

        ApiResponse<?> published = executeRpc(mutation, "ErpApsSchedule__publish",
                ApiRequest.build(Map.of("id", scheduleId)));
        assertEquals(0, published.getStatus());

        ApiResponse<?> archived = executeRpc(mutation, "ErpApsSchedule__archive",
                ApiRequest.build(Map.of("id", scheduleId)));
        assertEquals(0, archived.getStatus());
        assertEquals("ARCHIVED", ((Map<?, ?>) archived.getData()).get("status"));
    }

    @Test
    public void testArchiveFromDraftSucceeds() {
        Long scheduleId = createSchedule("S-ARC-D", "FORWARD");

        ApiResponse<?> archived = executeRpc(mutation, "ErpApsSchedule__archive",
                ApiRequest.build(Map.of("id", scheduleId)));

        assertEquals(0, archived.getStatus());
        assertEquals("ARCHIVED", ((Map<?, ?>) archived.getData()).get("status"));
    }

    @Test
    public void testPublishNonDraftFails() {
        Long scheduleId = createSchedule("S-PNF", "FORWARD");

        ApiResponse<?> published = executeRpc(mutation, "ErpApsSchedule__publish",
                ApiRequest.build(Map.of("id", scheduleId)));
        assertEquals(0, published.getStatus());

        ApiResponse<?> again = executeRpc(mutation, "ErpApsSchedule__publish",
                ApiRequest.build(Map.of("id", scheduleId)));
        assertTrue(again.getStatus() != 0, "PUBLISHED->PUBLISHED should fail");
    }

    @Test
    public void testArchiveAlreadyArchivedIsIdempotent() {
        Long scheduleId = createSchedule("S-AI", "FORWARD");

        ApiResponse<?> published = executeRpc(mutation, "ErpApsSchedule__publish",
                ApiRequest.build(Map.of("id", scheduleId)));
        assertEquals(0, published.getStatus());

        ApiResponse<?> archived = executeRpc(mutation, "ErpApsSchedule__archive",
                ApiRequest.build(Map.of("id", scheduleId)));
        assertEquals(0, archived.getStatus());
        assertEquals("ARCHIVED", ((Map<?, ?>) archived.getData()).get("status"));

        ApiResponse<?> again = executeRpc(mutation, "ErpApsSchedule__archive",
                ApiRequest.build(Map.of("id", scheduleId)));
        assertEquals(0, again.getStatus(), "archive ARCHIVED should be idempotent");
    }

    private Long createSchedule(String code, String mode) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("name", code);
        d.put("scheduleDate", "2026-07-10");
        d.put("schedulingMode", mode);
        d.put("horizonStart", HORIZON_START);
        d.put("horizonEnd", HORIZON_END);
        d.put("status", "DRAFT");
        ApiResponse<?> r = executeRpc(mutation, "ErpApsSchedule__save", ApiRequest.build(Map.of("data", d)));
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
