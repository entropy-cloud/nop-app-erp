//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaSamplingPlanInputBean extends CrudInputBase {

    
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


    }
