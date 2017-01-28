package nodomain.freeyourgadget.gadgetbridge.util;

public class JavaExtensions {

    /**
     * Equivalent c# '??' operator
     * @param one first value
     * @param two second value
     * @return first if not null, or second if first is null
     */
    public static <T> T coalesce(T one, T two)
    {
        return one != null ? one : two;
    }
}
