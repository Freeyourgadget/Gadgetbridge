Gadgetbridge is now hosted on [codeberg.org](https://codeberg.org/Freeyourgadget/Gadgetbridge/).

<a href="https://codeberg.org/Freeyourgadget/Gadgetbridge/">
    <img alt="Get it on Codeberg" src="https://get-it-on.codeberg.org/get-it-on-blue-on-white.png" height="60">
</a>

Gadgetbridge
============

Gadgetbridge is an Android (5.0+) application which will allow you to use your
Pebble, Mi Band, Amazfit Bip and HPlus device (and more) without the vendor's closed source application
and without the need to create an account and transmit any of your data to the
vendor's servers.

[Homepage](https://gadgetbridge.org) - [Blog](https://blog.freeyourgadget.org) - <a rel="me" href="https://social.anoxinon.de/@gadgetbridge">Mastodon</a>

[![Donate](https://liberapay.com/assets/widgets/donate.svg)](https://liberapay.com/Gadgetbridge/donate)


[![Translate](https://hosted.weblate.org/widgets/freeyourgadget/-/gadgetbridge/svg-badge.svg)](https://hosted.weblate.org/projects/freeyourgadget/gadgetbridge)

## Code Licenses

* Gadgetbridge is licensed under the AGPLv3
* Files in app/src/main/java/net/osmand/ and app/src/main/aidl/net/osmand/ are licensed under the GPLv3 by OsmAnd BV
* Files in app/src/main/java/org/bouncycastle are licensed under the MIT license by The Legion of the Bouncy Castle Inc.

## Download

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/app/nodomain.freeyourgadget.gadgetbridge)

- [Nightly releases](https://freeyourgadget.codeberg.page/fdroid/repo?fingerprint=CD381ECCC465AB324E21BCC335895615E07E70EE11E9FD1DF3C020C5194F00B2)
    - Nightly releases are updated more frequently and may be less stable than standard releases, and they are distributed by our F-Droid repository unlike standard releases. 
- [List of changes](https://codeberg.org/Freeyourgadget/Gadgetbridge/src/master/CHANGELOG.md)

## Supported Devices

Please see the [Gadgets](https://gadgetbridge.org/gadgets/) page on the website for a complete list of supported devices.

## Features

Please see the [Features](https://gadgetbridge.org/basics/features/) page on the website.

## Authors
### Core Team (in order of first code contribution)

* Andreas Shimokawa
* Carsten Pfeiffer
* Daniele Gobbetti
* Petr Vaněk

### Additional contributors
* João Paulo Barraca (HPlus)
* Vitaly Svyastyn (NO.1 F1)
* Sami Alaoui (Teclast H30)
* "ladbsoft" (XWatch)
* Sebastian Kranz (ZeTime)
* Vadim Kaushan (ID115)
* "maxirnilian" (Lenovo Watch 9)
* "ksiwczynski", "mkusnierz", "mamutcho" (Lenovo Watch X Plus)
* Andreas Böhler (Casio)
* Jean-François Greffier (Mi Scale 2)
* Johannes Schmitt (BFH-16)
* Lukas Schwichtenberg (Makibes HR3)
* Daniel Dakhno (Fossil Q Hybrid, Fossil Hybrid HR)
* Gordon Williams (Bangle.js)
* Pavel Elagin (JYou Y5)
* Taavi Eomäe (iTag)
* Erik Bloß (TLW64)
* Yukai Li (Lefun)
* José Rebelo (Roidmi, Sony Headphones, Miband 7)
* Arjan Schrijver (Fossil Hybrid HR watchfaces)

## Contribute

Contributions are welcome, be it feedback, bug reports, documentation, translation, research or code. Feel free to work
on any of the open [issues](https://codeberg.org/Freeyourgadget/Gadgetbridge/issues);
just leave a comment that you're working on one to avoid duplicated work.

[Developer documentation](https://codeberg.org/Freeyourgadget/Gadgetbridge/wiki/Developer-Documentation) - [Support for a new Device](https://codeberg.org/Freeyourgadget/Gadgetbridge/wiki/Support-for-a-new-Device) - [New Device Tutorial](https://codeberg.org/Freeyourgadget/Gadgetbridge/wiki/New-Device-Tutorial)

Translations can be contributed via https://hosted.weblate.org/projects/freeyourgadget/gadgetbridge/

## Community

If you would like to get in touch with other Gadgetbridge users and developers outside of Codeberg, you can do so via:
* Matrix: [`#gadgetbridge:matrix.org`](https://matrix.to/#/#gadgetbridge:matrix.org)

## Do you have further questions or feedback?

Feel free to open an issue on our issue tracker, but please:
- do not use the issue tracker as a forum, do not ask for ETAs and read the issue conversation before posting
- use the search functionality to ensure that your question wasn't already answered. Don't forget to check the **closed** issues as well!
- remember that this is a community project, people are contributing in their free time because they like doing so: don't take the fun away! Be kind and constructive.
- Do not ask for help regarding your own projects, unless they are Gadgetbridge related

## Having problems?

0. Phone crashing during device discovery? Disable Privacy Guard (or similarly named functionality) during discovery.
1. Open Gadgetbridge's settings and check the option to write log files
2. Reproduce the problem you encountered
3. Check the logfile at /sdcard/Android/data/nodomain.freeyourgadget.gadgetbridge/files/gadgetbridge.log
4. File an issue at https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/new/choose and possibly provide the logfile

Alternatively you may use the standard logcat functionality to access the log.
