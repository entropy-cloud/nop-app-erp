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

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _workOrderId;

    
        @PropMeta(propId=2)
    
        public String getWorkOrderId(){
            return _workOrderId;
        }

        public void setWorkOrderId(String value){
            this._workOrderId = value;
        }


        private String _jobCardId;

    
        @PropMeta(propId=3)
    
        public String getJobCardId(){
            return _jobCardId;
        }

        public void setJobCardId(String value){
            this._jobCardId = value;
        }


        private String _operationId;

    
        @PropMeta(propId=4)
    
        public String getOperationId(){
            return _operationId;
        }

        public void setOperationId(String value){
            this._operationId = value;
        }


        private String _inputLotId;

    
        @PropMeta(propId=5)
    
        public String getInputLotId(){
            return _inputLotId;
        }

        public void setInputLotId(String value){
            this._inputLotId = value;
        }


        private String _inputMaterialId;

    
        @PropMeta(propId=6)
    
        public String getInputMaterialId(){
            return _inputMaterialId;
        }

        public void setInputMaterialId(String value){
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


        private String _inputUoMId;

    
        @PropMeta(propId=8)
    
        public String getInputUoMId(){
            return _inputUoMId;
        }

        public void setInputUoMId(String value){
            this._inputUoMId = value;
        }


        private String _outputLotId;

    
        @PropMeta(propId=9)
    
        public String getOutputLotId(){
            return _outputLotId;
        }

        public void setOutputLotId(String value){
            this._outputLotId = value;
        }


        private String _outputMaterialId;

    
        @PropMeta(propId=10)
    
        public String getOutputMaterialId(){
            return _outputMaterialId;
        }

        public void setOutputMaterialId(String value){
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


        private String _outputUoMId;

    
        @PropMeta(propId=12)
    
        public String getOutputUoMId(){
            return _outputUoMId;
        }

        public void setOutputUoMId(String value){
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


        private java.sql.Timestamp _productionTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getProductionTime(){
            return _productionTime;
        }

        public void setProductionTime(java.sql.Timestamp value){
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


    }
