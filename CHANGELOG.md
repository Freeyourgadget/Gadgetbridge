###Changelog
####Version (next)
* Add discovery and pairing Activity for Mi Bands
* Listen for Pebble Message intents and forward notification (used by Conversations)
* Make strings translatable and add German, Italian, Spanish and Korean translations
* Mi Band: Display battery status
 
####Version 0.3.4
* Pebble: Huge speedup for app/firmware installation.
* Pebble: Use a separate notification with progress bar for installation procedure
* Pebble: Bugfix for beeing stuck while waiting for a slot, when none is available
* Mi Band: Display connection status in notification (previously Pebble only)

####Version 0.3.3
* Pebble: Try to reduce battery usage by acknowledging datalog packets
* Mi Band: Set current time on the device (thanks to PR by @danielegobbetti)
* General: More robust connection state handling and display

####Version 0.3.2
* Mi Band: Fix for notifications only working after manual connection
* Mi Band: Display firmware version
* Pebble: Display hardware revision
* Pebble: Check if firmware is compatible before allowing installation

####Version 0.3.1
* Mi Band: Fix for notifications only woking in Debug

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
* Filter out annyoing low battery notifications
* Fix for crash on some devices when creating a debug notification
* Lots of internal changes preparing multi device support

####Version 0.1.5
* Fix for DST (summer time)
* Option to sync time on connect (enabled by default)
* Opening .pbw files with Gadgetbridge prints some package information
  (This was not meant to be released yet, but the DST fix made a new release neccessary)

####Version 0.1.4
* New AppManager shows installed Apps/Watchfaces (removal possible via context menu)
* Allow back navigation in ActionBar (Debug and AppMananger Activities)
* Make sure Intent broadcasts do not leave Gadgetbridge
* Show hint in the Main Activiy (tap to connect etc)

####Version 0.1.3
* Remove the connect button, list all suported devices and connect on tap instead
* Display connection status and firmware of connected devices in the device list
* Remove quit button from the service notification, put a quit item in the context menu instead

####Version 0.1.2
* Added option to start Gadgetbridge and connect automatically when bluetooth is turned on
* stop service if bluetooth is turned off
* try to reconnect if connection was lost

####Version 0.1.1
* Fixed various bugs regarding K-9 Mail notifications.
* "Generic notification support" in Setting now opens Androids "Notifcaion access" dialog.

####Version 0.1.0
* Initial release
