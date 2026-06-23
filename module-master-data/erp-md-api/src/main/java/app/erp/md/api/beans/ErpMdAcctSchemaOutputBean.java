//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdAcctSchemaOutputBean {

    
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


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Integer _nature;

    
        @PropMeta(propId=5)
    
        public Integer getNature(){
            return _nature;
        }

        public void setNature(Integer value){
            this._nature = value;
        }


        private String _nature_label;

    
        public String getNature_label(){
            return _nature_label;
        }

        public void setNature_label(String value){
            this._nature_label = value;
        }


        private Long _functionalCurrencyId;

    
        @PropMeta(propId=6)
    
        public Long getFunctionalCurrencyId(){
            return _functionalCurrencyId;
        }

        public void setFunctionalCurrencyId(Long value){
            this._functionalCurrencyId = value;
        }


        private Integer _costingMethod;

    
        @PropMeta(propId=7)
    
        public Integer getCostingMethod(){
            return _costingMethod;
        }

        public void setCostingMethod(Integer value){
            this._costingMethod = value;
        }


        private String _costingMethod_label;

    
        public String getCostingMethod_label(){
            return _costingMethod_label;
        }

        public void setCostingMethod_label(String value){
            this._costingMethod_label = value;
        }


        private Boolean _isAdjustCurrency;

    
        @PropMeta(propId=8)
    
        public Boolean getIsAdjustCurrency(){
            return _isAdjustCurrency;
        }

        public void setIsAdjustCurrency(Boolean value){
            this._isAdjustCurrency = value;
        }


        private Boolean _isPropagate;

    
        @PropMeta(propId=9)
    
        public Boolean getIsPropagate(){
            return _isPropagate;
        }

        public void setIsPropagate(Boolean value){
            this._isPropagate = value;
        }


        private Integer _status;

    
        @PropMeta(propId=10)
    
        public Integer getStatus(){
            return _status;
        }

        public void setStatus(Integer value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=12)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=13)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=15)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _organization;

        public Map<String,Object> getOrganization(){
            return _organization;
        }

        public void setOrganization(Map<String,Object> value){
            this._organization = value;
        }


        private Map<String,Object> _functionalCurrency;

        public Map<String,Object> getFunctionalCurrency(){
            return _functionalCurrency;
        }

        public void setFunctionalCurrency(Map<String,Object> value){
            this._functionalCurrency = value;
        }


        private List<Map<String,Object>> _coaMappings;

        public List<Map<String,Object>> getCoaMappings(){
            return _coaMappings;
        }

        public void setCoaMappings(List<Map<String,Object>> value){
            this._coaMappings = value;
        }


    }
