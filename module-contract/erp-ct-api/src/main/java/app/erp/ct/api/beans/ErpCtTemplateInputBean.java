//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtTemplateInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _contractType;

    
        @PropMeta(propId=4)
    
        public String getContractType(){
            return _contractType;
        }

        public void setContractType(String value){
            this._contractType = value;
        }


        private String _contentTemplate;

    
        @PropMeta(propId=5)
    
        public String getContentTemplate(){
            return _contentTemplate;
        }

        public void setContentTemplate(String value){
            this._contentTemplate = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=6)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
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
