package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class AppBlacklistAdapter extends RecyclerView.Adapter<AppBlacklistAdapter.AppBLViewHolder> {

    private final List<ApplicationInfo> applicationInfoList;
    private final int mLayoutId;
    private final Context mContext;
    private final PackageManager mPm;
    private final IdentityHashMap<ApplicationInfo, String> mNameMap;


    public AppBlacklistAdapter(int layoutId, Context context) {
        mLayoutId = layoutId;
        mContext = context;
        mPm = context.getPackageManager();

        applicationInfoList = mPm.getInstalledApplications(PackageManager.GET_META_DATA);

        // sort the package list by label and blacklist status
        mNameMap = new IdentityHashMap<ApplicationInfo, String>(applicationInfoList.size());
        for (ApplicationInfo ai : applicationInfoList) {
            CharSequence name = mPm.getApplicationLabel(ai);
            if (name == null) {
                name = ai.packageName;
            }
            if (GBApplication.blacklist.contains(ai.packageName)) {
                // sort blacklisted first by prefixing with a '!'
                name = "!" + name;
            }
            mNameMap.put(ai, name.toString());
        }

        Collections.sort(applicationInfoList, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo ai1, ApplicationInfo ai2) {
                final String s1 = mNameMap.get(ai1);
                final String s2 = mNameMap.get(ai2);
                return s1.compareTo(s2);
            }
        });

    }

    @Override
    public AppBlacklistAdapter.AppBLViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);
        return new AppBLViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AppBlacklistAdapter.AppBLViewHolder holder, int position) {
        final ApplicationInfo appInfo = applicationInfoList.get(position);

        holder.deviceAppVersionAuthorLabel.setText(appInfo.packageName);
        holder.deviceAppNameLabel.setText(mNameMap.get(appInfo));
        holder.deviceImageView.setImageDrawable(appInfo.loadIcon(mPm));

        holder.checkbox.setChecked(GBApplication.blacklist.contains(appInfo.packageName));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = ((CheckBox) v.findViewById(R.id.item_checkbox));
                checkBox.toggle();
                if (checkBox.isChecked()) {
                    GBApplication.addToBlacklist(appInfo.packageName);
                } else {
                    GBApplication.removeFromBlacklist(appInfo.packageName);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return applicationInfoList.size();
    }

    public class AppBLViewHolder extends RecyclerView.ViewHolder {

        final CheckBox checkbox;
        final ImageView deviceImageView;
        final TextView deviceAppVersionAuthorLabel;
        final TextView deviceAppNameLabel;

        AppBLViewHolder(View itemView) {
            super(itemView);

            checkbox = (CheckBox) itemView.findViewById(R.id.item_checkbox);
            deviceImageView = (ImageView) itemView.findViewById(R.id.item_image);
            deviceAppVersionAuthorLabel = (TextView) itemView.findViewById(R.id.item_details);
            deviceAppNameLabel = (TextView) itemView.findViewById(R.id.item_name);
        }

    }

}
