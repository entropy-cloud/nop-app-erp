
package app.erp.b2b.service.entity;

import app.erp.b2b.biz.IErpB2bMftCertificateBiz;
import app.erp.b2b.dao.entity.ErpB2bMftCertificate;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * MFT 证书 BizModel。CRUD 之外提供证书过期检查查询入口。
 *
 * <p>注意：ORM 实体仅存储证书元数据（指纹/算法/过期日期），不含 PEM/私钥列。
 * 真实私钥加密存储（{@code EncryptionHelper}）归 follow-up（触发条件：真实证书集成）。
 *
 * <p>对应 {@code managed-file-transfer.md §证书管理}。
 */
@BizModel("ErpB2bMftCertificate")
public class ErpB2bMftCertificateBizModel extends CrudBizModel<ErpB2bMftCertificate>
        implements IErpB2bMftCertificateBiz {

    public ErpB2bMftCertificateBizModel() {
        setEntityName(ErpB2bMftCertificate.class.getName());
    }

    @Override
    @BizQuery
    public List<ErpB2bMftCertificate> findExpiringCertificates(@Name("withinDays") Integer withinDays,
                                                                IServiceContext context) {
        int days = withinDays != null ? withinDays : 30;
        LocalDate threshold = CoreMetrics.today().plusDays(days);

        IEntityDao<ErpB2bMftCertificate> dao = daoProvider().daoFor(ErpB2bMftCertificate.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", true));
        q.addFilter(le("expiresAt", threshold));
        q.setLimit(200);
        return dao.findAllByQuery(q);
    }
}
