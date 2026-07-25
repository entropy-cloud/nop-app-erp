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
import java.util.Objects;

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

    public void validateDocStatus(T entity, String docStatusField, String... allowedStatuses) {
        String current = readStatus(entity, docStatusField);
        for (String allowed : allowedStatuses) {
            if (Objects.equals(current, allowed)) {
                return;
            }
        }
        throw illegalStatusException(entity, current, allowedStatuses);
    }

    protected String readStatus(T entity, String fieldName) {
        Object value = entity.orm_propValueByName(fieldName);
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

    protected NopException defaultIllegalStatusException(String current, String... expected) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, current)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    protected abstract NopException notFoundException(String id);

    protected NopException illegalStatusException(T entity, String current, String... expected) {
        return defaultIllegalStatusException(current, expected);
    }
}
