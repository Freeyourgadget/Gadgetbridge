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
* Support for generic botificaions (above filtered out)

Known Issues:

* Can't reject or hang up phone calls yet
* Phone calls that are taken or rejected both count as rejected to make the
  Pebble stop vibrating. (No in-call display yet)
* No outgoing call support (No in-call display yet)
* Notifications are not properly queued, if two arrive at about the same time,
  one of them will get lost (TODO: confirm)
* Android 4.4+ only, we can only change this by not handling generic
  notifications or by using AccessibiltyService. Don't know if it is worth the
  hassle.

Apart from that there are many internal design flaws which we will discuss using
the issue tracker.