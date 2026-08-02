
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinVoucherTemplateBiz;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.service.processor.ErpFinVoucherTemplateRenderTemplateProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@BizModel("ErpFinVoucherTemplate")
public class ErpFinVoucherTemplateBizModel extends CrudBizModel<ErpFinVoucherTemplate> implements IErpFinVoucherTemplateBiz {

    @Inject
    ErpFinVoucherTemplateRenderTemplateProcessor renderTemplateProcessor;

    public ErpFinVoucherTemplateBizModel(){
        setEntityName(ErpFinVoucherTemplate.class.getName());
    }

    /**
     * 凭证模板预览（F16 P1）。委派 {@link ErpFinVoucherTemplateRenderTemplateProcessor}。
     * 仅用于前端预览，不触碰过账引擎。
     */
    @BizMutation
    public List<Map<String, Object>> renderTemplate(
            @Name("businessType") String businessType,
            @Name("context") Map<String, Object> context) {
        return renderTemplateProcessor.renderTemplate(businessType, context);
    }
}
