package app.erp.common.service;

import io.nop.orm.IOrmEntity;
import io.nop.orm.model.IEntityModel;

/**
 * F1.3（ai-check P1-CK-pur-003 族）：通用 CRUD 状态锁判定（纯函数，供单测与基类共享）。
 *
 * <p>posted=true 或 approveStatus=APPROVED（列存在时）的单据视为锁定——通用 {@code __update}/
 * {@code __delete} mutation 拒绝；内部状态机动作经 BizModel {@code updateEntity} helper 直写
 * 不经 prepare 钩子，不受影响（平台 CrudBizModel.java L1988/L2020/L2048 实证）。
 * posted 语义按「锁定」统一解读（含个别域 posted=非 GL 语义的列——普查无冲突用例，
 * 见 plan 2026-08-26-0430-1 §posted 列语义不统一假设）。
 */
public final class ErpCrudStatusLock {

    public static final String PROP_POSTED = "posted";
    public static final String PROP_APPROVE_STATUS = "approveStatus";

    private ErpCrudStatusLock() {
    }

    /** 判定实体是否因状态锁定而应拒绝通用 update/delete。无 posted/approveStatus 列的实体恒 false。 */
    public static boolean shouldBlock(IOrmEntity entity) {
        if (entity == null) {
            return false;
        }
        IEntityModel model = entity.orm_entityModel();
        if (model == null) {
            return false;
        }
        if (model.getColumn(PROP_POSTED, true) != null
                && Boolean.TRUE.equals(entity.orm_propValueByName(PROP_POSTED))) {
            return true;
        }
        if (model.getColumn(PROP_APPROVE_STATUS, true) != null) {
            Object approveStatus = entity.orm_propValueByName(PROP_APPROVE_STATUS);
            return "APPROVED".equals(approveStatus);
        }
        return false;
    }
}
