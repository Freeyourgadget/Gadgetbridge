
https://codeberg.org/Freeyourgadget/Gadgetbridge/wiki/Developer-Documentation

```Bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Messages sent to Bangle.js from Phone
--------------------------------------

wrapped in `GB(json)\n`

* `t:"notify", id:int, src,title,subject,body,sender,tel:string`  - new notification
* `t:"notify-", id:int`  - delete notification
* `t:"alarm", d:[{h,m},...]`  - set alarms
* `t:"find", n:bool`  - findDevice
* `t:"vibrate", n:int`  - vibrate
* `t:"weather", temp,hum,txt,wind,loc`  - weather report
* `t:"musicstate", state,position,shuffle,repeat`
* `t:"musicinfo", artist,album,track,dur,c(track count),n(track num)`

Messages from Bangle.js to Phone
--------------------------------

Just raw newline-terminated JSON lines:

* `t:"info", msg:"..."`
* `t:"warn", msg:"..."`
* `t:"error", msg:"..."`
* `t:"status", bat:0..100, volt:float(voltage)` - status update
* `t:"findPhone", n:bool`
* `t:"music", n:"play/pause/next/previous/volumeup/volumedown"`
* `t:"call", n:"ACCEPT/END/INCOMING/OUTGOING/REJECT/START/IGNORE"`
* `t:"notify", id:int, n:"DISMISS,DISMISS_ALL/OPEN/MUTE/REPLY", `
  * if `REPLY` can use `tel:string(optional), msg:string`
