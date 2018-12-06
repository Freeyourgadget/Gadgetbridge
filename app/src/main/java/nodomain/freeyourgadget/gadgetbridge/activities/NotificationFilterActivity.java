package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import de.greenrobot.dao.query.Query;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.AppBlacklistAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilter;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterDao;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterEntry;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterEntryDao;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NotificationFilterActivity extends AbstractGBActivity {

    private static final String TAG = NotificationFilterActivity.class.getName();

    public static final int NOTIFICATION_FILTER_MODE_NONE = 0;
    public static final int NOTIFICATION_FILTER_MODE_WHITELIST = 1;
    public static final int NOTIFICATION_FILTER_MODE_BLACKLIST = 2;
    public static final int NOTIFICATION_FILTER_SUBMODE_ANY = 0;
    public static final int NOTIFICATION_FILTER_SUBMODE_ALL = 1;

    private Button mButtonSave;
    private Spinner mSpinnerFilterMode;
    private Spinner mSpinnerFilterSubMode;
    private NotificationFilter mNotificationFilter;
    private EditText mEditTextWords;
    private DBHandler db = null;
    private List<String> mWordsList = new ArrayList<>();
    private List<Long> mFilterEntryIds = new ArrayList<>();

    private static final Logger LOG = LoggerFactory.getLogger(NotificationFilterActivity.class);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_filter);

        String packageName = getIntent().getStringExtra(AppBlacklistAdapter.STRING_EXTRA_PACKAGE_NAME);

        if (StringUtils.isBlank(packageName)) {
            this.finish();
        }

        packageName = packageName.toLowerCase();

        try {
            db = GBApplication.acquireDB();
        } catch (GBException e) {
            LOG.error("Could not acquire DB.", e);
            this.finish();
        }

        NotificationFilterDao notificationFilterDao = db.getDaoSession().getNotificationFilterDao();
        NotificationFilterEntryDao notificationFilterEntryDao = db.getDaoSession().getNotificationFilterEntryDao();

        Query<NotificationFilter> query = notificationFilterDao.queryBuilder().where(NotificationFilterDao.Properties.AppIdentifier.eq(packageName)).build();
        mNotificationFilter = query.unique();

        if (mNotificationFilter == null) {
            mNotificationFilter = new NotificationFilter();
            mNotificationFilter.setAppIdentifier(packageName);
            LOG.debug("New Notification Filter");
        } else {
            LOG.debug("Loaded existing notification filter");
            Query<NotificationFilterEntry> queryEntries = notificationFilterEntryDao.queryBuilder().where(NotificationFilterEntryDao.Properties.NotificationFilterId.eq(mNotificationFilter.getId())).build();
            List<NotificationFilterEntry> filterEntries = queryEntries.list();
            if (!filterEntries.isEmpty()) {
                for (NotificationFilterEntry temp : filterEntries) {
                    mWordsList.add(temp.getNotificationFilterContent());
                    mFilterEntryIds.add(temp.getId());
                    LOG.debug("Loaded filter word: " + temp.getNotificationFilterContent());
                }
            }
        }

        setupView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (db != null) {
            GBApplication.releaseDB();
        }
    }

    private void setupView() {

        mSpinnerFilterMode = findViewById(R.id.spinnerFilterMode);
        mSpinnerFilterMode.setSelection(mNotificationFilter.getNotificationFilterMode());

        mSpinnerFilterSubMode = findViewById(R.id.spinnerSubMode);
        mSpinnerFilterMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                switch (pos) {
                    case NOTIFICATION_FILTER_MODE_NONE:
                        mEditTextWords.setEnabled(false);
                        mSpinnerFilterSubMode.setEnabled(false);
                        break;
                    case NOTIFICATION_FILTER_MODE_BLACKLIST:
                    case NOTIFICATION_FILTER_MODE_WHITELIST:
                        mEditTextWords.setEnabled(true);
                        mSpinnerFilterSubMode.setEnabled(true);
                        break;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mSpinnerFilterSubMode.setSelection(mNotificationFilter.getNotificationFilterSubMode());

        mEditTextWords = findViewById(R.id.editTextWords);

        if (!mWordsList.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String temp : mWordsList) {
                builder.append(temp);
                builder.append("\n");
            }
            mEditTextWords.setText(builder.toString());
        }

        mEditTextWords.setEnabled(mSpinnerFilterMode.getSelectedItemPosition() == NOTIFICATION_FILTER_MODE_NONE);

        mButtonSave = findViewById(R.id.buttonSaveFilter);
        mButtonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // TODO: check for modifications, only save if something changed
                String words = mEditTextWords.getText().toString();

                if (StringUtils.isBlank(words) && mSpinnerFilterMode.getSelectedItemPosition() != NOTIFICATION_FILTER_MODE_NONE) {
                    Toast.makeText(NotificationFilterActivity.this, R.string.toast_notification_filter_words_empty_hint, Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    db = GBApplication.acquireDB();
                    NotificationFilterDao notificationFilterDao = db.getDaoSession().getNotificationFilterDao();
                    NotificationFilterEntryDao notificationFilterEntryDao = db.getDaoSession().getNotificationFilterEntryDao();

                    debugOutput(notificationFilterDao);

                    mNotificationFilter.setNotificationFilterMode(mSpinnerFilterMode.getSelectedItemPosition());
                    mNotificationFilter.setNotificationFilterSubMode(mSpinnerFilterSubMode.getSelectedItemPosition());

                    notificationFilterEntryDao.deleteByKeyInTx(mFilterEntryIds);

                    Long filterId = notificationFilterDao.insertOrReplace(mNotificationFilter);

                    // only save words if filter mode != none
                    if (mNotificationFilter.getNotificationFilterMode() != NOTIFICATION_FILTER_MODE_NONE) {
                        String[] wordsSplitted = words.split("\n");
                        for (String temp : wordsSplitted) {

                            if (StringUtils.isBlank(temp)) {
                                continue;
                            }

                            temp = temp.trim();
                            NotificationFilterEntry notificationFilterEntry = new NotificationFilterEntry();
                            notificationFilterEntry.setNotificationFilterContent(temp);
                            notificationFilterEntry.setNotificationFilterId(filterId);
                            notificationFilterEntryDao.insert(notificationFilterEntry);
                        }
                    }

                    Toast.makeText(NotificationFilterActivity.this, R.string.toast_notification_filter_saved_successfully, Toast.LENGTH_SHORT).show();
                    NotificationFilterActivity.this.finish();

                } catch (GBException e) {
                    LOG.error("Could not acquire DB.", e);
                    Toast.makeText(NotificationFilterActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void debugOutput(NotificationFilterDao notificationFilterDao) {
        if (BuildConfig.DEBUG) {

            List<NotificationFilter> filters = notificationFilterDao.loadAll();

            LOG.info(TAG, "Saved filters");

            for (NotificationFilter temp : filters) {
                LOG.info(TAG, "Filter: " + temp.getId() + " " + temp.getAppIdentifier());
            }
        }
    }
}
