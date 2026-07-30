//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmLeadConvLogOutputBean {

    
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


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=10)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=11)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=13)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _lead;

        public Map<String,Object> getLead(){
            return _lead;
        }

        public void setLead(Map<String,Object> value){
            this._lead = value;
        }


        private Map<String,Object> _fromStage;

        public Map<String,Object> getFromStage(){
            return _fromStage;
        }

        public void setFromStage(Map<String,Object> value){
            this._fromStage = value;
        }


        private Map<String,Object> _toStage;

        public Map<String,Object> getToStage(){
            return _toStage;
        }

        public void setToStage(Map<String,Object> value){
            this._toStage = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
