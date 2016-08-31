TODO before 0.12.0 release:

* ~~Support importing Pebble Health data from old database~~ DONE, needs check.
* ~~Add onboarding activity on first startup (to merge old data)~~ DONE, needs check.
* ~~export db/delete db improvements~~
* ~~optional raw health record storing (settings)~~
* ~~onboarding hint about import-db in Debug activity~~ DONE, needs check.
* support pebble health record of firmware 4
* check why the old DB reappears (apparently the ActivityDatabaseHandler.getOldActivityDatabaseHandler() is to blame.
* TESTING!
* tx pull

Non blocking issues:

* don't store raw health data if the record is completely decoded (e.g. sleep/deep sleep overlay as of fw 3.14)
* Add back UUID_CHARACTERISTIC_PAIR support, at least optionally
* CSV Export
