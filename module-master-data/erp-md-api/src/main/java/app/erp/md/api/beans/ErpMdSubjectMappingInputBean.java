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

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _sourceSubjectId;

    
        @PropMeta(propId=2)
    
        public String getSourceSubjectId(){
            return _sourceSubjectId;
        }

        public void setSourceSubjectId(String value){
            this._sourceSubjectId = value;
        }


        private String _targetAcctSchemaId;

    
        @PropMeta(propId=3)
    
        public String getTargetAcctSchemaId(){
            return _targetAcctSchemaId;
        }

        public void setTargetAcctSchemaId(String value){
            this._targetAcctSchemaId = value;
        }


        private String _targetSubjectId;

    
        @PropMeta(propId=4)
    
        public String getTargetSubjectId(){
            return _targetSubjectId;
        }

        public void setTargetSubjectId(String value){
            this._targetSubjectId = value;
        }


    }
