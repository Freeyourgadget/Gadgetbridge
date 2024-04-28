package nodomain.freeyourgadget.gadgetbridge.util;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GBToStringBuilder extends ToStringBuilder {
    public static final GBToStringStyle STYLE = new GBToStringStyle();

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    public GBToStringBuilder(final Object object) {
        super(object, STYLE);
    }

    public static class GBToStringStyle extends ToStringStyle {
        public GBToStringStyle() {
            super();

            this.setUseShortClassName(true);
            this.setUseIdentityHashCode(false);

            this.setContentStart("{");
            this.setContentEnd("}");

            this.setArrayStart("[");
            this.setArrayEnd("]");

            this.setFieldSeparator(", ");
            this.setFieldNameValueSeparator("=");

            this.setNullText("null");
        }

        @Override
        public void append(final StringBuffer buffer, final String fieldName, final Object value, final Boolean fullDetail) {
            // omit nulls
            if (value != null) {
                if (value instanceof Date) {
                    super.append(buffer, fieldName, SDF.format(value), fullDetail);
                } else {
                    super.append(buffer, fieldName, value, fullDetail);
                }
            }
        }
    }
}
