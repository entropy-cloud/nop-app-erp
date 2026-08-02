//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmCampaignInputBean extends CrudInputBase {

    
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


        private String _campaignName;

    
        @PropMeta(propId=5)
    
        public String getCampaignName(){
            return _campaignName;
        }

        public void setCampaignName(String value){
            this._campaignName = value;
        }


        private String _medium;

    
        @PropMeta(propId=6)
    
        public String getMedium(){
            return _medium;
        }

        public void setMedium(String value){
            this._medium = value;
        }


        private String _source;

    
        @PropMeta(propId=7)
    
        public String getSource(){
            return _source;
        }

        public void setSource(String value){
            this._source = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private java.math.BigDecimal _budgetAmount;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getBudgetAmount(){
            return _budgetAmount;
        }

        public void setBudgetAmount(java.math.BigDecimal value){
            this._budgetAmount = value;
        }


        private java.math.BigDecimal _actualCost;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getActualCost(){
            return _actualCost;
        }

        public void setActualCost(java.math.BigDecimal value){
            this._actualCost = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
