package app.erp.common.test;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import io.nop.orm.support.DynamicOrmEntity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * seq-string 行为 Proof（id-string-migration M0.1 Phase 3，载体 (a)：module-common-test 测试专用 orm）。
 *
 * <p>对应平台 {@code orm-model-design.md §主键设计方案 B}：BIGINT 列 + {@code stdDataType="string"} +
 * {@code tagSet="seq-default"} 的三断言：
 * <ul>
 *   <li>断言 1（防空证）：无显式 id 保存 → id 为 {@code String} 且非空（instanceof String，非仅非空）；</li>
 *   <li>断言 2（coercion 防空证）：显式数字 id（{@code 5L} 与 {@code "5"}）保存 → 存活且为 {@code String "5"}；</li>
 *   <li>断言 3（FK 形态列）：BIGINT + string 的 FK 形态列 refId 值经保存/读取往返保持 String。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:app/erp/common/test/seq-proof-test.yaml")
public class TestSeqStringIdProof extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testSeqGeneratedIdIsNonEmptyString() {
        final Object[] idBox = new Object[1];
        ormTemplate.runInSession(() -> {
            IEntityDao<DynamicOrmEntity> dao = dao();
            DynamicOrmEntity e = dao.newEntity();
            e.prop_set("code", "SEQ-PROOF-1");
            dao.saveEntity(e);

            Object id = e.orm_id();
            idBox[0] = id;
            assertTrue(id instanceof String, "seq-default 生成 id 必须是 String，实际: " + (id == null ? "null" : id.getClass().getName()));
            assertFalse(((String) id).isEmpty(), "生成 id 非空");

        });
        ormTemplate.runInSession(() -> {
            DynamicOrmEntity reloaded = findByCode(dao(), "SEQ-PROOF-1");
            assertTrue(reloaded.orm_id() instanceof String, "落库回读 id 必须是 String");
            assertEquals(idBox[0], reloaded.orm_id(), "回读 id 与生成 id 一致");
        });
    }

    @Test
    public void testExplicitNumericLongIdCoercedToString() {
        ormTemplate.runInSession(() -> {
            IEntityDao<DynamicOrmEntity> dao = dao();
            DynamicOrmEntity e = dao.newEntity();
            e.prop_set("code", "SEQ-PROOF-L5");
            e.orm_propValue(1, 5L);
            dao.saveEntity(e);

            assertTrue(e.orm_id() instanceof String, "显式 Long 5L 保存后 id 必须是 String");
            assertEquals("5", e.orm_id(), "显式 5L 保存后 id 值为字符串 \"5\"");

        });
        ormTemplate.runInSession(() -> {
            DynamicOrmEntity reloaded = findByCode(dao(), "SEQ-PROOF-L5");
            assertEquals("5", reloaded.orm_id(), "显式 5L 落库回读为 \"5\"");
        });
    }

    @Test
    public void testExplicitNumericStringIdSurvives() {
        ormTemplate.runInSession(() -> {
            IEntityDao<DynamicOrmEntity> dao = dao();
            DynamicOrmEntity e = dao.newEntity();
            e.prop_set("code", "SEQ-PROOF-S7");
            e.prop_set("id", "7");
            dao.saveEntity(e);

            assertEquals("7", e.orm_id(), "显式 \"7\" 保存后存活且为 String");

        });
        ormTemplate.runInSession(() -> {
            DynamicOrmEntity reloaded = findByCode(dao(), "SEQ-PROOF-S7");
            assertEquals("7", reloaded.orm_id(), "显式 \"7\" 落库回读为 \"7\"");
        });
    }

    @Test
    public void testFkShapedBigIntStringColumnRoundTrip() {
        ormTemplate.runInSession(() -> {
            IEntityDao<DynamicOrmEntity> dao = dao();
            DynamicOrmEntity e = dao.newEntity();
            e.prop_set("code", "SEQ-PROOF-FK");
            e.prop_set("refId", "12345");
            dao.saveEntity(e);

        });
        ormTemplate.runInSession(() -> {
            DynamicOrmEntity reloaded = findByCode(dao(), "SEQ-PROOF-FK");
            Object refId = reloaded.prop_get("refId");
            assertTrue(refId instanceof String, "BIGINT+string FK 形态列值必须是 String，实际: " + (refId == null ? "null" : refId.getClass().getName()));
            assertEquals("12345", refId);
        });
    }

    @SuppressWarnings("unchecked")
    private IEntityDao<DynamicOrmEntity> dao() {
        return (IEntityDao<DynamicOrmEntity>) (IEntityDao<?>) daoProvider.dao("ErpTstSeqProof");
    }

    private DynamicOrmEntity findByCode(IEntityDao<DynamicOrmEntity> dao, String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        return dao.findAllByQuery(q).get(0);
    }
}
