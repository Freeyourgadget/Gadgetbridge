
adb install app/build/outputs/apk/debug/app-debug.apk

`t:"notify", id:id, src,title,subject,body,sender,tel`  - new notification
`t:"notify-", id:id`  - delete notification
`t:"alarm", d:[{h,m},...]`  - set alarms
`t:"find", n:bool`  - findDevice
`t:"vibrate", n:int`  - vibrate
`t:"weather", temp,hum,txt,wind,loc`  - weather report

"musicstate", state,position,shuffle,repeat
"musicinfo", artist,album,track,dur,c(track count),n(track num)
