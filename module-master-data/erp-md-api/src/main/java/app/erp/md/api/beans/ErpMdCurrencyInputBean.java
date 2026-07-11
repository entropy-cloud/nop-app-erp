//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdCurrencyInputBean extends CrudInputBase {

    
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


        private String _symbol;

    
        @PropMeta(propId=4)
    
        public String getSymbol(){
            return _symbol;
        }

        public void setSymbol(String value){
            this._symbol = value;
        }


        private Integer _decimalPlaces;

    
        @PropMeta(propId=5)
    
        public Integer getDecimalPlaces(){
            return _decimalPlaces;
        }

        public void setDecimalPlaces(Integer value){
            this._decimalPlaces = value;
        }


        private Boolean _isFunctional;

    
        @PropMeta(propId=6)
    
        public Boolean getIsFunctional(){
            return _isFunctional;
        }

        public void setIsFunctional(Boolean value){
            this._isFunctional = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=7)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


    }
