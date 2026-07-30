//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsCannedResponseInputBean extends CrudInputBase {

    
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


        private String _title;

    
        @PropMeta(propId=4)
    
        public String getTitle(){
            return _title;
        }

        public void setTitle(String value){
            this._title = value;
        }


        private String _content;

    
        @PropMeta(propId=5)
    
        public String getContent(){
            return _content;
        }

        public void setContent(String value){
            this._content = value;
        }


        private Long _categoryId;

    
        @PropMeta(propId=6)
    
        public Long getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(Long value){
            this._categoryId = value;
        }


        private String _variableDefs;

    
        @PropMeta(propId=7)
    
        public String getVariableDefs(){
            return _variableDefs;
        }

        public void setVariableDefs(String value){
            this._variableDefs = value;
        }


        private Long _macroTicketTypeId;

    
        @PropMeta(propId=8)
    
        public Long getMacroTicketTypeId(){
            return _macroTicketTypeId;
        }

        public void setMacroTicketTypeId(Long value){
            this._macroTicketTypeId = value;
        }


        private String _macroPriority;

    
        @PropMeta(propId=9)
    
        public String getMacroPriority(){
            return _macroPriority;
        }

        public void setMacroPriority(String value){
            this._macroPriority = value;
        }


        private Integer _sequence;

    
        @PropMeta(propId=10)
    
        public Integer getSequence(){
            return _sequence;
        }

        public void setSequence(Integer value){
            this._sequence = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=11)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private Integer _usageCount;

    
        @PropMeta(propId=12)
    
        public Integer getUsageCount(){
            return _usageCount;
        }

        public void setUsageCount(Integer value){
            this._usageCount = value;
        }


    }
