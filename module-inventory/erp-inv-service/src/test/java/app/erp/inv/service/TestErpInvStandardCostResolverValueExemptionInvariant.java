package app.erp.inv.service;

import app.erp.inv.service.costing.StandardCostResolver;
import io.nop.core.unittest.BaseTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E3.2 防回归守卫（plan 2026-08-10-0739-2 / Q4 取值豁免）：断言 {@link StandardCostResolver} 始终为
 * 非 BizModel 直 DAO 消费者——@Inject 字段类型集合不含任何 user-context 类型（破坏则标准成本解析
 * 取值豁免前提失效，见 {@code docs/design/finance/costing-methods.md §成本卷算取值豁免边界}）。
 *
 * <p><b>禁止类型集合（硬失败）</b>：
 * <ul>
 *   <li>{@code io.nop.api.core.context.IContext}</li>
 *   <li>{@code io.nop.api.core.auth.IUserContext}（注：plan 原文误作
 *       {@code io.nop.api.core.context.IUserContext}，实际平台 FQN 在 {@code auth} 包，此处用真实 FQN
 *       使守卫可拦截真实注入——执行期裁决修正）</li>
 * </ul>
 *
 * <p><b>文档化期望集（tripwire 注释，非硬失败）</b>：StandardCostResolver 当前 @Inject 类型 =
 * {@code {IDaoProvider, IOrmTemplate}}。未来新增<b>非 user-context</b> 的合法 @Inject 不触发硬失败，
 * 仅需同步更新此注释；仅当新增 user-context 类型时硬失败拦截。
 *
 * <p><b>守卫范围注记</b>：本守卫覆盖 @Inject 字段注入这一最可能违规向量；不覆盖方法参数 / ThreadLocal /
 * IOrmTemplate session-context 等非常规向量（StandardCostResolver 类级 Javadoc 文本约束更宽，守卫为
 * 最佳effort 拦截）。
 */
public class TestErpInvStandardCostResolverValueExemptionInvariant extends BaseTestCase {

    private static final Set<String> FORBIDDEN_FQNS = Set.of(
            "io.nop.api.core.context.IContext",
            "io.nop.api.core.auth.IUserContext"
    );

    @Test
    public void noUserContextInjection() {
        Set<String> injectFieldFqns = collectInjectFieldTypeFqns(StandardCostResolver.class);

        Set<String> violations = new LinkedHashSet<>();
        for (String fqn : injectFieldFqns) {
            if (FORBIDDEN_FQNS.contains(fqn)) {
                violations.add(fqn);
            }
        }
        assertTrue(violations.isEmpty(),
                "StandardCostResolver 架构不变量（E3.2）被破坏：检测到禁止的 user-context @Inject 类型 = "
                        + violations + "。引入 IContext/IUserContext 会破坏 Q4 取值豁免前提"
                        + "（见 costing-methods.md §成本卷算取值豁免边界）。当前 @Inject 类型集合 = "
                        + injectFieldFqns);
    }

    @Test
    public void documentedExpectedSetHolds() {
        Set<String> injectFieldFqns = collectInjectFieldTypeFqns(StandardCostResolver.class);
        Set<String> expected = Stream.of(
                IDaoProvider.class.getName(), IOrmTemplate.class.getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertTrue(injectFieldFqns.equals(expected),
                "StandardCostResolver @Inject 期望集（tripwire）漂移：期望 = " + expected
                        + "，实际 = " + injectFieldFqns
                        + "。若为新增非 user-context 合法注入，请同步更新本测试 tripwire 注释 + "
                        + "StandardCostResolver 类 Javadoc；若为 user-context 注入，上一个测试会硬失败。");
    }

    private static Set<String> collectInjectFieldTypeFqns(Class<?> clazz) {
        Set<String> fqns = new LinkedHashSet<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(Inject.class)) {
                fqns.add(f.getType().getName());
            }
        }
        assertFalse(fqns.isEmpty(),
                "StandardCostResolver 应至少含一个 @Inject 字段（IDaoProvider），实际为零——类结构异常");
        return fqns;
    }
}
