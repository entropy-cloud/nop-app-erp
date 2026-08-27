//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinApDocumentLogInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _documentId;

    
        @PropMeta(propId=2)
    
        public String getDocumentId(){
            return _documentId;
        }

        public void setDocumentId(String value){
            this._documentId = value;
        }


        private String _step;

    
        @PropMeta(propId=3)
    
        public String getStep(){
            return _step;
        }

        public void setStep(String value){
            this._step = value;
        }


        private Boolean _success;

    
        @PropMeta(propId=4)
    
        public Boolean getSuccess(){
            return _success;
        }

        public void setSuccess(Boolean value){
            this._success = value;
        }


        private String _detail;

    
        @PropMeta(propId=5)
    
        public String getDetail(){
            return _detail;
        }

        public void setDetail(String value){
            this._detail = value;
        }


    }
