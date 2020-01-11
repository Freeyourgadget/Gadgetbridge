package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class CustomWidget {
    private ArrayList<CustomWidgetElement> elements = new ArrayList<>();
    private int angle, distance;

    public CustomWidget(int angle, int distance) {
        this.angle = angle;
        this.distance = distance;
    }

    public int getAngle() {
        return angle;
    }

    public int getDistance() {
        return distance;
    }

    public Collection<CustomWidgetElement> getElements(){
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
}
