//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdUoMConversionInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _materialId;

    
        @PropMeta(propId=2)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _fromUoMId;

    
        @PropMeta(propId=3)
    
        public String getFromUoMId(){
            return _fromUoMId;
        }

        public void setFromUoMId(String value){
            this._fromUoMId = value;
        }


        private String _toUoMId;

    
        @PropMeta(propId=4)
    
        public String getToUoMId(){
            return _toUoMId;
        }

        public void setToUoMId(String value){
            this._toUoMId = value;
        }


        private java.math.BigDecimal _conversionRate;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getConversionRate(){
            return _conversionRate;
        }

        public void setConversionRate(java.math.BigDecimal value){
            this._conversionRate = value;
        }


    }
