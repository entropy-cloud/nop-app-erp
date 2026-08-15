package io.nop.app.all.seed;

import io.nop.api.core.beans.FilterBeans;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.ioc.BeanContainer;
import io.nop.commons.util.StringHelper;
import io.nop.core.initialize.CoreInitialization;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.unittest.BaseTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmEntity;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.model.IEntityJoinConditionModel;
import io.nop.orm.model.IEntityModel;
import io.nop.orm.model.IEntityRelationModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通用种子数据引用完整性测试（plan 2026-08-15-2000-1）。
 *
 * <p>两个通用校验：
 * <ul>
 *   <li>{@link #testAllSeedTablesLoadable()}——枚举 {@code IDaoProvider.getEntityNames()}（全量含平台实体，
 *       Phase 1 Decision (a)：418 = app.erp.* 352 + 平台 66），逐实体 {@code findAll()} 不抛异常；
 *       存在 seed CSV 的表行数 &gt; 0（镜像 {@code DataInitInitializer.loadCsvData} 的 CSV 查找逻辑）。</li>
 *   <li>{@link #testNonNullRelationKeysPointToExistingRows()}——逐实体经
 *       {@code getEntityModel().getRelations()} 取全部 <b>to-one</b> 关系，逐行取 join leftProp 值，
 *       非空时校验 refEntity 存在。主键 join（全仓实证 100% 主键 join）refEntity 主键集一次性加载为
 *       Set 内存比对；{@code isJoinOnNonPkColumn()} 的关系按 refProp 值经 refEntity 列查询
 *       （Phase 1 Decision (a) 语义精确；实证当前零命中）。to-many 为反向关系不重复校验。</li>
 * </ul>
 *
 * <p>白名单豁免机制：{@link #WHITELIST_KEYS} 三元组 (ownerEntity, relationName, key) 常量表。
 * Phase 1 初扫 722 个非空 FK 值零悬空，当前白名单为空；未来 seed 追加引入合法弱指针/占位引用时在此登记。
 * 占位软引用（如 spc_chart.parameterId=0，ORM 无 {@code <to-one>}）天然跳过无需登记。
 *
 * <p>采用 {@code BaseTestCase} + 手动 {@code CoreInitialization.initialize()}（镜像
 * {@code TestAuthSeedLoadingProof}），因 NopJunitExtension ALL_LAZY 模式下
 * {@code DataBaseSchemaInitializer} 的 @PostConstruct 不先于 DB 访问 bean 运行（pre-existing 仓库行为）。
 */
public class TestErpSeedDataIntegrity extends BaseTestCase {

    static final String INIT_DATA_LOCATION = "/_init-data/";

    /**
     * 白名单豁免表：(ownerEntity, relationName, key) 三元组。
     * Phase 1 初扫（722 非空 FK 值）零悬空 → 当前为空；每项豁免必须注明证据来源（seed CSV 注释 /
     * seed-data.md 注记 / bug 记录）。
     */
    static final Set<String> WHITELIST_KEYS = Set.of();

    static String whitelistKey(String ownerEntity, String relationName, String key) {
        return ownerEntity + "|" + relationName + "|" + key;
    }

    @BeforeAll
    public static void initialize() {
        // 独立 H2 文件（jdbc:h2:./db/erp-integrity）：surefire forkCount=1C + parallel=classes 下
        // 本类与 TestAuthSeedLoadingProof 可能在并行 JVM 同时打开 db/erp.mv.db 导致文件锁冲突，
        // 故用系统属性（优先级高于 application.yaml，setTestConfig 无法覆盖 datasource）指向独立文件。
        System.setProperty("nop.datasource.jdbc-url", "jdbc:h2:./db/erp-integrity");
        for (String suffix : new String[]{".mv.db", ".trace.db"}) {
            new java.io.File("db/erp-integrity" + suffix).delete();
        }
        setTestConfig("nop.orm.init-database-schema", true);
        setTestConfig("nop.orm.init-database-data", true);
        setTestConfig("nop.orm.init-database-data-location", INIT_DATA_LOCATION);
        CoreInitialization.initialize();
    }

    @AfterAll
    public static void destroy() {
        CoreInitialization.destroy();
        // 还原系统属性，避免泄漏给同 JVM 后续测试类（如 TestAuthSeedLoadingProof 的 db/erp 路径）。
        System.clearProperty("nop.datasource.jdbc-url");
    }

    @SuppressWarnings("unchecked")
    private IEntityDao<IOrmEntity> daoForTable(String tableName) {
        return BeanContainer.getBeanByType(IDaoProvider.class).daoForTable(tableName);
    }

    @SuppressWarnings("unchecked")
    private IEntityDao<IOrmEntity> daoForEntity(String entityName) {
        return BeanContainer.getBeanByType(IDaoProvider.class).dao(entityName);
    }

    @Test
    public void testAllSeedTablesLoadable() {
        IDaoProvider daoProvider = BeanContainer.getBeanByType(IDaoProvider.class);
        Set<String> entityNames = new TreeSet<>(daoProvider.getEntityNames());
        assertFalse(entityNames.isEmpty(), "getEntityNames 必须非空");
        assertTrue(entityNames.stream().anyMatch(n -> n.startsWith("app.erp.")),
                "getEntityNames 必须含 app.erp.* 前缀实体");

        // 逐实体 findAll() 不抛异常（含平台实体，Phase 1 Decision (a)）
        List<String> loadFailures = new ArrayList<>();
        for (String entityName : entityNames) {
            try {
                daoForEntity(entityName).findAll();
            } catch (Exception e) {
                loadFailures.add(entityName + ": " + e);
            }
        }
        assertTrue(loadFailures.isEmpty(),
                "以下实体 findAll() 抛异常:\n" + String.join("\n", loadFailures));

        // 存在 seed CSV 的表必须行数 > 0（镜像 DataInitInitializer.loadCsvData 的 CSV 查找逻辑）
        List<String> emptyTables = new ArrayList<>();
        for (String entityName : entityNames) {
            IEntityModel model = ((IOrmEntityDao<?>) daoForEntity(entityName)).getEntityModel();
            String csvPath = StringHelper.appendPath(INIT_DATA_LOCATION, model.getTableName() + ".csv");
            if (ResourceHelper.resolve(csvPath).exists()) {
                if (daoForEntity(entityName).findAll().isEmpty()) {
                    emptyTables.add(entityName + " (" + model.getTableName() + ".csv)");
                }
            }
        }
        assertTrue(emptyTables.isEmpty(),
                "以下有 seed CSV 的表行数为 0（悬空 seed 文件或加载失败）:\n" + String.join("\n", emptyTables));
    }

    @Test
    public void testNonNullRelationKeysPointToExistingRows() {
        IDaoProvider daoProvider = BeanContainer.getBeanByType(IDaoProvider.class);
        List<String> failures = new ArrayList<>();
        int checked = 0;

        for (String entityName : new TreeSet<>(daoProvider.getEntityNames())) {
            IEntityDao<IOrmEntity> dao = daoForEntity(entityName);
            IEntityModel model = ((IOrmEntityDao<?>) dao).getEntityModel();
            List<IOrmEntity> rows = dao.findAll();

            for (IEntityRelationModel rel : model.getRelations()) {
                if (!rel.getKind().isToOneRelation())
                    continue; // to-many 为反向关系，对端 to-one 已覆盖
                if (rel.isDynamicJoin())
                    continue; // 计算属性/alias join 无实体列可校验，实证零命中

                List<? extends IEntityJoinConditionModel> joins = rel.getJoin();
                if (joins.size() != 1) {
                    failures.add(entityName + "." + rel.getName() + " 复合 join 暂不支持（当前 0 命中）");
                    continue;
                }
                String leftProp = joins.get(0).getLeftProp();
                String rightProp = joins.get(0).getRightProp();
                String refEntityName = rel.getRefEntityName();
                IEntityDao<IOrmEntity> refDao = daoForEntity(refEntityName);

                if (!rel.isJoinOnNonPkColumn()) {
                    // 主键 join：refEntity 主键集一次性加载为 Set 内存比对（Phase 1 Decision (a)）
                    Set<String> refKeys = new HashSet<>();
                    for (IOrmEntity refRow : refDao.findAll()) {
                        Object v = refRow.orm_propValueByName(rightProp);
                        if (v != null)
                            refKeys.add(String.valueOf(v));
                    }
                    for (IOrmEntity row : rows) {
                        Object v = row.orm_propValueByName(leftProp);
                        if (v == null)
                            continue;
                        checked++;
                        if (!refKeys.contains(String.valueOf(v)) && !isWhitelisted(entityName, rel.getName(), v)) {
                            failures.add(entityName + "." + leftProp + "=" + v
                                    + " 悬空（rel " + rel.getName() + " -> " + refEntityName + "." + rightProp + "）");
                        }
                    }
                } else {
                    // 非主键 join：按 refProp 值查询 refEntity 对应列（语义精确）
                    for (IOrmEntity row : rows) {
                        Object v = row.orm_propValueByName(leftProp);
                        if (v == null)
                            continue;
                        checked++;
                        QueryBean query = new QueryBean();
                        query.addFilter(FilterBeans.eq(rightProp, v));
                        if (!refDao.existsByQuery(query) && !isWhitelisted(entityName, rel.getName(), v)) {
                            failures.add(entityName + "." + leftProp + "=" + v
                                    + " 悬空（非主键 join rel " + rel.getName() + " -> " + refEntityName + "." + rightProp + "）");
                        }
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(),
                "发现 " + failures.size() + " 个悬空引用（共检查 " + checked + " 个非空关联键）:\n"
                        + String.join("\n", failures));
    }

    private boolean isWhitelisted(String ownerEntity, String relationName, Object key) {
        return WHITELIST_KEYS.contains(whitelistKey(ownerEntity, relationName, String.valueOf(key)));
    }
}
