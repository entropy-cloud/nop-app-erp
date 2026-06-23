//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdAcctSchemaInputBean extends CrudInputBase {

    
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


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private List<ErpMdAcctSchemaCoaInputBean> _coaMappings;

        public List<ErpMdAcctSchemaCoaInputBean> getCoaMappings(){
            return _coaMappings;
        }

        public void setCoaMappings(List<ErpMdAcctSchemaCoaInputBean> value){
            this._coaMappings = value;
        }


    }
