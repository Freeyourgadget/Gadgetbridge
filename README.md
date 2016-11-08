Gadgetbridge
============

Gadgetbridge is an Android (4.4+) Application which will allow you to use your
Pebble or Mi Band without the vendor's closed source application and without the
need to create an account and transmit any of your data to the vendor's servers.

[![Build](https://travis-ci.org/Freeyourgadget/Gadgetbridge.svg?branch=master)](https://travis-ci.org/Freeyourgadget/Gadgetbridge)

## Download

[![Gadgetbridge on F-Droid](/Get_it_on_F-Droid.svg.png?raw=true "Download from F-Droid")](https://f-droid.org/repository/browse/?fdid=nodomain.freeyourgadget.gadgetbridge)

[List of changes](CHANGELOG.md)

## Supported Devices
* Pebble, Pebble Steel, Pebble Time, Pebble Time Steel, Pebble Time Round
* Mi Band, Mi Band 1A, Mi Band 1S
* Mi Band 2 (only notifications)
* Vibratissimo (experimental)

***THE PEBBLE 2 AND PEBBLE TIME 2 ARE CURRENTLY NOT SUPPORTED. ADDING SUPPORT IS HIGH-PRIORITY BUT WE CANNOT GIVE YOU AN ETA!***


## Features (Pebble)

* Incoming calls notification and display
* Outgoing call display
* Reject/hangup calls
* SMS notification
* K-9 Mail notification support
* Support for generic notifications (above filtered out)
* Support for up to 16 predefined replies for SMS and Android Wear compatible notifications (experimental, tested with Signal)
* Dismiss individial notifications, mute or open corresponding app on phone from the action menu (generic notifications)
* Dismiss all notifications from the action menu (non-generic notifications) 
* Music playback info (artist, album, track)
* Music control: play/pause, next track, previous track, volume up, volume down
* List and remove installed apps/watchfaces
* Install watchfaces and watchapps (.pbw)
* Install firmware files (.pbz) [READ THE WIKI](https://github.com/Freeyourgadget/Gadgetbridge/wiki/Pebble-Firmware-updates)
* Install language files (.pbl)
* Take and share screenshots from the Pebble's screen
* PebbleKit support for 3rd Party Android Apps (experimental)
* Fetch activity data from Pebble Health, Misfit and Morpheuz (experimental)
* Configure watchfaces / apps (limited compatibility, experimental)

## Notes about Firmware >=3.0 (Pebble Time, updated OG)

* Gadgetbridge will keep track of installed watchfaces, but if the Pebble is used with another phone or another app, the information displayed in the app manager can get out of sync since it is impossible to query Firmware >= 3.x for installed apps/watchfaces.

## Getting Started (Pebble)

1. Pair your Pebble through the Android's Bluetooth Settings
2. Start Gadgetbridge, tap on the device you want to connect to
3. To test, choose "Debug" from the menu and play around

For more information read [this wiki article](https://github.com/Freeyourgadget/Gadgetbridge/wiki/Pebble-Getting-Started) 

## Features (Mi Band 1x)

* Discovery and pairing
* Mi Band notifications (LEDs + vibration) for
    * Incoming calls
    * SMS received
    * K-9 mails received
    * Conversations messages
    * Generic Android notifications
* Synchronize the time to the Mi Band
* Display firmware version and battery state
* Firmware Update
* Heart rate measurement on demand and during sleep
* Synchronize activity data
* Display sleep data (alpha)
* Display sports data (step count) (alpha)
* Display live activity data (alpha)
* Set alarms on the Mi Band

## Features (Mi Band 2)

* Discovery and pairing
* Mi Band notifications (Display + vibration) for
    * Incoming calls
    * SMS received
    * K-9 mails received
    * Conversations messages
    * Generic Android notifications
* Synchronize the time to the Mi Band
* Display firmware version
* Heart rate measurement (alpha)
* Set alarms on the Mi Band 2

## How to use (Mi Band 1+2)

* When starting Gadgetbridge the first time, it will automatically
  attempt to discover and pair your Mi Band. Alternatively you can invoke discovery
  manually via the "+" button. It will ask you for some personal info that appears
  to be needed for proper steps calculation on the band. If you do not provide these,
  some hardcoded default "dummy" values will be used instead. 

  When your Mi Band starts to vibrate and blink during the pairing process,
  tap it quickly a few times in a row to confirm the pairing with the band.

1. Configure other notifications as desired
2. Go back to the "Gadgetbridge" Activity
3. Tap the Mi Band item to connect if you're not connected yet.
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

Translations can be contributed via https://www.transifex.com/projects/p/gadgetbridge/resource/strings/ or manually.

## Do you have further questions or feedback?

Feel free to open an issue on our issue tracker, but please:
- do not use the issue tracker as a forum, do not ask for ETAs and read the issue conversation before posting
- use the search functionality to ensure that your questions wasn't already answered. Don't forget to check the **closed** issues as well!
- remember that this is a community project, people are contributing in their free time because they like doing so: don't take the fun away! Be kind and constructive.


## Having problems?

1. Open Gadgetbridge's settings and check the option to write log files
2. Reproduce the problem you encountered
3. Check the logfile at /sdcard/Android/data/nodomain.freeyourgadget.gadgetbridge/files/gadgetbridge.log
4. File an issue at https://github.com/Freeyourgadget/Gadgetbridge/issues/new and possibly provide the logfile

Alternatively you may use the standard logcat functionality to access the log.

