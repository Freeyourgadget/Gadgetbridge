package nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields;

public class Int32MessageField extends VarintMessageField{
    public Int32MessageField(int fieldNumber, int value){
        super(fieldNumber, value);
        this.fieldType = FieldType.INT_32_BIT;
    }
}
