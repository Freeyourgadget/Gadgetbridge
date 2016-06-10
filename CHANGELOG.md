###Changelog
####Version 0.10.1
* Pebble: set extended music info by dissecting notifications on Android 5.0+
* Pebble: allow ignoring activity trackers indiviually (to keep the data on the pebble)
* Mi Band: support for shifting the device time by N hours (for people who sleep at daytime)
* Mi Band: initial and untested support for Mi Band 2
* Allow setting the application language

####Version 0.10.0
* Pebble: option to send sunrise and sunset events to timeline
* Pebble: fix problems with unknown app keys while configuring watchfaces
* Mi Band: BLE connection fixes
* Fixes for enabling logging at whithout restarting Gadgetbridge
* Re-enable device paring activity on Android 6 (BLE scanning needs the location preference)
* Display device address in device info

####Version 0.9.8
* Pebble: fix more reconnnect issues
* Pebble: fix deep sleep not being detected with Firmware 3.12 when using Pebble Health
* Pebble: option in AppManager to delete files from cache
* Pebble: enable pbw cache and watchface configuration for Firmware 2.x
* Pebble: allow enabling of Pebble Health without "untested features" being enabled
* Pebble: fix music information being messed up
* Honour "Do Not Disturb" for phone calls and SMS

####Version 0.9.7
* Pebble: hopefully fix some reconnect issues
* Mi Band: fix live activity monitoring running forever if back button pressed
* Mi Band: allow low latency firmware updates, fixes update with some phones
* Mi Band: inital experimental and probably broken support for Amazfit
* Show aliases for BT Devices if they had been renamed in BT Settings
* Do not show a hint about App Manager when a Mi Band is connected

####Version 0.9.6
* Again some UI/theme improvements
* New preference to reconnect after connection loss (defaults to true)
* Fix crash when dealing with certain old preference values
* Mi Band: automatically reconnect when back in range after connection loss
* Mi Band 1S: display heart rate value again when invoked via the Debug view

