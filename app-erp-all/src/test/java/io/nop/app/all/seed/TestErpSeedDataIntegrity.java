package io.nop.app.all.seed;

import io.nop.api.core.beans.FilterBeans;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.ioc.BeanContainer;
import io.nop.commons.util.StringHelper;
import io.nop.core.initialize.CoreInitialization;
import io.nop.core.resource.IResource;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.VirtualFileSystem;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通用种子数据引用完整性测试（plan 2026-08-15-2000-1）。
 *
 * <p>两个通用校验：
 * <ul>
 *   <li>{@link #testAllSeedTablesLoadable()}——枚举 {@code IDaoProvider.getEntityNames()}（动态全量含平台实体，
 *       随 ORM 演进自动扩展；2026-09-01 快照 = app.erp.* 363 + 平台 66；计数口径权威登记处 =
 *       {@code docs/architecture/seed-data.md}「全量化裁决」段对账表，漂移 &gt; 0 时先更新对账表再消费——
 *       M0.1 残留风险条款更新协议），逐实体 {@code findAll()} 不抛异常；
 *       存在 seed CSV 的表行数 &gt; 0（镜像 {@code DataInitInitializer.loadCsvData} 的 CSV 查找逻辑）。</li>
 *   <li>{@link #testNonNullRelationKeysPointToExistingRows()}——逐实体经
 *       {@code getEntityModel().getRelations()} 取全部 <b>to-one</b> 关系，逐行取 join leftProp 值，
 *       非空时校验 refEntity 存在。主键 join（全仓实证 100% 主键 join）refEntity 主键集一次性加载为
 *       Set 内存比对；{@code isJoinOnNonPkColumn()} 的关系按 refProp 值经 refEntity 列查询
 *       （Phase 1 Decision (a) 语义精确；实证当前零命中）。to-many 为反向关系不重复校验。</li>
 * </ul>
 *
 * <p>M0.2 门禁强化（plan 2026-09-01-0527-1，按 M0.1 裁决口径锚定）新增两断言：
 * <ul>
 *   <li>{@link #testAdjudicatedScopePinned()}——app.erp.* 实体计数锚定 M0.1 裁决快照常量
 *       {@link #EXPECTED_APP_ERP_ENTITY_COUNT}（scope-pinning，防 ORM 演进口径漂移静默通过；M0.1 目标集
 *       口径 = className 唯一计数 363）+ 已知模型声明缺 className 属性实体集锚定
 *       （{@link #EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES}，2026-09-01 实仓核实 finance 域 5 个，
 *       运行时实体集 = 363 + 5）+ sys_* 语义实体（{@code ErpSysNotification}/
 *       {@code ErpSysNotificationRead}/{@code ErpSysConfig}）在 app.erp.* 集内（M0.1 计入裁决）；
 *       平台实体（{@code NopAuthUser}）在动态 findAll 全量语义内但不计入 363 目标集常量（M0.1 排除裁决）。</li>
 *   <li>{@link #testSeedAssetInventoryBaselines()}——零孤儿 CSV（{@code _init-data/} 每个 {@code .csv} ↔
 *       已知实体 tableName 精确匹配；{@code erp_md_uom}/{@code erp_md_uom_conversion} 软缩写即真实表名，
 *       精确匹配天然覆盖）+ app.erp.* 与平台 CSV 基线常量（{@link #EXPECTED_APP_ERP_CSV_COUNT} 368（M1.5 起，
 *       批次沿革见常量 javadoc）+ {@link #EXPECTED_PLATFORM_CSV_COUNT} 4，基点 = plan 1143-1「97 CSV」实证快照）。</li>
 * </ul>
 *
 * <p>白名单豁免机制核验注记（M0.2 Proof，2026-09-01）：{@link #WHITELIST_KEYS} 三元组 (ownerEntity,
 * relationName, key) 登记纪律 + 每项豁免注明证据来源要求 + 占位软引用（ORM 无 {@code <to-one>} 关系，如
 * {@code spc_chart.parameterId=0}）天然跳过语义，经与 M0.1 裁决对账均满足 roadmap M0.2「白名单豁免常量表」
 * 需求，机制无需扩展（建成于 plan 2000-1；本注记为核验结论，非豁免登记处）。
 *
 * <p>采用 {@code BaseTestCase} + 手动 {@code CoreInitialization.initialize()}（镜像
 * {@code TestAuthSeedLoadingProof}），因 NopJunitExtension ALL_LAZY 模式下
 * {@code DataBaseSchemaInitializer} 的 @PostConstruct 不先于 DB 访问 bean 运行（pre-existing 仓库行为）。
 */
public class TestErpSeedDataIntegrity extends BaseTestCase {

    static final String INIT_DATA_LOCATION = "/_init-data/";

    /**
     * sys_* 语义 app.erp.* 实体（M0.1 分项裁决：计入 363 目标集）。
     */
    static final String ENTITY_ERP_SYS_NOTIFICATION = "app.erp.notify.dao.entity.ErpSysNotification";
    static final String ENTITY_ERP_SYS_NOTIFICATION_READ = "app.erp.notify.dao.entity.ErpSysNotificationRead";
    static final String ENTITY_ERP_SYS_CONFIG = "app.erp.md.dao.entity.ErpSysConfig";

    /**
     * 平台实体代表（M0.1 排除裁决：在动态 findAll 全量语义内，不计入 363 目标集常量）。
     */
    static final String ENTITY_PLATFORM_NOP_AUTH_USER = "io.nop.auth.dao.entity.NopAuthUser";

    /**
     * 实体模型声明缺 className 属性的 app.erp.* 实体（2026-09-01 实仓核实：finance 域 5 个手写实体）。
     *
     * <p>这些实体定义完整（列/关系/物理表/生成 Java 类均在，codegen 与运行时按 entity name 默认补齐
     * className），但 {@code rg 'className="app\.erp\.'} 口径天然遗漏它们——M0.1 裁决 363 与「270 缺 seed
     * 规格表」按该口径编制，未收录此 5 个（它们也无 seed CSV，实际运行时 app.erp.* 实体集 = 363 + 5）。
     * 新增同类实体时按更新协议先对账 seed-data.md 再扩充本集合；模型补齐 className 属性后同步移除。
     */
    static final Set<String> EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES = Set.of(
            "app.erp.fin.dao.entity.ErpFinCashForecast",
            "app.erp.fin.dao.entity.ErpFinCreditFacility",
            "app.erp.fin.dao.entity.ErpFinNotesDiscount",
            "app.erp.fin.dao.entity.ErpFinNotesPayable",
            "app.erp.fin.dao.entity.ErpFinNotesReceivable");

    /**
     * app.erp.* 实体唯一计数快照常量（M0.1 裁决口径，2026-09-01 实仓复算）。
     *
     * <p>更新协议（M0.1 残留风险条款，禁止跳步）：ORM 演进后先重跑
     * {@code rg -o 'className="app\.erp\.[^"]+"' module-&#42;/model/*.orm.xml --no-filename | sort -u | wc -l}
     * 取唯一计数 → 更新 {@code docs/architecture/seed-data.md}「全量化裁决」段对账表 → 再改本常量；
     * 禁止跳过对账表直接改常量。
     */
    static final int EXPECTED_APP_ERP_ENTITY_COUNT = 363;

    /**
     * app.erp.* 实体 seed CSV 基线常量（plan 1143-1「97 CSV」实证基线快照的 app.erp.* 分项）。
     *
     * <p>M1.x 每批 seed 落地后的随批更新协议：更新本常量与 {@link #EXPECTED_PLATFORM_CSV_COUNT} +
     * 同步 {@code docs/architecture/seed-data.md}「全量化裁决」段对账表 + 在该批 plan 中登记。
     *
     * <p>批次沿革：93（1143-1 基线）→ 105（M1.1a 批次 plan 2026-09-01-0838-1，master-data 4 + sales 8，
     * 对账表已同步 2026-09-01）→ 121（M1.1b 批次 plan 2026-09-01-0838-2，inventory 16，对账表已同步 2026-09-01）
     * → 133（M1.1c 批次 plan 2026-09-01-0838-3，purchase 12，对账表已同步 2026-09-01）
     * → 164（M1.2a1 批次 plan 2026-09-01-1245-1，finance 26 规格表 + 5 运行时补充实体，对账表已同步 2026-09-01）
     * → 190（M1.2a2 批次 plan 2026-09-01-1245-2，manufacturing 26 规格表，对账表已同步 2026-09-01）
     * → 218（M1.2b 批次 plan 2026-09-01-1245-3，maintenance 7 + quality 10 + projects 11，对账表已同步 2026-09-01）
      * → 237（M1.2c 批次 plan 2026-09-01-2255-1，assets 17 + notify 2，对账表已同步 2026-09-02）
       * → 269（M1.3 批次 plan 2026-09-01-2255-2，hr 32，对账表已同步 2026-09-02）
       * → 314（M1.4a 批次 plan 2026-09-01-2255-3，crm 30 + cs 15，对账表已同步 2026-09-02）
       * → 329（M1.4b 批次 plan 2026-09-02-1415-1，aps 7 + logistics 8，对账表已同步 2026-09-02）
       * → 368（M1.5 批次 plan 2026-09-02-1415-2，b2b 13 + contract 15 + drp 11，对账表已同步 2026-09-02；
       *   M1 全量覆盖闭环：有 seed 368 = 运行时口径 363 + finance 5 补充档全覆盖）。
        */
    static final int EXPECTED_APP_ERP_CSV_COUNT = 368;

    /**
     * 平台实体 seed CSV 基线常量（{@code nop_auth_user}/{@code nop_auth_user_role}/{@code nop_auth_role}/
     * {@code nop_sys_code_rule} 共 4；更新协议同 {@link #EXPECTED_APP_ERP_CSV_COUNT}）。
     */
    static final int EXPECTED_PLATFORM_CSV_COUNT = 4;

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
        // 同 JVM 前序 NopJunitExtension 测试类遗留动态配置（in-memory datasource URL / ALL_LAZY
        // 容器启动模式等），其 reset() 仅在其自身 beforeAll 时执行。宿主模式依赖 DEFAULT 启动下
        // DataInitInitializer 等非 lazy bean 的 @PostConstruct 先于 DB 访问运行。先 reset 清遗留，
        // 再显式回置 ALL_EAGER，保证串行 fork（app-erp-all surefire forkCount=1）下 seed 仍被装载。
        io.nop.api.core.config.AppConfig.getConfigProvider().reset();
        setTestConfig(io.nop.ioc.IocConfigs.CFG_IOC_APP_BEANS_CONTAINER_START_MODE,
                io.nop.api.core.ioc.BeanContainerStartMode.ALL_EAGER.name());
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

    /**
     * M0.2 裁决口径 scope-pinning：门禁锚定 app.erp.* 实体计数与 M0.1 裁决快照常量一致，
     * ORM 增删实体时计数漂移显式失败而非静默通过。
     *
     * <p>M0.1 目标集口径 = className 唯一计数（363）。运行时 getEntityNames() 实测还含 5 个模型声明
     * 缺 className 属性的实体（grep 口径遗漏项，见 {@link #EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES}；
     * 运行时 className 按 name 默认补齐，无法经 {@code getClassName()} 区分），故运行时计数断言 =
     * 363 + 已知遗漏集，双向漂移均显式失败。
     */
    @Test
    public void testAdjudicatedScopePinned() {
        IDaoProvider daoProvider = BeanContainer.getBeanByType(IDaoProvider.class);
        Set<String> entityNames = new TreeSet<>(daoProvider.getEntityNames());

        Set<String> appErpEntities = new TreeSet<>();
        for (String entityName : entityNames) {
            if (entityName.startsWith("app.erp."))
                appErpEntities.add(entityName);
        }

        assertEquals(EXPECTED_APP_ERP_ENTITY_COUNT + EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES.size(),
                appErpEntities.size(),
                "app.erp.* 运行时实体计数与 M0.1 裁决快照常量（363）+ 已知声明缺 className 集合（5）漂移。"
                        + "更新协议（M0.1 残留风险条款，禁止跳步）：先重跑 rg -o 'className=\"app\\.erp\\.[^\"]+\"' "
                        + "module-*/model/*.orm.xml --no-filename | sort -u | wc -l 取唯一计数"
                        + " → 更新 docs/architecture/seed-data.md「全量化裁决」段对账表 → 再改 EXPECTED_APP_ERP_ENTITY_COUNT；"
                        + "若为新增声明缺 className 实体，同时扩充 EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES");
        assertTrue(appErpEntities.containsAll(EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES),
                "已知声明缺 className 属性的实体须在运行时实体集内");

        // M0.1 计入裁决：sys_* 语义 className 实体在 363 目标集内
        assertTrue(appErpEntities.contains(ENTITY_ERP_SYS_NOTIFICATION),
                ENTITY_ERP_SYS_NOTIFICATION + " 必须在 app.erp.* 裁决目标集内（M0.1 计入）");
        assertTrue(appErpEntities.contains(ENTITY_ERP_SYS_NOTIFICATION_READ),
                ENTITY_ERP_SYS_NOTIFICATION_READ + " 必须在 app.erp.* 裁决目标集内（M0.1 计入）");
        assertTrue(appErpEntities.contains(ENTITY_ERP_SYS_CONFIG),
                ENTITY_ERP_SYS_CONFIG + " 必须在 app.erp.* 裁决目标集内（M0.1 计入）");

        // M0.1 排除裁决：平台实体在动态 findAll 全量语义内，但不计入 363 目标集常量
        assertTrue(entityNames.contains(ENTITY_PLATFORM_NOP_AUTH_USER),
                ENTITY_PLATFORM_NOP_AUTH_USER + " 必须在动态 findAll 全量语义内（平台 66 表随 ORM 演进全量枚举）");
        assertFalse(appErpEntities.contains(ENTITY_PLATFORM_NOP_AUTH_USER),
                ENTITY_PLATFORM_NOP_AUTH_USER + " 属平台资产管理边界，不计入 app.erp.* 目标集（M0.1 排除）");
    }

    /**
     * M0.2 seed 资产清单断言：零孤儿 CSV（无对应实体表的 CSV 是 M1.x 新增命名错误的典型形态）
     * + app.erp.* 与平台 CSV 基线常量对账（对齐 plan 1143-1「97 CSV」实证基线，为 M1.x 每批落地提供对账基准）。
     */
    @Test
    public void testSeedAssetInventoryBaselines() {
        IDaoProvider daoProvider = BeanContainer.getBeanByType(IDaoProvider.class);

        // 已知实体 tableName 集（表名以 ORM 实体定义为唯一权威；分类按实体名前缀，erp_sys_* 等
        // sys 语义表归属由 className 决定而非文件名前缀）
        Map<String, String> tableNameToEntity = new HashMap<>();
        for (String entityName : daoProvider.getEntityNames()) {
            IEntityModel model = ((IOrmEntityDao<?>) daoForEntity(entityName)).getEntityModel();
            tableNameToEntity.putIfAbsent(model.getTableName(), entityName);
        }

        List<IResource> csvFiles = new ArrayList<>();
        List<String> orphanCsvs = new ArrayList<>();
        int appErpCsvCount = 0;
        int platformCsvCount = 0;
        for (IResource child : VirtualFileSystem.instance().getChildren(INIT_DATA_LOCATION)) {
            String fileName = child.getName();
            if (!fileName.endsWith(".csv"))
                continue; // zz-sequence-advance.sql 等非 CSV 资产不在本断言范围
            csvFiles.add(child);
            String tableName = fileName.substring(0, fileName.length() - ".csv".length());
            String entityName = tableNameToEntity.get(tableName);
            if (entityName == null) {
                orphanCsvs.add(fileName);
            } else if (entityName.startsWith("app.erp.")) {
                appErpCsvCount++;
            } else {
                platformCsvCount++;
            }
        }
        assertFalse(csvFiles.isEmpty(), "_init-data/ 必须存在 seed CSV 资产");
        assertTrue(orphanCsvs.isEmpty(),
                "发现孤儿 CSV（_init-data/ 下无对应实体表的 CSV，M1.x 新增 CSV 命名错误典型形态）:\n"
                        + String.join("\n", orphanCsvs));

        assertEquals(EXPECTED_APP_ERP_CSV_COUNT, appErpCsvCount,
                "app.erp.* CSV 基线漂移：按常量 javadoc 随批更新协议对账后更新常量 + 同步 seed-data.md 对账表 + 该批 plan 登记");
        assertEquals(EXPECTED_PLATFORM_CSV_COUNT, platformCsvCount,
                "平台 CSV 基线漂移：按常量 javadoc 随批更新协议对账后更新常量 + 同步 seed-data.md 对账表 + 该批 plan 登记");
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
