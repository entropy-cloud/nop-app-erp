package app.erp.common.service;

import io.nop.orm.IOrmEntity;
import io.nop.orm.model.IEntityModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * F1.3：ErpCrudStatusLock 纯函数分支（无实体测试基建的 common-service 内可测部分）——
 * null 实体与无 ORM 模型实体恒放行；带列实体的 posted/APPROVED 分支由
 * erp-pur-service TestErpPurCrudStatusLock 实体级证明（M1 修订分工）。
 */
public class TestErpCrudStatusLock {

    @Test
    public void testNullAndModelLessEntitiesPass() {
        assertFalse(ErpCrudStatusLock.shouldBlock(null), "null 实体放行");

        IOrmEntity modelLess = (IOrmEntity) Proxy.newProxyInstance(
                IOrmEntity.class.getClassLoader(), new Class<?>[]{IOrmEntity.class},
                (proxy, method, args) -> "orm_entityModel".equals(method.getName()) ? null : null);
        assertFalse(ErpCrudStatusLock.shouldBlock(modelLess), "无 ORM 模型实体放行");
    }
}