####Version 0.9.5
* Several UI Improvements
* Easier First-time setup by using a FAB
* Optional Dark Theme
* Notification App Blacklist is now sorted
* Gadgetbridge Icon in the notification bar displays connection state
* Logging is now configurable without restart
* Mi Band 1S: Initial live heartrate tracking
* Fix certain crash in charts activity on slower devices (#277)

####Version 0.9.4
* Pebble: support pebble health datalog messages of firmware 3.11 (this adds support for deep sleep!)
* Pebble: try to reconnect on new notifications and phone calls when connection was lost unexpectedly
* Pebble: delay between reconnection attempts (from 1 up to 64 seconds)
* Fix crash in charts activities when changing the date, quickly (#277)
* Mi Band: preference to enable heart rate measurement during sleep (#232, thanks computerlyrik!)
* Mi Band: display measured heart rate in charts (#232)
* Mi Band 1S: full support for firmware upgrade/downgrade (both for Mi Band and heart rate sensor) (#234)
* Mi Band 1S: fix device detection for certain versions

####Version 0.9.3
* Pebble: Fix Pebble Health activation (was not available in the App Manager)
* Simplify connection state display (only connecting->connected)
* Small improvements to the pairing activity
* Mi Band 1S: Fix for mi band firmware update

####Version 0.9.2
* Mi Band: Fix update of second (HR) firmware on Mi1S (#234)
* Fix ordering issue of device infos being displayed

####Version 0.9.1
* Mi Band: fix sporadic connection problems (stuck on "Initializing" #249)
* Mi Band: enable low latency connection (faster) during initialization and activity sync
* Mi Band: better feedback for firmware update
* Device Item is now clickable also when the information entries are visible
* Fix enabling log file writing #261

####Version 0.9.0
* Pebble: Support for configuring watchfaces/apps locally (clay) or though webbrowser (some do not work)
* Pebble: hide the alarm management activity as it's unsupported
* Mi Band: Improve firmware detection and updates, including 1S support
* Mi Band: Display HR FW for 1S
* FW and HW versions are only displayed after tapping on the "info" button in Control Center
* Do not display activity samples when navigating too far in the past
* Fix auto connect which was broken under some circumstances

####Version 0.8.2
* Fix database creation and updates (thanks @feclare)
* Add experimental widget to set the alarm time to a configurable number of hours in the future (thanks @0nse)
* Use ckChangeLog to display the Changelog within Gadgetbridge
* Workaround to fix logfile rotation (bug in logback-android)

####Version 0.8.1
* Pebble: install (and start) freshly-installed apps on the watch instead of showing a Toast that tells the user to do so. (only applies to firmware 3.x)
* Pebble: fix crash while receiving Health data
* Mi Band 1S: support for synchronizing activity data (#205)
* Mi Band 1S: support for reading the heart rate via the "Debug Screen" #178

####Version 0.8.0
* Pebble: Support Pebble Health: steps/activity data are stored correctly. Sleep time is considered as light sleep. Deep sleep is discarded. The pebble will send data where it seems appropriate, there is no action to perform on the watch for this to happen.
* Pebble: Fix support for newer version of morpheuz (>=3.3?)
* Pebble: Allow to select the preferred activity tracker via settings activity (Health, Misfit, Morpheuz)
* Pebble: Fix wrong(previous) contact being displayed on the pebble 
* Mi Band: improvements to pairing and connecting
* Fix a problem related to shared preferences storage of activity settings
* Very basic support Android 6 runtime permission
* Fix layout of the alarms activity

####Version 0.7.4
* Refactored the settings activity: User details are now generic instead of miband specific. Old settings are preserved.
* Pebble: Fix regression with broken active reconnect since 0.7.0
* Pebble: Support activation and deactivation of Pebble Health. Activation uses the User details as seen above. Insigths are NOT activated.
  Please be aware that deactivation does NOT delete the data stored on the watch (but it seems to stop the tracking), and we do not know how to switch to metric length units.

####Version 0.7.3
* Pebble: Report connection state to PebbleKit companion apps via content provider. NOTE: Makes Gadgetbridge mutual exclusive with the original Pebble app.
* Ignore generic notification when from SMSSecure when SMS Notifications are on

####Version 0.7.2
* Pebble: Allow replying to generic notifications that contain a wearable reply action (tested with Signal)
* Pebble: Support seting up a common suffix for canned replies (defaults to " (canned reply)")
* Mi Band: Avoid NPEs when aborting an erroneous sync #205
* Mi Band: Fix discovery of Mi Band 1S
* Add a confirmation dialog when performing a db import
* Sort blacklist by package names

####Version 0.7.1
* Pebble: allow reinstallation of apps in pbw-cache from App Manager (long press menu)
* Pebble: Fix regression which freezes Gadgetbridge when disconnecting via long-press menu

####Version 0.7.0
* Read upcoming events (up to 7 days in the future). Requires READ_CALENDAR permission 
* Fix double SMS on Sony Android and Android 6.0
* Pebble: Support replying to SMS form the watch (canned replies)
* Pebble: Allow installing apps compiled with SDK 2.x also on the basalt platform (Time, Time Steel)
* Pebble: Fix decoding strings in appmessages from the pebble (fixes sending SMS from "Dialer for Pebble")
* Pebble: Support incoming reconnections when device returns from "Airplane Mode" or "Stand-By Mode"
* Pebble: Fix crash when turning off bluetooth when connected on Android 6.0
* Mi Band: reserve some alarm slots for alerting when upcoming events begin. NB: the band will vibrate at the start time of the event, android reminders are ignored
* Mi Band: Display unique devices Names, not just "MI"
* Some new and updated icons

####Version 0.6.9
* Pebble: Store app details in pbw-cache and display them in app manager on firmware 3.x 
* Pebble: Increase maximum notification body length from 255 to 512 bytes on firmware 3.x
* Pebble: Support installing .pbl (language files) on firmware 3.x
* Pebble: Correct setting the timezone on firmware 3.x (pebble expects the "ID" eg. Europe/Berlin)
* Pebble: Show correct icon for activity tracker and watchfaces in app installer (language and fw icons still missing)
* Pebble: Fix crash when trying to install files though a file manager which are located inside the pbw-cache on firmware 3.x
* Support for deleting all activity data (in the 'Debug' screen)
* Don't pop up the virtual keyboard when entering the Debug screen
* Remove all pending notifications on quit
* Mi Band: KitKat: hopefully fixed showing the progress bar during activity data synchronization (#155)
* Mi Band 1S: hopefully fixed connection errors (#178) Notifications probably do not work yet, though

####Version 0.6.8
* Mi Band: support for Firmware upgrade/downgrade on Mi Band 1A (white LEDs, no heartrate sensor)
* Pebble: fix regression in 0.6.7 when installing pbw/pbz files from content providers (eg. download manager)
* Pebble: fix installation of pbw files on firmware 3.x when using content providers (eg. download manager)   
* Pebble: fix crash on firmware 3.x when pebble requests a pbw that is not in Gadgetbridge's cache 
+ Treat Signal notifications as chat notifications
* Fix crash when contacts cannot be read on Android 6.0 (non-granted pemissions)

####Version 0.6.7
* Pebble: Allow installation of 3.x apps on OG Pebble (FW will be released soon)
* Fix crashes on startup when logging is enabled or when entering the app manager on some phones
+ Fix Pebble being detected as MI when unpaired and autoconnect is enabled
* Fix Crash when not having K9 Mail permissions (happens when installing K9 after Gadgetbridge) (#175)

####Version 0.6.6
* Mi Band: Huge performance improvement fetching activity data
* Mi Band: attempt at fixing connection problems (#156)
* Pebble: Try to interpret sleep data from Misfit data
* Fix exporting the activity database on devices with read-only external storage (#153)
* Fix totally wrong sleep time in the sleep chart

####Version 0.6.5
* Mi Band: Support "Locate Device" with Mi Band 1A (and Mi Band 1 with new firmware)
* Pebble: Support syncing steps from Misfit (untested features must be turned on to see them), intensity=steps, no sleep support yet
* Disable activity fetching when not supported
* Small improvements to live activity charts

####Version 0.6.4
* Support pull down to synchronize activity data (#138)
* Display tabs in the Charts activity (#139)
* Mi Band: initial support for Mi Band 1a (the one with white LEDs) (thanks @sarg) (#136)
* Mi Band: Attempt at fixing problem with never finishing activity data fetching (#141, #142)
* Register/unregister BroadcastReceivers instead of enabling/disabling them with PackageManager (#134)
  (should fix disconnection because the service is being killed)

####Version 0.6.3
* Pebble: support installation of language files (.pbl) on FW 2.x
* Try to prevent service being killed by disallowing backups

####Version 0.6.2
* Mi Band: support firmare versione 1.0.10.14 (and onwards?) vibration
* Mi Band: get device name from official BT SIG endpoint
* Mi Band: initial support for displaying live activity data, screen stays on

####Version 0.6.1
* Pebble: Allow muting (blacklisting) Apps from within generic notifications on the watch
* Pebble: Detect all known Pebble Versions including new "chalk" platform (Pebble Time Round)
* Option to ignore phone calls (useful for Pebble Dialer)
* Mi Band: Added progressbar for activity data transfer and fixes for firmware transfer progressbar
* Bugfix for app blacklist (some checkboxes where wrongly drawn as checked)

####Version 0.6.0
* Pebble: WIP implementantion of PebbleKit Intents to make some 3rd party Android apps work with the Pebble (eg. Ventoo)
* Pebble: Option to set reconnection attempts in settings (one attempt usually takes about 5 seconds)
* Support contolling all audio players that react to media buttons (can be chosen in settings)
* Treat SMS as generic notification if set to "never" (can be blacklisted there also if desired)
* Treat Conversations messagess as chat messages, even if arrived via Pebble Intents (nice icon for Pebble FW 3.x)
* Allow opening firmware / app files from the download manager "app" (technically a content provider)
* Mi Band: whitelisted a few firmware versions

####Version 0.5.4
* Mi Band: allow the transfer of activity data without clearing MiBand's memory
* Pebble: for generic notifications use generic icon instead of SMS icons on FW 3.x (thanks @roidelapluie)
* Pebble: use different icons and background colors for specific groups of applications (chat, mail, etc) (thanks @roidelapluie)
* In settings, support blacklisting apps for generic notifications

####Version 0.5.3
* Pebble: For generic notifications, support dismissing individual notifications and "Open on Phone" feature (OG & PT)
* Pebble: Allow to treat K9 notifications as generic notifications (if notification mode is set to never)
* Ignore QKSMS notifications to avoid double notification for incoming SMS
* Improved UI of Firmware/App installer
* Device state again visible on lockscreen
* Date display and navigation now working properly for all charts

####Version 0.5.2
* Pebble: support "dismiss all" action also on Pebble Time/FW 3.x notifications
* Mi Band: show a notification when the battery is below 10%
* Graphs are now using the same theme as the rest of the application
* Graphs now show when the device was not worn by the user (for devices that send this information)
* Remove unused settings option in charts view
* Build target is now Android SDK 23 (Marshmellow)

####Version 0.5.1
* Pebble: support taking screenshot from Pebble Time
* Fix broken "find lost device" which was broken in 0.5.0

####Version 0.5.0
* Mi Band: fix setting wear location
* Pebble: experimental watchapp installation support for FW 3.x/Pebble Time
* Pebble: support Pebble emulator via TCP connection (needs rebuild with INTERNET permission)
* Pebble: use SMS/EMAIL icons for FW 3.x/Pebble Time
* Pebble: do not throttle notifications
* Support going forward/backwards in time in the activity charts
* Various small bugfixes to the App/Fw Installation Activity

####Version 0.4.6
* Mi Band: Fixed negative number of steps displayed (#91)
* Mi Band: fixed (re-) connection problems after band getting disconnected
* Pebble: new option to enable untested code (enable only if you like bad surprises)
* Pebble: always enable 2.x notifications with "dismiss all" action on FW 2.x (except for K9)
* Fixed slight steps graph distortion through black text labels
* Fixed control center activity and notification showing different device connection state
* Small firmware installation improvements
* Various refactorings and code cleanups

####Version 0.4.5
* Enhancement to activity graphs: new graph showing the number of steps done today and in the last week
* New preference to set the desired fitness goal (number of steps to walk in one day)
* Mi Band: support for setting the fitness goal (the band will show the progress to the goal with the leds and vibrates when the goal is reached)
* Mi Band: send the wear location (left / right hand) to the device
* Mi Band: support for flashing firmware from .fw files (upgrades and downgrades are possible)
* Fixed crash when synchronizing activity data in the graphs activity and changing device orientation

####Version 0.4.4
* Set GadgetBridge notification visibility to public, to show the connection status on the lockscreen
* Support for backup up and restoring of the activity database (via Debug activity)
* Support for graceful upgrades and downgrades, keeping your activity database intact
* Enhancement to activity graphs: new graphs for sleep data (only last night) accessible swiping right from the main graph
* Enhancement to graphs activity: it is now possible to fetch the activity data directly from this activity
* Pebble: experimental support for dismissing (all) notifications via actionable notifications (disabled by default)
* Pebble: make FW 3.x notifications available by default
* Mi Band: Set the graphs activity as the default action available with a single tap on the connected device

####Version 0.4.3
* Mi Band: Support for setting alarms
* Mi Band: Bugfix for activity data synchronization

####Version 0.4.2
* Material style for Lollipop
* Support for finding a lost device (vibrate until cancelled)
* Mi Band: Support for vibration profiles, configurable for notifications
* Pebble: Support taking screenshots from the device context menu (Pebble Time not supported yet)

####Version 0.4.1
* New icons, thanks xphnx!
* Improvements to Sleep Monitor charts
* Pebble: use new Sleep Monitor for Morpheuz (previously Mi Band only)
* Pebble: experimental support for FW 3.x notification protocol
* Pebble: dev option to force latest notification protocol

####Version 0.4.0
* Pebble: Initial Morpheuz protocol support for getting sleep data
* Pebble: Support launching of watchapps though the AppManager Activity
* Pebble: Support CM 12.1 default music app (Eleven)
* Pebble: Fix firmware installation when all 8 app slots are in use
* Pebble: Fix firmware installation when Pebble is in recovery mode
* Pebble: Fix error when reinstalling apps, useful for upgrading/downgrading
* Mi Band: Make vibration count configurable for different kinds of Notifications
* Mi Band: Initial support for fetching activity data
* Support rebooting Mi Band/Pebble through the Debug Activity
* Add highly experimental sleep monitor (accessible via long press on a device)
* Fix Debug activity (SMS and E-Mail buttons were broken)
* Add Turkish translation contributed by Tarik Sekmen

####Version 0.3.5
* Add discovery and pairing Activity for Pebble and Mi Band
* Listen for Pebble Message Intents and forward notifications (used by Conversations)
* Make strings translatable and add German, Italian, Russian, Spanish and Korean translations
* Mi Band: Display battery status

####Version 0.3.4
* Pebble: Huge speedup for app/firmware installation.
* Pebble: Use a separate notification with progress bar for installation procedure
* Pebble: Bugfix for being stuck while waiting for a slot, when none is available
* Mi Band: Display connection status in notification (previously Pebble only)

####Version 0.3.3
* Pebble: Try to reduce battery usage by acknowledging datalog packets
* Mi Band: Set current time on the device (thanks to PR by @danielegobbetti)
* More robust connection state handling and display

####Version 0.3.2
* Mi Band: Fix for notifications only working after manual connection
* Mi Band: Display firmware version
* Pebble: Display hardware revision
* Pebble: Check if firmware is compatible before allowing installation

####Version 0.3.1
* Mi Band: Fix for notifications only working in Debug

####Version 0.3.0
* Mi Band: Initial support (see README.md)
* Pebble: Firmware installation (USE AT YOUR OWN RISK)
* Pebble: Fix installation problems with certain .pbw files
* Pebble: Volume control
* Add icon for activity tracker apps (icon by xphnx)
* Let the application quit when in reconnecting state

####Version 0.2.0
* Experimental pbw installation support (watchfaces/apps)
* New icons for device and app lists
* Fix for device list not refreshing when bluetooth gets turned on
* Filter out annoying low battery notifications
* Fix for crash on some devices when creating a debug notification
* Lots of internal changes preparing multi device support

####Version 0.1.5
* Fix for DST (summer time)
* Option to sync time on connect (enabled by default)
* Opening .pbw files with Gadgetbridge prints some package information
  (This was not meant to be released yet, but the DST fix made a new release necessary)

####Version 0.1.4
* New AppManager shows installed Apps/Watchfaces (removal possible via context menu)
* Allow back navigation in ActionBar (Debug and AppMananger Activities)
* Make sure Intent broadcasts do not leave Gadgetbridge
* Show hint in the Main Activity (tap to connect etc)

####Version 0.1.3
* Remove the connect button, list all supported devices and connect on tap instead
* Display connection status and firmware of connected devices in the device list
* Remove quit button from the service notification, put a quit item in the context menu instead

####Version 0.1.2
* Added option to start Gadgetbridge and connect automatically when bluetooth is turned on
* stop service if bluetooth is turned off
* try to reconnect if connection was lost

####Version 0.1.1
* Fixed various bugs regarding K-9 Mail notifications.
* "Generic notification support" in Setting now opens Androids "Notification access" dialog.

####Version 0.1.0
* Initial release
