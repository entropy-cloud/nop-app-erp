package app.erp.common.service;

import io.nop.dao.DaoErrors;
import io.nop.dao.exceptions.JdbcException;

/**
 * 唯一约束冲突判定工具（plan 2026-07-30-0841-2 R1.28 并发 UK 幂等修复）。
 *
 * <p>平台 JDBC 翻译器把 {@link java.sql.SQLIntegrityConstraintViolationException} 翻译为
 * {@link JdbcException}，错误码为 {@link DaoErrors#ERR_SQL_DUPLICATE_KEY}（H2/PG/MySQL 唯一索引冲突）
 * 或 {@link DaoErrors#ERR_SQL_DATA_INTEGRITY_VIOLATION}（其他完整性违例，少数驱动归类至此）。
 * 遍历 cause 链以兼容包装异常（事务管理器 / IoC）。
 *
 * <p>典型用法：在并发首插（INSERT）路径 flush 后捕获异常，判定为 UK 冲突时翻译为域友好错误码，
 * 避免向调用方暴露 {@code ERR_ORM_DATA_EXCEPTION} 丑陋异常（参 inventory {@code StockMoveBookkeeper} 范式）。
 */
public final class UniqueConstraintHelper {

    private UniqueConstraintHelper() {
    }

    public static boolean isUniqueConstraintViolation(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof JdbcException) {
                String code = ((JdbcException) cur).getErrorCode();
                if (DaoErrors.ERR_SQL_DUPLICATE_KEY.getErrorCode().equals(code)
                        || DaoErrors.ERR_SQL_DATA_INTEGRITY_VIOLATION.getErrorCode().equals(code)) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}
