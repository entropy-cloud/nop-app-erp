package app.erp.common.service;

import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.support.OrmEntity;
import jakarta.inject.Inject;

import java.sql.Timestamp;

/**
 * 项目级 Processor 根基类（plan 2026-07-24-2200-1 Phase 1）。
 *
 * <p>提供所有 per-mutation Processor 共用的辅助方法：实体加载、状态守卫、当前用户/时间戳获取。
 * 子类（{@code AbstractApproveProcessor} 等）在此之上构建特定 mutation 的编排骨架。
 *
 * <p>设计原则：
 * <ul>
 *   <li>状态读取/写入通过抽象方法委托给子类（避免反射，保留类型安全）</li>
 *   <li>所有 hook 方法默认空实现，子类按需覆盖</li>
 *   <li>异常构造委托给子类（保留各域错误码语义）</li>
 * </ul>
 */
public abstract class AbstractProcessor<T extends OrmEntity> {

    @Inject
    protected IDaoProvider daoProvider;

    protected abstract IEntityDao<T> dao();

    public T requireEntity(String id) {
        T entity = dao().getEntityById(id);
        if (entity == null) {
            throw notFoundException(id);
        }
        return entity;
    }

    public T requireEntityForUpdate(String id) {
        return requireEntity(id);
    }

    public void checkEntityNotNull(T entity, String id) {
        if (entity == null) {
            throw notFoundException(id);
        }
    }

    protected String getCreatedBy(T entity) {
        Object value = entity.orm_propValueByName("createdBy");
        return value == null ? null : value.toString();
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected Timestamp now() {
        return CoreMetrics.currentTimestamp();
    }

    protected NopException defaultNotFoundException(String id) {
        return new NopException(ErpCommonErrors.ERR_ENTITY_NOT_FOUND)
                .param(ErpCommonErrors.ARG_BIZ_OBJ_ID, id);
    }

    protected abstract NopException notFoundException(String id);

    /**
     * 非法状态迁移领域异常工厂（骨架守卫唯一出码口）。plan 2026-09-07-2200-1 起 abstract：
     * StateMachine 直抛领域码后，common 码 {@code ERR_ILLEGAL_STATUS_TRANSITION} 无通用消费方
     * （lesson 19 反模式），骨架守卫路径不得再回落 common 码——各实体必须给出领域码实现。
     * current 与 expected 均传状态码/枚举名/字典值本身（禁中文散文），否定语义传 {@code "!" + 状态码}。
     */
    protected abstract NopException illegalStatusException(T entity, String current, String... expected);
}
