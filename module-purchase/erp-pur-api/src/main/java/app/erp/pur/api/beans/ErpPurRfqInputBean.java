//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurRfqInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _requisitionId;

    
        @PropMeta(propId=4)
    
        public Long getRequisitionId(){
            return _requisitionId;
        }

        public void setRequisitionId(Long value){
            this._requisitionId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private java.time.LocalDate _validUntil;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getValidUntil(){
            return _validUntil;
        }

        public void setValidUntil(java.time.LocalDate value){
            this._validUntil = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=7)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=8)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpPurRfqLineInputBean> _lines;

        public List<ErpPurRfqLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpPurRfqLineInputBean> value){
            this._lines = value;
        }


        private List<ErpPurQuotationInputBean> _quotations;

        public List<ErpPurQuotationInputBean> getQuotations(){
            return _quotations;
        }

        public void setQuotations(List<ErpPurQuotationInputBean> value){
            this._quotations = value;
        }


    }
