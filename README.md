Gadgetbridge
============

Gadgetbridge is a Android (4.4+) Application which will allow you to use your
Gadget (Smartwatches/Fitness Bands etc) without the vendors closed source
application and without the need to create an account and sync your data to the
vendors servers.

Right now this is in very early testing stages and only supports the Pebble.

USE IT AT YOUR OWN RISK. It will problably not work. And if it works it will
annoy you more than it helps you ;)

Known Visible Issues:

* No special notifications, EVERYTHING will be send as a Chat/SMS message
* Notifications are not properly queued, if two arrive at about the same time,
  one of them will get lost
* Connection to Pebble will be reopened and closed for every message (dont know
  if this saves or consumes more energy)
* Android 4.4+ only, we can only change this by implementing an
  AccessibiltyService. Don't know if it is worth the hassle.
* This will open the dialog to allow capturing notifications every time the
  Activity gets restarted.

Apart from that there are many internal design flaws which we will discuss using
the issue tracker.
