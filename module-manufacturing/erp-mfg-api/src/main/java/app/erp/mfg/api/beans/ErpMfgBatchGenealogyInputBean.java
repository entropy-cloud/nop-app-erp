//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgBatchGenealogyInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _workOrderId;

    
        @PropMeta(propId=2)
    
        public Long getWorkOrderId(){
            return _workOrderId;
        }

        public void setWorkOrderId(Long value){
            this._workOrderId = value;
        }


        private Long _jobCardId;

    
        @PropMeta(propId=3)
    
        public Long getJobCardId(){
            return _jobCardId;
        }

        public void setJobCardId(Long value){
            this._jobCardId = value;
        }


        private Long _operationId;

    
        @PropMeta(propId=4)
    
        public Long getOperationId(){
            return _operationId;
        }

        public void setOperationId(Long value){
            this._operationId = value;
        }


        private Long _inputLotId;

    
        @PropMeta(propId=5)
    
        public Long getInputLotId(){
            return _inputLotId;
        }

        public void setInputLotId(Long value){
            this._inputLotId = value;
        }


        private Long _inputMaterialId;

    
        @PropMeta(propId=6)
    
        public Long getInputMaterialId(){
            return _inputMaterialId;
        }

        public void setInputMaterialId(Long value){
            this._inputMaterialId = value;
        }


        private java.math.BigDecimal _inputQty;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getInputQty(){
            return _inputQty;
        }

        public void setInputQty(java.math.BigDecimal value){
            this._inputQty = value;
        }


        private Long _inputUoMId;

    
        @PropMeta(propId=8)
    
        public Long getInputUoMId(){
            return _inputUoMId;
        }

        public void setInputUoMId(Long value){
            this._inputUoMId = value;
        }


        private Long _outputLotId;

    
        @PropMeta(propId=9)
    
        public Long getOutputLotId(){
            return _outputLotId;
        }

        public void setOutputLotId(Long value){
            this._outputLotId = value;
        }


        private Long _outputMaterialId;

    
        @PropMeta(propId=10)
    
        public Long getOutputMaterialId(){
            return _outputMaterialId;
        }

        public void setOutputMaterialId(Long value){
            this._outputMaterialId = value;
        }


        private java.math.BigDecimal _outputQty;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getOutputQty(){
            return _outputQty;
        }

        public void setOutputQty(java.math.BigDecimal value){
            this._outputQty = value;
        }


        private Long _outputUoMId;

    
        @PropMeta(propId=12)
    
        public Long getOutputUoMId(){
            return _outputUoMId;
        }

        public void setOutputUoMId(Long value){
            this._outputUoMId = value;
        }


        private java.time.LocalDate _productionDate;

    
        @PropMeta(propId=13)
    
        public java.time.LocalDate getProductionDate(){
            return _productionDate;
        }

        public void setProductionDate(java.time.LocalDate value){
            this._productionDate = value;
        }


        private java.time.LocalDateTime _productionTime;

    
        @PropMeta(propId=14)
    
        public java.time.LocalDateTime getProductionTime(){
            return _productionTime;
        }

        public void setProductionTime(java.time.LocalDateTime value){
            this._productionTime = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=15)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _lotStatus;

    
        @PropMeta(propId=16)
    
        public String getLotStatus(){
            return _lotStatus;
        }

        public void setLotStatus(String value){
            this._lotStatus = value;
        }


        private Boolean _isInputConsumed;

    
        @PropMeta(propId=17)
    
        public Boolean getIsInputConsumed(){
            return _isInputConsumed;
        }

        public void setIsInputConsumed(Boolean value){
            this._isInputConsumed = value;
        }


        private String _remark;

    
        @PropMeta(propId=18)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=19)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
