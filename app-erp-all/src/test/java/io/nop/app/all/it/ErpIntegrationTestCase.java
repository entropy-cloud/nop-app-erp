package io.nop.app.all.it;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvStockMove;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.ioc.BeanContainer;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmEntity;
import io.nop.orm.initialize.DataInitInitializer;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 系统级黄金路径集成测试基类（M0.1 设计文档机制 (c)「抑制 tableInit 的文件 H2 双模方案」）。
 *
 * <p>机制要点（与 {@code docs/design/integration-testing.md §3.2/§3.3} 对应，M0.2 试点已逐项实证）：
 * <ul>
 *   <li><b>DB 恒为文件 H2</b>（{@code application.yaml} {@code jdbc:h2:./db/erp}）：类级
 *       {@code @NopTestConfig(localDb = false)} 抑制 {@code NopTestConfigProcessor} 的内存库 URL 覆盖；
 *       子类 override {@link #configExecutionMode} 强制两模式 {@code setLocalDb(false)}+{@code setTableInit(false)}，
 *       CHECKING 不再强制 in-memory / RECORDING 不再走内存库，平台 {@code AutoTestCaseDataBaseInitializer}
 *       不建表、不装载 input/tables。</li>
 *   <li><b>前置态 = 全量部署 seed</b>：NopJunitExtension 以 ALL_LAZY 启动容器（schema 由 force-init
 *       的 {@code DataBaseSchemaInitializer} 幂等创建）；{@code DataInitInitializer} 为惰性 bean
 *       （无 force-init），本类在 per-method {@code container.restart()} 之后显式触发其 @PostConstruct，
 *       以部署同源 loader 装载 94 CSV + 1 SQL（拓扑序，门控 {@code nop.orm.init-database-data=true}
 *       须经子类 {@code @NopTestProperty} 在容器启动前声明）。</li>
 *   <li><b>fresh-DB 每类 1 次</b>：首个测试方法的 {@code initBeans} 删除 {@code db/erp.mv.db/.trace.db}
 *       后 restart（文件 H2 重建空库）+ 装载 seed；同 JVM 后续类/方法复用该前置态，restart 保留文件库
 *       （per-method restart × 文件 H2 兼容性 = M0.2 待证风险 ③，已实证成立）。</li>
 * </ul>
 *
 * <p>1 用例 = 1 测试类 1 测试方法（roadmap M0.2 粒度约定）；多步 id 传递：ERP 实体主键
 * {@code tagSet="seq-default"} 不被快照框架自动注册为 {@code @var} 变量（仅 {@code var} 标签列如
 * {@code code} 自动注册，M0.2 实证），故链步骤从响应提取 id 后 {@link #addVar} 供后续 request 文件引用
 * （对齐 {@code TestErpMdPartnerCrudSmoke} 先例）。
 *
 * <p>共享 step helper 集（M0.2 产物，供 B1-Bn 复用；对标 Playwright {@code _helper.ts} 先例）：
 * {@link #rpcMutation} / {@link #idOf} / {@link #submitForApproval} / {@link #approve} /
 * {@link #reload} / {@link #findBillLink} / {@link #requireVoucherBalanced} /
 * {@link #findApItem} / {@link #findStockMove}。
 */
@NopTestConfig(localDb = false, initDatabaseSchema = OptionalBoolean.TRUE)
public abstract class ErpIntegrationTestCase extends JunitAutoTestCase {

    private static Class<?> freshDbForClass;

    @Override
    protected void initBeans() {
        boolean needFreshAndSeed = freshDbForClass != getClass();
        if (needFreshAndSeed) {
            freshDbForClass = getClass();
            deleteDbFiles();
        }
        super.initBeans();
        if (needFreshAndSeed) {
            // ALL_LAZY 下 DataInitInitializer 惰性，显式实例化触发 @PostConstruct 装载全量部署 seed。
            // 每类恰 1 次：restart 不重建文件库，seed 落库后跨 restart 保留。
            BeanContainer.getBeanByType(DataInitInitializer.class);
        }
    }

    private static void deleteDbFiles() {
        for (String suffix : new String[]{".mv.db", ".trace.db"}) {
            new File("db/erp" + suffix).delete();
        }
    }

    @Override
    protected void configExecutionMode(TestInfo testInfo) {
        super.configExecutionMode(testInfo);
        // 机制 (c)：双模均抑制 localDb/tableInit —— 回放态 DB = 文件 H2 全量部署 seed。
        setLocalDb(false);
        setTableInit(false);
    }

    // ---------- 共享 step helper 集（M0.2，供 B1-Bn 复用） ----------

    /** GraphQL RPC 执行（BizModel 动作一律经 IGraphQLEngine，禁绕过）。 */
    protected ApiResponse<?> rpcMutation(String action, ApiRequest<?> request) {
        return executeRpc(mutation, action, request);
    }

    protected ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = getGraphQLEngine().newRpcContext(opType, action, request);
        return getGraphQLEngine().executeRpc(ctx);
    }

    private IGraphQLEngine getGraphQLEngine() {
        return BeanContainer.getBeanByType(IGraphQLEngine.class);
    }

    private IDaoProvider getDaoProvider() {
        return BeanContainer.getBeanByType(IDaoProvider.class);
    }

    /** 从 save/action 响应提取实体主键（ERP id 非 @var 自动注册，须显式传递）。 */
    protected String idOf(ApiResponse<?> response) {
        assertNotNull(response.getData(), "响应 data 必须非空");
        return String.valueOf(((Map<?, ?>) response.getData()).get("id"));
    }

    /** DIRECT 审批轴：submitForApproval（{@code ErpXxx__submitForApproval}）。 */
    protected ApiResponse<?> submitForApproval(String entityName, String id) {
        return rpcMutation(entityName + "__submitForApproval", ApiRequest.build(Map.of("id", id)));
    }

    /** DIRECT 审批轴：approve（{@code ErpXxx__approve}）。 */
    protected ApiResponse<?> approve(String entityName, String id) {
        return rpcMutation(entityName + "__approve", ApiRequest.build(Map.of("id", id)));
    }

    protected <T extends IOrmEntity> T reload(Class<T> clazz, String id) {
        return getDaoProvider().daoFor(clazz).getEntityById(id);
    }

    /** 业财回链反查（billCode → ErpFinVoucherBillR）。 */
    protected ErpFinVoucherBillR findBillLink(String billCode) {
        List<ErpFinVoucherBillR> links = getDaoProvider().daoFor(ErpFinVoucherBillR.class)
                .findAllByQuery(new QueryBean());
        return links.stream().filter(l -> billCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    /** 断言凭证存在且借贷平衡（金额 = expectedTotal），返回凭证。 */
    protected ErpFinVoucher requireVoucherBalanced(ErpFinVoucherBillR link, BigDecimal expectedTotal, String label) {
        assertNotNull(link, label + " 应生成业财回链");
        ErpFinVoucher voucher = getDaoProvider().daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, label + " 凭证应落库");
        assertEquals(0, voucher.getTotalDebit().compareTo(expectedTotal), label + " 借方合计");
        assertEquals(0, voucher.getTotalCredit().compareTo(expectedTotal), label + " 贷方合计");
        return voucher;
    }

    /** AR/AP 辅助账反查（sourceBillType + sourceBillCode）。 */
    protected ErpFinArApItem findApItem(String sourceBillType, String sourceBillCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillType", sourceBillType));
        q.addFilter(eq("sourceBillCode", sourceBillCode));
        List<ErpFinArApItem> list = getDaoProvider().daoFor(ErpFinArApItem.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 库存移动单反查（relatedBillType + relatedBillCode）。 */
    protected ErpInvStockMove findStockMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = getDaoProvider().daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected static QueryBean eqFilterQuery(String propName, Object value) {
        QueryBean q = new QueryBean();
        q.addFilter(eq(propName, value));
        return q;
    }
}