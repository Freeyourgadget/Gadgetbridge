Gadgetbridge
============

Gadgetbridge is an Android (4.4+) Application which will allow you to use your
Pebble or Mi Band without the vendor's closed source application and without the
need to create an account and transmit any of your data to the vendor's servers.

[![Build](https://travis-ci.org/Freeyourgadget/Gadgetbridge.svg?branch=master)](https://travis-ci.org/Freeyourgadget/Gadgetbridge)

## Download

[![Gadgetbridge on F-Droid](/Get_it_on_F-Droid.svg.png?raw=true "Download from F-Droid")](https://f-droid.org/repository/browse/?fdid=nodomain.freeyourgadget.gadgetbridge)

[List of changes](CHANGELOG.md)

## Features (Pebble)

* Incoming calls notification and display (caller, phone number)
* Outgoing call display
* Reject/hangup calls
* SMS notification (sender, body)
* K-9 Mail notification support (sender, subject, preview)
* Support for generic notifications (above filtered out)
* Dismiss individial notifications or open corresponding app on phone from the action menu (generic notifications)
* Dismiss all notifications from the action menu (non-generic notifications) 
* Music playback info (artist, album, track)
* Music control: play/pause, next track, previous track, volume up, volume down
* List and remove installed apps/watchfaces
* Install watchfaces and watchapps (.pbw)
* Install firwmare files (.pbz) [READ THE WIKI](https://github.com/Freeyourgadget/Gadgetbridge/wiki/Pebble-Firmware-updates)
* Install language files (.pbl)
* Take and share screenshots from the Pebble's screen
* PebbleKit support for 3rd Party Android Apps support (experimental) 
* Morpheuz sleep data syncronization (experimental)
* Misfit steps data synchronization (experimental)

## Notes about Firmware 3.x (Pebble Time, updated OG)

* Listing installed watchfaces will simply display previously installed watchapps, no matter if they are still installed or not.
* You should disable the "Stand-By-Mode" introduced in firmware 3.4.
According to pebble, this mode will disable bluetooth automatically when the
pebble does not sense motion for about 30 minutes, and then re-enable it if it
senses motion again. We do not support initiating connections from the pebble
yet, so this will leave you pebble disconnected until you connect it again
manually, and/or use even more energy by triggering a reconnect from
Gadgetbridge.

## How to use (Pebble)

1. Pair your Pebble through Gadgetbridge's Discovery Activity or the Android Bluetooth Settings
2. Start Gadgetbridge, tap on the device you want to connect to
3. To test, choose "Debug" from the menu and play around

## Features (Mi Band)

* Mi Band notifications (LEDs + vibration) for 
    * Discovery and pairing
    * Incoming calls
    * SMS received
    * K-9 mails received
    * Generic Android notifications
* Synchronize the time to the Mi Band
* Display firmware version and battery state
* Synchronize activity data
* Display sleep data (alpha)
* Display sports data (step count) (alpha)
* Display live activity data (alpha)
* Set alarms on the Mi Band

## How to use (Mi Band)

* When starting Gadgetbridge and no device is visible, it will automatically
  attempt to discover and pair your Mi Band. Alternatively you can invoke this
  manually via the menu button. It will ask you for some personal info that appears
  to be needed for proper steps calculation on the band. If you do not provide these,
  some hardcoded default "dummy" values will be used instead. 

  When your Mi Band starts to vibrate and blink with all three LEDs during the pairing process,
  tap it quickly a few times in a row to confirm the pairing with the band.

1. Configure other notifications as desired
2. Go back to the "Gadgetbridge" Activity
3. Tap the "MI" item to connect if you're not connected yet.
4. To test, chose "Debug" from the menu and play around

Known Issues:

* The initial connection to a Mi Band sometimes takes a little patience. Try to connect a few times, wait, 
  and try connecting again. This only happens until you have "bonded" with the Mi Band, i.e. until it 
  knows your MAC address. This behavior may also only occur with older firmware versions.

## Authors (in order of first code contribution)

* Andreas Shimokawa
* Carsten Pfeiffer
* Daniele Gobbetti

## Contribute

Contributions are welcome, be it feedback, bugreports, documentation, translation, research or code. Feel free to work
on any of the open [issues](https://github.com/Freeyourgadget/Gadgetbridge/issues?q=is%3Aopen+is%3Aissue);
just leave a comment that you're working on one to avoid duplicated work.

Translations can be contributed via https://www.transifex.com/projects/p/gadgetbridge/resource/strings/ or
manually.

## Having problems?

1. Open Gadgetbridge's settings and check the option to write log files
2. Quit Gadgetbridge and restart it
3. Reproduce the problem you encountered
4. Check the logfile at /sdcard/Android/data/nodomain.freeyourgadget.gadgetbridge/files/gadgetbridge.log
5. File an issue at https://github.com/Freeyourgadget/Gadgetbridge/issues/new and possibly provide the logfile

Alternatively you may use the standard logcat functionality to access the log.

