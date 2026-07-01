//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaSamplingPlanOutputBean {

    
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


        private String _aqlLevel;

    
        @PropMeta(propId=4)
    
        public String getAqlLevel(){
            return _aqlLevel;
        }

        public void setAqlLevel(String value){
            this._aqlLevel = value;
        }


        private java.math.BigDecimal _lotSizeFrom;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getLotSizeFrom(){
            return _lotSizeFrom;
        }

        public void setLotSizeFrom(java.math.BigDecimal value){
            this._lotSizeFrom = value;
        }


        private java.math.BigDecimal _lotSizeTo;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getLotSizeTo(){
            return _lotSizeTo;
        }

        public void setLotSizeTo(java.math.BigDecimal value){
            this._lotSizeTo = value;
        }


        private java.math.BigDecimal _sampleSize;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getSampleSize(){
            return _sampleSize;
        }

        public void setSampleSize(java.math.BigDecimal value){
            this._sampleSize = value;
        }


        private Integer _acceptNumber;

    
        @PropMeta(propId=8)
    
        public Integer getAcceptNumber(){
            return _acceptNumber;
        }

        public void setAcceptNumber(Integer value){
            this._acceptNumber = value;
        }


        private Integer _rejectNumber;

    
        @PropMeta(propId=9)
    
        public Integer getRejectNumber(){
            return _rejectNumber;
        }

        public void setRejectNumber(Integer value){
            this._rejectNumber = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=10)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=12)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=13)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=14)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=16)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


    }
