package net.osmand.aidlapi;

import net.osmand.aidlapi.navigation.ANavigationUpdateParams;
import net.osmand.aidlapi.navigation.ANavigationVoiceRouterMessageParams;
import net.osmand.aidlapi.IOsmAndAidlCallback;

interface IOsmAndAidlInterface {
    /**
     * Method to register for updates during navgation. Notifies user about distance to the next turn and its type.
     *
     * @param subscribeToUpdates (boolean) - subscribe or unsubscribe from updates
     * @param callbackId (long) - id of callback, needed to unsubscribe from updates
     * @param callback (IOsmAndAidlCallback) - callback to notify user on navigation data change
     */
    long registerForNavigationUpdates(in ANavigationUpdateParams params, IOsmAndAidlCallback callback) = 65;

    /**
     * Method to register for Voice Router voice messages during navigation. Notifies user about voice messages.
     *
     * @params subscribeToUpdates (boolean) - boolean flag to subscribe or unsubscribe from messages
     * @params callbackId (long) - id of callback, needed to unsubscribe from messages
     * @params callback (IOsmAndAidlCallback) - callback to notify user on voice message
     */
    long registerForVoiceRouterMessages(in ANavigationVoiceRouterMessageParams params, IOsmAndAidlCallback callback) = 71;
}
