//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmLeadSequenceProgressInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _leadId;

    
        @PropMeta(propId=2)
    
        public Long getLeadId(){
            return _leadId;
        }

        public void setLeadId(Long value){
            this._leadId = value;
        }


        private Long _sequenceId;

    
        @PropMeta(propId=3)
    
        public Long getSequenceId(){
            return _sequenceId;
        }

        public void setSequenceId(Long value){
            this._sequenceId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Integer _currentStepIndex;

    
        @PropMeta(propId=5)
    
        public Integer getCurrentStepIndex(){
            return _currentStepIndex;
        }

        public void setCurrentStepIndex(Integer value){
            this._currentStepIndex = value;
        }


        private String _status;

    
        @PropMeta(propId=6)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private java.sql.Timestamp _startedAt;

    
        @PropMeta(propId=7)
    
        public java.sql.Timestamp getStartedAt(){
            return _startedAt;
        }

        public void setStartedAt(java.sql.Timestamp value){
            this._startedAt = value;
        }


        private java.sql.Timestamp _completedAt;

    
        @PropMeta(propId=8)
    
        public java.sql.Timestamp getCompletedAt(){
            return _completedAt;
        }

        public void setCompletedAt(java.sql.Timestamp value){
            this._completedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
