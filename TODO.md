TODO before 0.12.0 release:

* ~~Support importing Pebble Health data from old database~~ DONE, needs check.
* ~~Add onboarding activity on first startup (to merge old data)~~ DONE, needs check.
* ~~export db/delete db improvements~~
* ~~optional raw health record storing (settings)~~
* ~~onboarding hint about import-db in Debug activity~~ DONE, needs check.
* support pebble health record of firmware 4
* ~~check why the old DB reappears (apparently the ActivityDatabaseHandler.getOldActivityDatabaseHandler() is to blame.~~ DONE, needs check
* TESTING!
  * If the user does something else while the import is in progress (e.g. switch to other app) when going back to GB there's a crash.
```
09-07 21:01:42.739 17420-17420/nodomain.freeyourgadget.gadgetbridge E/WindowManager: android.view.WindowLeaked: Activity nodomain.freeyourgadget.gadgetbridge.activities.OnboardingActivity has leaked window com.android.internal.policy.impl.PhoneWindow$DecorView{80404d6 V.E..... R......D 0,0-684,322} that was originally added here
                                                                                         at android.view.ViewRootImpl.<init>(ViewRootImpl.java:363)
                                                                                         at android.view.WindowManagerGlobal.addView(WindowManagerGlobal.java:271)
                                                                                         at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:85)
                                                                                         at android.app.Dialog.show(Dialog.java:298)
                                                                                         at android.app.ProgressDialog.show(ProgressDialog.java:116)
                                                                                         at android.app.ProgressDialog.show(ProgressDialog.java:104)
                                                                                         at nodomain.freeyourgadget.gadgetbridge.activities.OnboardingActivity.mergeOldActivityDbContents(OnboardingActivity.java:63)
                                                                                         at nodomain.freeyourgadget.gadgetbridge.activities.OnboardingActivity.access$000(OnboardingActivity.java:19)
                                                                                         at nodomain.freeyourgadget.gadgetbridge.activities.OnboardingActivity$1.onClick(OnboardingActivity.java:45)
                                                                                         at android.view.View.performClick(View.java:4780)
                                                                                         at android.view.View$PerformClick.run(View.java:19866)
                                                                                         at android.os.Handler.handleCallback(Handler.java:739)
                                                                                         at android.os.Handler.dispatchMessage(Handler.java:95)
                                                                                         at android.os.Looper.loop(Looper.java:135)
                                                                                         at android.app.ActivityThread.main(ActivityThread.java:5254)
                                                                                         at java.lang.reflect.Method.invoke(Native Method)
                                                                                         at java.lang.reflect.Method.invoke(Method.java:372)
                                                                                         at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:903)
                                                                                         at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:698)
09-07 21:01:42.879 17420-17420/nodomain.freeyourgadget.gadgetbridge D/nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService: Service startcommand: nodomain.freeyourgadget.gadgetbridge.devices.action.start
09-07 21:01:42.879 17420-17420/nodomain.freeyourgadget.gadgetbridge D/nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService: Service startcommand: nodomain.freeyourgadget.gadgetbridge.devices.action.request_deviceinfo
09-07 21:01:43.398 17420-17420/nodomain.freeyourgadget.gadgetbridge I/nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService: Setting broadcast receivers to: true
09-07 21:01:43.425 17420-17420/nodomain.freeyourgadget.gadgetbridge I/Choreographer: Skipped 30 frames!  The application may be doing too much work on its main thread.
09-07 21:01:43.601 17420-17420/nodomain.freeyourgadget.gadgetbridge D/AndroidRuntime: Shutting down VM
09-07 21:01:43.616 17420-17420/nodomain.freeyourgadget.gadgetbridge E/nodomain.freeyourgadget.gadgetbridge.LoggingExceptionHandler: Uncaught exception: View=com.android.internal.policy.impl.PhoneWindow$DecorView{80404d6 V.E..... R......D 0,0-684,322} not attached to window managerjava.lang.IllegalArgumentException: View=com.android.internal.policy.impl.PhoneWindow$DecorView{80404d6 V.E..... R......D 0,0-684,322} not attached to window manager
                                                                                                                                    	at android.view.WindowManagerGlobal.findViewLocked(WindowManagerGlobal.java:396) ~[na:0.0]
                                                                                                                                    	at android.view.WindowManagerGlobal.removeView(WindowManagerGlobal.java:322) ~[na:0.0]
                                                                                                                                    	at android.view.WindowManagerImpl.removeViewImmediate(WindowManagerImpl.java:116) ~[na:0.0]
                                                                                                                                    	at android.app.Dialog.dismissDialog(Dialog.java:341) ~[na:0.0]
                                                                                                                                    	at android.app.Dialog$1.run(Dialog.java:120) ~[na:0.0]
                                                                                                                                    	at android.os.Handler.handleCallback(Handler.java:739) ~[na:0.0]
                                                                                                                                    	at android.os.Handler.dispatchMessage(Handler.java:95) ~[na:0.0]
                                                                                                                                    	at android.os.Looper.loop(Looper.java:135) ~[na:0.0]
                                                                                                                                    	at android.app.ActivityThread.main(ActivityThread.java:5254) ~[na:0.0]
                                                                                                                                    	at java.lang.reflect.Method.invoke(Native Method) ~[na:0.0]
                                                                                                                                    	at java.lang.reflect.Method.invoke(Method.java:372) ~[na:0.0]
                                                                                                                                    	at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:903) ~[na:0.0]
                                                                                                                                    	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:698) ~[na:0.0]


                                                                                                                                    --------- beginning of crash
09-07 21:01:43.616 17420-17420/nodomain.freeyourgadget.gadgetbridge E/AndroidRuntime: FATAL EXCEPTION: main
                                                                                      Process: nodomain.freeyourgadget.gadgetbridge, PID: 17420
                                                                                      java.lang.IllegalArgumentException: View=com.android.internal.policy.impl.PhoneWindow$DecorView{80404d6 V.E..... R......D 0,0-684,322} not attached to window manager
                                                                                          at android.view.WindowManagerGlobal.findViewLocked(WindowManagerGlobal.java:396)
                                                                                          at android.view.WindowManagerGlobal.removeView(WindowManagerGlobal.java:322)
                                                                                          at android.view.WindowManagerImpl.removeViewImmediate(WindowManagerImpl.java:116)
                                                                                          at android.app.Dialog.dismissDialog(Dialog.java:341)
                                                                                          at android.app.Dialog$1.run(Dialog.java:120)
                                                                                          at android.os.Handler.handleCallback(Handler.java:739)
                                                                                          at android.os.Handler.dispatchMessage(Handler.java:95)
                                                                                          at android.os.Looper.loop(Looper.java:135)
                                                                                          at android.app.ActivityThread.main(ActivityThread.java:5254)
                                                                                          at java.lang.reflect.Method.invoke(Native Method)
                                                                                          at java.lang.reflect.Method.invoke(Method.java:372)
                                                                                          at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:903)
                                                                                          at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:698)
```
  * After deleting the DB, until GB is quitted and swiped away from the recent activities this error is displayed:
```
 09-07 20:47:56.846 3217-3217/nodomain.freeyourgadget.gadgetbridge E/nodomain.freeyourgadget.gadgetbridge.util.GB: Error retrieving devices from database
```
  * Birthday in the user table is a timestamp: year is the user-entered value, month, day, hour, etc. are from the timestamp when the record gets created.
* tx pull

Non blocking issues:

* don't store raw health data if the record is completely decoded (e.g. sleep/deep sleep overlay as of fw 3.14)
* Add back UUID_CHARACTERISTIC_PAIR support, at least optionally
* CSV Export
