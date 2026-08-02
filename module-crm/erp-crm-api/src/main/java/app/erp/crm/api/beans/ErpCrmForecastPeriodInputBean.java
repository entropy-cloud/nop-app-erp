//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmForecastPeriodInputBean extends CrudInputBase {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _periodType;

    
        @PropMeta(propId=4)
    
        public String getPeriodType(){
            return _periodType;
        }

        public void setPeriodType(String value){
            this._periodType = value;
        }


        private java.time.LocalDate _periodStart;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getPeriodStart(){
            return _periodStart;
        }

        public void setPeriodStart(java.time.LocalDate value){
            this._periodStart = value;
        }


        private java.time.LocalDate _periodEnd;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getPeriodEnd(){
            return _periodEnd;
        }

        public void setPeriodEnd(java.time.LocalDate value){
            this._periodEnd = value;
        }


        private String _label;

    
        @PropMeta(propId=7)
    
        public String getLabel(){
            return _label;
        }

        public void setLabel(String value){
            this._label = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Boolean _isCurrent;

    
        @PropMeta(propId=9)
    
        public Boolean getIsCurrent(){
            return _isCurrent;
        }

        public void setIsCurrent(Boolean value){
            this._isCurrent = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
