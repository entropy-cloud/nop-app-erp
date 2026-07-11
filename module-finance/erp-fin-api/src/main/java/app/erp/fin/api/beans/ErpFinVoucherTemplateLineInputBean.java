//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinVoucherTemplateLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _templateId;

    
        @PropMeta(propId=2)
    
        public Long getTemplateId(){
            return _templateId;
        }

        public void setTemplateId(Long value){
            this._templateId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _subjectCode;

    
        @PropMeta(propId=4)
    
        public String getSubjectCode(){
            return _subjectCode;
        }

        public void setSubjectCode(String value){
            this._subjectCode = value;
        }


        private String _dcDirection;

    
        @PropMeta(propId=5)
    
        public String getDcDirection(){
            return _dcDirection;
        }

        public void setDcDirection(String value){
            this._dcDirection = value;
        }


        private String _amountExpression;

    
        @PropMeta(propId=6)
    
        public String getAmountExpression(){
            return _amountExpression;
        }

        public void setAmountExpression(String value){
            this._amountExpression = value;
        }


        private String _accountKey;

    
        @PropMeta(propId=7)
    
        public String getAccountKey(){
            return _accountKey;
        }

        public void setAccountKey(String value){
            this._accountKey = value;
        }


        private String _amountKey;

    
        @PropMeta(propId=8)
    
        public String getAmountKey(){
            return _amountKey;
        }

        public void setAmountKey(String value){
            this._amountKey = value;
        }


        private String _memoTemplate;

    
        @PropMeta(propId=9)
    
        public String getMemoTemplate(){
            return _memoTemplate;
        }

        public void setMemoTemplate(String value){
            this._memoTemplate = value;
        }


    }
