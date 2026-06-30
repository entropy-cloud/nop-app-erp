
package app.erp.b2b.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bMftCertificateBiz;
import app.erp.b2b.dao.entity.ErpB2bMftCertificate;

@BizModel("ErpB2bMftCertificate")
public class ErpB2bMftCertificateBizModel extends CrudBizModel<ErpB2bMftCertificate> implements IErpB2bMftCertificateBiz{
    public ErpB2bMftCertificateBizModel(){
        setEntityName(ErpB2bMftCertificate.class.getName());
    }
}
