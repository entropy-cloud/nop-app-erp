//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmActivityInputBean extends CrudInputBase {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _activityType;

    
        @PropMeta(propId=4)
    
        public String getActivityType(){
            return _activityType;
        }

        public void setActivityType(String value){
            this._activityType = value;
        }


        private java.time.LocalDate _activityDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getActivityDate(){
            return _activityDate;
        }

        public void setActivityDate(java.time.LocalDate value){
            this._activityDate = value;
        }


        private String _summary;

    
        @PropMeta(propId=6)
    
        public String getSummary(){
            return _summary;
        }

        public void setSummary(String value){
            this._summary = value;
        }


        private String _ownerId;

    
        @PropMeta(propId=7)
    
        public String getOwnerId(){
            return _ownerId;
        }

        public void setOwnerId(String value){
            this._ownerId = value;
        }


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
