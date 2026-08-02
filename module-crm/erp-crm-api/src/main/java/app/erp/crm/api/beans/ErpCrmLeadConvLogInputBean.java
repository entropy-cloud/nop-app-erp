//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmLeadConvLogInputBean extends CrudInputBase {

    
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


        private Long _fromStageId;

    
        @PropMeta(propId=4)
    
        public Long getFromStageId(){
            return _fromStageId;
        }

        public void setFromStageId(Long value){
            this._fromStageId = value;
        }


        private Long _toStageId;

    
        @PropMeta(propId=5)
    
        public Long getToStageId(){
            return _toStageId;
        }

        public void setToStageId(Long value){
            this._toStageId = value;
        }


        private java.sql.Timestamp _changedAt;

    
        @PropMeta(propId=6)
    
        public java.sql.Timestamp getChangedAt(){
            return _changedAt;
        }

        public void setChangedAt(java.sql.Timestamp value){
            this._changedAt = value;
        }


        private String _changedBy;

    
        @PropMeta(propId=7)
    
        public String getChangedBy(){
            return _changedBy;
        }

        public void setChangedBy(String value){
            this._changedBy = value;
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
