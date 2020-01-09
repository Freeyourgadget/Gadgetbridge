#!/bin/bash
#This script needs bash for the read command
#Takes screenshots of Gadgetbridge for the fastlane. 
#Live Activity screenshot not taken, as it needs more inputs.

VERSION="0421"
#Version sets clock in Android demo mode to version
#to indicate when screenshots have been taken

DIR="metadata/android/en-US/images/phoneScreenshots/"
#DIR="/tmp/"

#enable demo mode
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm $VERSION
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4

#launch Gadgetbridge
adb shell monkey -p nodomain.freeyourgadget.gadgetbridge -c android.intent.category.LAUNCHER 1
sleep 3

#Start taking screenshots:

adb shell screencap -p  > $DIR"10-MainScreen.png"

adb shell input tap 455 355
sleep 0.5
adb shell screencap -p  > $DIR"20-ActivityAndSleep.png"

adb shell input tap 240 210
read -p "slightly adjust label on chart and press Enter"
adb shell screencap -p  > $DIR"30-Sleep.png"

adb shell input tap 420 210
read -p "slightly adjust label on chart and press Enter"
adb shell screencap -p  > $DIR"40-SleepPerWeek.png"

#switch to month
adb shell input tap 670 100
sleep 0.5
adb shell input tap 670 1080
sleep 0.5
adb shell input keyevent 111
read -p "slightly adjust label on chart and press Enter"

adb shell screencap -p  > $DIR"41-SleepPerMonth.png"


adb shell input tap 590 210
sleep 0.5
adb shell screencap -p  > $DIR"51-StepsPerMonth.png"

#switch back to week
adb shell input tap 670 100
sleep 0.5
adb shell input tap 670 1080
sleep 0.5
adb shell input keyevent 111
sleep 1

adb shell screencap -p  > $DIR"50-StepsPerWeek.png"

adb shell input tap 590 210
sleep 0.5
adb shell screencap -p  > $DIR"60-SpeedZones.png"

#Go back to main screen
adb shell input keyevent 111

#disable demo mode
adb shell am broadcast -a com.android.systemui.demo -e command exit
adb shell settings put global sysui_demo_allowed 0
