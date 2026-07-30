//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmTerritoryAssignmentRuleOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _ruleName;

    
        @PropMeta(propId=3)
    
        public String getRuleName(){
            return _ruleName;
        }

        public void setRuleName(String value){
            this._ruleName = value;
        }


        private Integer _priority;

    
        @PropMeta(propId=4)
    
        public Integer getPriority(){
            return _priority;
        }

        public void setPriority(Integer value){
            this._priority = value;
        }


        private Long _territoryId;

    
        @PropMeta(propId=5)
    
        public Long getTerritoryId(){
            return _territoryId;
        }

        public void setTerritoryId(Long value){
            this._territoryId = value;
        }


        private String _conditionType;

    
        @PropMeta(propId=6)
    
        public String getConditionType(){
            return _conditionType;
        }

        public void setConditionType(String value){
            this._conditionType = value;
        }


        private String _conditionType_label;

    
        public String getConditionType_label(){
            return _conditionType_label;
        }

        public void setConditionType_label(String value){
            this._conditionType_label = value;
        }


        private String _conditionValue;

    
        @PropMeta(propId=7)
    
        public String getConditionValue(){
            return _conditionValue;
        }

        public void setConditionValue(String value){
            this._conditionValue = value;
        }


        private String _assignmentMethod;

    
        @PropMeta(propId=8)
    
        public String getAssignmentMethod(){
            return _assignmentMethod;
        }

        public void setAssignmentMethod(String value){
            this._assignmentMethod = value;
        }


        private String _assignmentMethod_label;

    
        public String getAssignmentMethod_label(){
            return _assignmentMethod_label;
        }

        public void setAssignmentMethod_label(String value){
            this._assignmentMethod_label = value;
        }


        private Long _groupId;

    
        @PropMeta(propId=9)
    
        public Long getGroupId(){
            return _groupId;
        }

        public void setGroupId(Long value){
            this._groupId = value;
        }


        private Boolean _isDefault;

    
        @PropMeta(propId=10)
    
        public Boolean getIsDefault(){
            return _isDefault;
        }

        public void setIsDefault(Boolean value){
            this._isDefault = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=11)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _territory;

        public Map<String,Object> getTerritory(){
            return _territory;
        }

        public void setTerritory(Map<String,Object> value){
            this._territory = value;
        }


        private Map<String,Object> _team;

        public Map<String,Object> getTeam(){
            return _team;
        }

        public void setTeam(Map<String,Object> value){
            this._team = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
