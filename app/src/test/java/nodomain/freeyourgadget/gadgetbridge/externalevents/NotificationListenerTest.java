package nodomain.freeyourgadget.gadgetbridge.externalevents;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilter;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotificationListenerTest extends TestBase {

    private NotificationListener mNotificationListener;
    private List<String> wordList = Arrays.asList("Hello", "world", "test");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mNotificationListener = new NotificationListener();
    }

    @Test
    public void shouldContinueAfterFilter_TestBlacklistFindAnyWord_WordFound_MustReturnFalse() {
        String body = "Hello world this is a test";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_BLACKLIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ANY);
        assertFalse(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestWhitelistFindAnyWord_WordFound_MustReturnTrue() {
        String body = "Hello world this is a test";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_WHITELIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ANY);
        assertTrue(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestBlacklistFindAllWords_WordsFound_MustReturnFalse() {
        String body = "Hello world this is a test";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_BLACKLIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ALL);
        assertFalse(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestWhitelistFindAllWords_WordsFound_MustReturnTrue() {
        String body = "Hello world this is a test";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_WHITELIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ALL);
        assertTrue(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));
    }

    @Test
    public void shouldContinueAfterFilter_TestBlacklistFindAnyWord_WordNotFound_MustReturnTrue() {
        String body = "Hallo Welt das ist ein Versuch";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_BLACKLIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ANY);
        assertTrue(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestWhitelistFindAnyWord_WordNotFound_MustReturnFalse() {
        String body = "Hallo Welt das ist ein Versuch";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_WHITELIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ANY);
        assertFalse(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestBlacklistFindAllWords_WordNotFound_MustReturnTrue() {
        String body = "Hallo Welt das ist ein Versuch";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_BLACKLIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ALL);
        assertTrue(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));

    }

    @Test
    public void shouldContinueAfterFilter_TestWhitelistFindAllWords_WordNotFound_MustReturnFalse() {
        String body = "Hallo Welt das ist ein Versuch";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_WHITELIST);
        filter.setNotificationFilterSubMode(NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ALL);
        assertFalse(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));
    }

    @Test
    public void shouldContinueAfterFilter_TestFilterNone_MustReturnTrue() {
        String body = "A text without a meaning";
        NotificationFilter filter = new NotificationFilter();
        filter.setNotificationFilterMode(NotificationFilterActivity.NOTIFICATION_FILTER_MODE_NONE);
        assertTrue(mNotificationListener.shouldContinueAfterFilter(body, wordList, filter));
    }
}