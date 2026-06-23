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

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=2)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _fromUoMId;

    
        @PropMeta(propId=3)
    
        public Long getFromUoMId(){
            return _fromUoMId;
        }

        public void setFromUoMId(Long value){
            this._fromUoMId = value;
        }


        private Long _toUoMId;

    
        @PropMeta(propId=4)
    
        public Long getToUoMId(){
            return _toUoMId;
        }

        public void setToUoMId(Long value){
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


        private Long _delVersion;

    
        @PropMeta(propId=6)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
