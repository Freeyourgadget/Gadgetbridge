package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class CustomWidget {
    private HashMap<String, CustomWidgetElement> elements = new HashMap<>();
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
        return this.elements.values();
    }

    public void addElement(CustomWidgetElement element){
        this.elements.put(element.getId(), element);
    }

    public CustomWidgetElement getElement(String id){
        return elements.get(id);
    }

    public CustomWidgetElement removeElement(String id){
        return elements.remove(id);
    }
}
