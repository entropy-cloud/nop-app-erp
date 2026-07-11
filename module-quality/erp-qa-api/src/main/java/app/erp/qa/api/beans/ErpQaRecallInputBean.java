//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaRecallInputBean extends CrudInputBase {

    
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


        private String _recallName;

    
        @PropMeta(propId=3)
    
        public String getRecallName(){
            return _recallName;
        }

        public void setRecallName(String value){
            this._recallName = value;
        }


        private String _triggerType;

    
        @PropMeta(propId=4)
    
        public String getTriggerType(){
            return _triggerType;
        }

        public void setTriggerType(String value){
            this._triggerType = value;
        }


        private Long _sourceNcrId;

    
        @PropMeta(propId=5)
    
        public Long getSourceNcrId(){
            return _sourceNcrId;
        }

        public void setSourceNcrId(Long value){
            this._sourceNcrId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=6)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _batchId;

    
        @PropMeta(propId=7)
    
        public Long getBatchId(){
            return _batchId;
        }

        public void setBatchId(Long value){
            this._batchId = value;
        }


        private String _serialNo;

    
        @PropMeta(propId=8)
    
        public String getSerialNo(){
            return _serialNo;
        }

        public void setSerialNo(String value){
            this._serialNo = value;
        }


        private String _rootCause;

    
        @PropMeta(propId=9)
    
        public String getRootCause(){
            return _rootCause;
        }

        public void setRootCause(String value){
            this._rootCause = value;
        }


        private String _severityLevel;

    
        @PropMeta(propId=10)
    
        public String getSeverityLevel(){
            return _severityLevel;
        }

        public void setSeverityLevel(String value){
            this._severityLevel = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Boolean _notifyCustomer;

    
        @PropMeta(propId=12)
    
        public Boolean getNotifyCustomer(){
            return _notifyCustomer;
        }

        public void setNotifyCustomer(Boolean value){
            this._notifyCustomer = value;
        }


        private String _status;

    
        @PropMeta(propId=13)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=14)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=17)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpQaRecallTargetInputBean> _targets;

        public List<ErpQaRecallTargetInputBean> getTargets(){
            return _targets;
        }

        public void setTargets(List<ErpQaRecallTargetInputBean> value){
            this._targets = value;
        }


    }
