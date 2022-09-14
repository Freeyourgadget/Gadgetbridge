package nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields;

public class Int64MessageField extends VarintMessageField{
    public Int64MessageField(int fieldNumber, int value){
        super(fieldNumber, value);
        this.fieldType = FieldType.INT_64_BIT;
    }
}
