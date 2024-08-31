package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Copied as-is from <a href="https://github.com/ByteHamster/SearchPreference/blob/0c5669a6423292f1653abd9259a4040dad96f84e/lib/src/main/java/com/bytehamster/lib/preferencesearch/SearchPreferenceResult.java">SearchPreferenceResult</a>, since the constructor is protected, and
 * we need a way to highlight preferences in a different activity due to the way Gadgetbridge is built.
 */
public class SearchPreferenceHighlighter {
    public static void highlight(final PreferenceFragmentCompat prefsFragment, final String key) {
        new Handler().post(() -> doHighlight(prefsFragment, key));
    }

    private static void doHighlight(final PreferenceFragmentCompat prefsFragment, final String key) {
        final Preference prefResult = prefsFragment.findPreference(key);

        if (prefResult == null) {
            Log.e("doHighlight", "Preference not found on given screen");
            return;
        }
        final RecyclerView recyclerView = prefsFragment.getListView();
        final RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
        if (adapter instanceof PreferenceGroup.PreferencePositionCallback) {
            PreferenceGroup.PreferencePositionCallback callback = (PreferenceGroup.PreferencePositionCallback) adapter;
            final int position = callback.getPreferenceAdapterPosition(prefResult);
            if (position != RecyclerView.NO_POSITION) {
                recyclerView.scrollToPosition(position);
                recyclerView.postDelayed(() -> {
                    RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
                    if (holder != null) {
                        Drawable oldBackground = holder.itemView.getBackground();
                        int color = getColorFromAttr(prefsFragment.getContext(), android.R.attr.textColorPrimary);
                        holder.itemView.setBackgroundColor(color & 0xffffff | 0x33000000);
                        new Handler().postDelayed(() -> holder.itemView.setBackgroundDrawable(oldBackground), 1000);
                        return;
                    }
                    highlightFallback(prefsFragment, prefResult);
                }, 200);
                return;
            }
        }
        highlightFallback(prefsFragment, prefResult);
    }

    /**
     * Alternative highlight method if accessing the view did not work
     */
    private static void highlightFallback(PreferenceFragmentCompat prefsFragment, final Preference prefResult) {
        final Drawable oldIcon = prefResult.getIcon();
        final boolean oldSpaceReserved = prefResult.isIconSpaceReserved();
        Drawable arrow = AppCompatResources.getDrawable(prefsFragment.getContext(), com.bytehamster.lib.preferencesearch.R.drawable.searchpreference_ic_arrow_right);
        int color = getColorFromAttr(prefsFragment.getContext(), android.R.attr.textColorPrimary);
        arrow.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        prefResult.setIcon(arrow);
        prefsFragment.scrollToPreference(prefResult);
        new Handler().postDelayed(() -> {
            prefResult.setIcon(oldIcon);
            prefResult.setIconSpaceReserved(oldSpaceReserved);
        }, 1000);
    }

    private static int getColorFromAttr(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        TypedArray arr = context.obtainStyledAttributes(typedValue.data, new int[]{
                android.R.attr.textColorPrimary});
        int color = arr.getColor(0, 0xff3F51B5);
        arr.recycle();
        return color;
    }
}
