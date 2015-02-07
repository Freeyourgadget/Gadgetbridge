Gadgetbridge
============

Gadgetbridge is a Android (4.4+) Application which will allow you to use your
Gadget (Smartwatches/Fitness Bands etc) without the vendors closed source
application and without the need to create an account and sync your data to the
vendors servers.

Right now this is in very early testing stages and only supports the Pebble.

USE IT AT YOUR OWN RISK. It will problably not work. And if it works it will
annoy you more than it helps you ;)

Features:

* Incoming calls (caller, phone number)
* SMS notification (sender, body)
* K-9 Mail notification support (sender, subject, preview)
* Support for generic notificaions (above filtered out)

How to use (Pebble):

1. Pair your Pebble though the Android Bluetooth Settings
2. Start Gadgetbridge, press "connect"
3. To test, chose "Debug" from the menu and play around

Known Issues:

* No reconnect, if connection is lost, you have to press "connect" again
* Can't reject or hang up phone calls yet
* No outgoing call support 
* Notifications are not properly queued, if two arrive at about the same time,
  one of them might get lost (TODO: confirm)
* Android 4.4+ only, we can only change this by not handling generic
  notifications or by using AccessibiltyService. Don't know if it is worth the
  hassle.

Apart from that there are many internal design flaws which we will discuss using
the issue tracker.