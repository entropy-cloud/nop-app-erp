//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdSubjectMappingInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _sourceSubjectId;

    
        @PropMeta(propId=2)
    
        public Long getSourceSubjectId(){
            return _sourceSubjectId;
        }

        public void setSourceSubjectId(Long value){
            this._sourceSubjectId = value;
        }


        private Long _targetAcctSchemaId;

    
        @PropMeta(propId=3)
    
        public Long getTargetAcctSchemaId(){
            return _targetAcctSchemaId;
        }

        public void setTargetAcctSchemaId(Long value){
            this._targetAcctSchemaId = value;
        }


        private Long _targetSubjectId;

    
        @PropMeta(propId=4)
    
        public Long getTargetSubjectId(){
            return _targetSubjectId;
        }

        public void setTargetSubjectId(Long value){
            this._targetSubjectId = value;
        }


    }
