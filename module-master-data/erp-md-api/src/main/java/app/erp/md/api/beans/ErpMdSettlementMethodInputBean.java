//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdSettlementMethodInputBean extends CrudInputBase {

    
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


        private Integer _settlementType;

    
        @PropMeta(propId=4)
    
        public Integer getSettlementType(){
            return _settlementType;
        }

        public void setSettlementType(Integer value){
            this._settlementType = value;
        }


        private Long _defaultFundAccountId;

    
        @PropMeta(propId=5)
    
        public Long getDefaultFundAccountId(){
            return _defaultFundAccountId;
        }

        public void setDefaultFundAccountId(Long value){
            this._defaultFundAccountId = value;
        }


        private Integer _defaultDays;

    
        @PropMeta(propId=6)
    
        public Integer getDefaultDays(){
            return _defaultDays;
        }

        public void setDefaultDays(Integer value){
            this._defaultDays = value;
        }


        private Integer _status;

    
        @PropMeta(propId=7)
    
        public Integer getStatus(){
            return _status;
        }

        public void setStatus(Integer value){
            this._status = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=8)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
