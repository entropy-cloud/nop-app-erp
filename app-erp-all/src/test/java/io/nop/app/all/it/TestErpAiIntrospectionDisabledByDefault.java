package io.nop.app.all.it;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.graphql.GraphQLRequestBean;
import io.nop.api.core.beans.graphql.GraphQLResponseBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E3.6 最小落地集 item 1 反向断言：introspection 默认关闭（`ai-native-interface.md` 前置调研结论
 * ——应用 {@code application.yaml} 显式 false）——`__` 前缀操作被拒且错误码可断言。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAiIntrospectionDisabledByDefault extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testIntrospectionRejectedByDefault() {
        GraphQLRequestBean request = new GraphQLRequestBean();
        request.setQuery("{__schema{queryType{name}}}");
        // 默认关闭时引擎在 selection 解析阶段直接抛 introspection-not-enabled（非响应信封）
        io.nop.api.core.exceptions.NopException ex = org.junit.jupiter.api.Assertions.assertThrows(
                io.nop.api.core.exceptions.NopException.class,
                () -> graphQLEngine.executeGraphQL(graphQLEngine.newGraphQLContext(request)));
        assertTrue(String.valueOf(ex.getErrorCode()).contains("introspection"),
                "错误码应指向 introspection 未启用: " + ex.getErrorCode());
    }
}
