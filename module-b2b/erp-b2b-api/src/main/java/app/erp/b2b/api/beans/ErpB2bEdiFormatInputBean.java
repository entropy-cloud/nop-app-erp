//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bEdiFormatInputBean extends CrudInputBase {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _formatName;

    
        @PropMeta(propId=4)
    
        public String getFormatName(){
            return _formatName;
        }

        public void setFormatName(String value){
            this._formatName = value;
        }


        private String _formatStandard;

    
        @PropMeta(propId=5)
    
        public String getFormatStandard(){
            return _formatStandard;
        }

        public void setFormatStandard(String value){
            this._formatStandard = value;
        }


        private String _direction;

    
        @PropMeta(propId=6)
    
        public String getDirection(){
            return _direction;
        }

        public void setDirection(String value){
            this._direction = value;
        }


        private Integer _needsWebService;

    
        @PropMeta(propId=7)
    
        public Integer getNeedsWebService(){
            return _needsWebService;
        }

        public void setNeedsWebService(Integer value){
            this._needsWebService = value;
        }


        private Integer _isActive;

    
        @PropMeta(propId=8)
    
        public Integer getIsActive(){
            return _isActive;
        }

        public void setIsActive(Integer value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
