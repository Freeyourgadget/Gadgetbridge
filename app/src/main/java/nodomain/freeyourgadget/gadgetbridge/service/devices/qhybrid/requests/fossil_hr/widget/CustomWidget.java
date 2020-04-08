package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class CustomWidget extends Widget {
    private ArrayList<CustomWidgetElement> elements = new ArrayList<>();
    private int angle, distance;
    private String name;

    public CustomWidget(String name, int angle, int distance, String fontColor) {
        super(null, angle, distance, fontColor);
        this.angle = angle;
        this.distance = distance;
        this.name = name;
    }

    public void setElements(ArrayList<CustomWidgetElement> elements) {
        this.elements = elements;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<CustomWidgetElement> getElements(){
        return this.elements;
    }

    public void addElement(CustomWidgetElement element){
        this.elements.add(element);
    }

    public boolean updateElementValue(String id, String value){
        boolean updatedValue = false;
        for(CustomWidgetElement element : this.elements){
            String elementId = element.getId();
            if(elementId != null && elementId.equals(id)){
                element.setValue(value);
                updatedValue = true;
            }
        }
        return updatedValue;
    }

    private CustomWidgetElement getElement(String id){
        for(CustomWidgetElement element : this.elements){
            String elementId = element.getId();
            if(elementId != null && elementId.equals(id)) return element;
        }
        return null;
    }

    public String getName() {
        return name;
    }
}
