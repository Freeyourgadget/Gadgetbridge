###Changelog

####Version 0.2.0
* Experimental pbw installation support (watchfaces/apps)
* New icons for device and app lists
* Fix for device list not refreshing when bluetooth gets turned on
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
