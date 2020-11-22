package nodomain.freeyourgadget.gadgetbridge.adapter;

public class SpinnerWithIconItem {

    String text;
    Long id;
    int imageId;

    public SpinnerWithIconItem(String text, Long id, int imageId) {
        this.text = text;
        this.id = id;
        this.imageId = imageId;
    }

    public String getText() {
        return text;
    }

    public int getImageId() {
        return imageId;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return text + " " + id + " " + imageId;
    }

}
