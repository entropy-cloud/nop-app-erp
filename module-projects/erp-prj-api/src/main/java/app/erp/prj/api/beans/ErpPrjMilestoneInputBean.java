//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjMilestoneInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=2)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private String _code;

    
        @PropMeta(propId=3)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private String _name;

    
        @PropMeta(propId=4)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private java.time.LocalDate _plannedDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getPlannedDate(){
            return _plannedDate;
        }

        public void setPlannedDate(java.time.LocalDate value){
            this._plannedDate = value;
        }


        private java.time.LocalDate _actualDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getActualDate(){
            return _actualDate;
        }

        public void setActualDate(java.time.LocalDate value){
            this._actualDate = value;
        }


        private java.math.BigDecimal _billingAmount;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getBillingAmount(){
            return _billingAmount;
        }

        public void setBillingAmount(java.math.BigDecimal value){
            this._billingAmount = value;
        }


        private Boolean _isBillingTrigger;

    
        @PropMeta(propId=8)
    
        public Boolean getIsBillingTrigger(){
            return _isBillingTrigger;
        }

        public void setIsBillingTrigger(Boolean value){
            this._isBillingTrigger = value;
        }


        private String _status;

    
        @PropMeta(propId=9)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
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
