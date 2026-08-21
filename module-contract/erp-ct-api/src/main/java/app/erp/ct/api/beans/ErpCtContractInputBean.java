//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtContractInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _contractName;

    
        @PropMeta(propId=4)
    
        public String getContractName(){
            return _contractName;
        }

        public void setContractName(String value){
            this._contractName = value;
        }


        private String _contractType;

    
        @PropMeta(propId=5)
    
        public String getContractType(){
            return _contractType;
        }

        public void setContractType(String value){
            this._contractType = value;
        }


        private String _contractDirection;

    
        @PropMeta(propId=6)
    
        public String getContractDirection(){
            return _contractDirection;
        }

        public void setContractDirection(String value){
            this._contractDirection = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=7)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=8)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _totalAmount;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(java.math.BigDecimal value){
            this._totalAmount = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private java.time.LocalDate _signDate;

    
        @PropMeta(propId=12)
    
        public java.time.LocalDate getSignDate(){
            return _signDate;
        }

        public void setSignDate(java.time.LocalDate value){
            this._signDate = value;
        }


        private String _status;

    
        @PropMeta(propId=13)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _templateId;

    
        @PropMeta(propId=14)
    
        public String getTemplateId(){
            return _templateId;
        }

        public void setTemplateId(String value){
            this._templateId = value;
        }


        private String _parentContractId;

    
        @PropMeta(propId=15)
    
        public String getParentContractId(){
            return _parentContractId;
        }

        public void setParentContractId(String value){
            this._parentContractId = value;
        }


        private String _description;

    
        @PropMeta(propId=16)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _attachmentFileId;

    
        @PropMeta(propId=17)
    
        public String getAttachmentFileId(){
            return _attachmentFileId;
        }

        public void setAttachmentFileId(String value){
            this._attachmentFileId = value;
        }


        private String _remark;

    
        @PropMeta(propId=18)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=25)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private List<ErpCtContractLineInputBean> _lines;

        public List<ErpCtContractLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpCtContractLineInputBean> value){
            this._lines = value;
        }


        private List<ErpCtContractVersionInputBean> _versions;

        public List<ErpCtContractVersionInputBean> getVersions(){
            return _versions;
        }

        public void setVersions(List<ErpCtContractVersionInputBean> value){
            this._versions = value;
        }


    }
