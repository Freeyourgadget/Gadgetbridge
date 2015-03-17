Gadgetbridge
============

Gadgetbridge is a Android (4.4+) Application which will allow you to use your
Pebble without the vendors closed source application and without the need to
create an account and transmit any of your data to the vendors servers.

We plan to add support for the Mi Band and maybe even more devices.

Features:

* Incoming calls notification and display (caller, phone number)
* Outgoing call display
* Reject/hangup calls
* SMS notification (sender, body)
* K-9 Mail notification support (sender, subject, preview)
* Support for generic notificaions (above filtered out)
* Apollo playback info (artist, album, track)
* Music control: play/pause, next track, previous track

How to use:

1. Pair your Pebble though the Android Bluetooth Settings
2. Start Gadgetbridge, press "connect"
3. To test, chose "Debug" from the menu and play around

Known Issues:

* No reconnect, if connection is lost, you have to press "connect" again
* Android 4.4+ only, we can only change this by not handling generic
  notifications or by using AccessibiltyService. Don't know if it is worth the
  hassle.

Apart from that there are many internal design flaws which we will discuss using
the issue tracker.