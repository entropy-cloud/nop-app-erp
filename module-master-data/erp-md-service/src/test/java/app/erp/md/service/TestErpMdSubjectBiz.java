package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * F7 §3 {@code ErpMdSubjectBizModel} 后端 @BizQuery 单元测试（plan 2026-07-20-1020-2 Phase 2）。
 *
 * <p>覆盖 {@code testIsCodeUnique}：新编码 true / 重复 false / excludeId 自身排除 true（3 用例）。
 *
 * <p>科目不新增 countReferences：会计语义上科目可停用不可删除（保留历史凭证完整性），
 * F1 已移除删除按钮，countReferences 无删除路径消费者，归 Non-Goal。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdSubjectBiz extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testIsCodeUnique() {
        Long subjectId = seedSubject("E2E-SUBJ-UNIQ-1");
        // 1. 新编码（无冲突）→ true
        Boolean fresh = (Boolean) rpcData(GraphQLOperationType.query, "ErpMdSubject__isCodeUnique",
                Map.of("code", "E2E-SUBJ-UNIQ-NEW"));
        assertEquals(Boolean.TRUE, fresh, "无冲突编码应返回 true");

        // 2. 重复编码（已被占用）→ false
        Boolean dup = (Boolean) rpcData(GraphQLOperationType.query, "ErpMdSubject__isCodeUnique",
                Map.of("code", "E2E-SUBJ-UNIQ-1"));
        assertEquals(Boolean.FALSE, dup, "已存在编码应返回 false");

        // 3. excludeId 自身排除 → true（edit 模式保留原 code 不应误判）
        Boolean self = (Boolean) rpcData(GraphQLOperationType.query, "ErpMdSubject__isCodeUnique",
                Map.of("code", "E2E-SUBJ-UNIQ-1", "excludeId", subjectId));
        assertEquals(Boolean.TRUE, self, "excludeId 排除自身后应返回 true");
    }

    // ---------- helpers ----------

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Object rpcData(GraphQLOperationType opType, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(opType, action, ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), action + " 应成功，实际 code=" + resp.getCode());
        return resp.getData();
    }

    private Long seedSubject(String code) {
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName("E2E-" + code);
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus("ACTIVE");
        subject.setIsLeaf(Boolean.TRUE);
        ormTemplate.runInSession(() -> subjectDao().saveEntity(subject));
        return subject.getId();
    }

    private IEntityDao<ErpMdSubject> subjectDao() {
        return daoProvider.daoFor(ErpMdSubject.class);
    }
}
