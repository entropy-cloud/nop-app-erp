//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bCodeMappingInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _mappingType;

    
        @PropMeta(propId=3)
    
        public String getMappingType(){
            return _mappingType;
        }

        public void setMappingType(String value){
            this._mappingType = value;
        }


        private String _internalCode;

    
        @PropMeta(propId=4)
    
        public String getInternalCode(){
            return _internalCode;
        }

        public void setInternalCode(String value){
            this._internalCode = value;
        }


        private String _externalCode;

    
        @PropMeta(propId=5)
    
        public String getExternalCode(){
            return _externalCode;
        }

        public void setExternalCode(String value){
            this._externalCode = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=6)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
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
