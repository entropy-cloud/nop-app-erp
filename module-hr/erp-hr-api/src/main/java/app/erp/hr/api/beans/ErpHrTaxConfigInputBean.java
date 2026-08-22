//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrTaxConfigInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private Integer _year;

    
        @PropMeta(propId=2)
    
        public Integer getYear(){
            return _year;
        }

        public void setYear(Integer value){
            this._year = value;
        }


        private java.math.BigDecimal _taxThreshold;

    
        @PropMeta(propId=3)
    
        public java.math.BigDecimal getTaxThreshold(){
            return _taxThreshold;
        }

        public void setTaxThreshold(java.math.BigDecimal value){
            this._taxThreshold = value;
        }


        private String _taxBrackets;

    
        @PropMeta(propId=4)
    
        public String getTaxBrackets(){
            return _taxBrackets;
        }

        public void setTaxBrackets(String value){
            this._taxBrackets = value;
        }


        private String _specialDeductionItems;

    
        @PropMeta(propId=5)
    
        public String getSpecialDeductionItems(){
            return _specialDeductionItems;
        }

        public void setSpecialDeductionItems(String value){
            this._specialDeductionItems = value;
        }


        private String _orgId;

    
        @PropMeta(propId=6)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=7)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
