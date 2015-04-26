Gadgetbridge
============

Gadgetbridge is an Android (4.4+) Application which will allow you to use your
Pebble or Mi Band without the vendor's closed source application and without the
need to create an account and transmit any of your data to the vendor's servers.

## Features (Pebble)

* Incoming calls notification and display (caller, phone number)
* Outgoing call display
* Reject/hangup calls
* SMS notification (sender, body)
* K-9 Mail notification support (sender, subject, preview)
* Support for generic notificaions (above filtered out)
* Apollo playback info (artist, album, track)
* Music control: play/pause, next track, previous track, volume up, volume down
* List and remove installed apps/watchfaces
* Install .pbw files
* Install firmware from .pbz files (EXPERIMENTAL)

## How to use (Pebble)

1. Pair your Pebble through the Android Bluetooth Settings
2. Start Gadgetbridge, tap on the device you want to connect to
3. To test, choose "Debug" from the menu and play around

## Features (Mi Band)

* Mi Band notifications (LEDs + vibration) for 
** Incoming calls
** SMS received
** K-9 mails received
** Generic Android notifications
* Synchronize the time to the Mi Band
* Display firmware version

## How to use (Mi Band)

* With older Mi Band firmware (e.g. 1.4.0.x): Add your Mi Band's MAC address manually  (Settings -> Debug)
* With newer firmware that supports pairing, pair your Mi Band through the Android Bluetooth Settings

1. Configure other notifications as desired
2. Go back to the "Gadgetbridge" Activity
3. Tap the "MI" device to connect
4. To test, chose "Debug" from the menu and play around

Known Issues:

* Android 4.4+ only, we can only change this by not handling generic
  notifications or by using AccessibiltyService. Don't know if it is worth the
  hassle.

* The initial connection to a Mi Band sometimes takes a little patience. Try to connect a few times, wait, 
  possibly quit Gadgetbridge before connecting again. This only happens until you have "bonded" with the 
  Mi Band, i.e. until it knows your MAC address. This behavior may also only occur with older firmware versions.

## Download

[![Gadgetbridge on F-Droid](/Get_it_on_F-Droid.svg.png?raw=true "Download from F-Droid")](https://f-droid.org/repository/browse/?fdid=nodomain.freeyourgadget.gadgetbridge)
