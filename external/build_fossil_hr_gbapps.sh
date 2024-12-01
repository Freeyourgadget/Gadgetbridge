#!/bin/bash
pushd jerryscript
gcc_version="$(gcc -v 2>&1 | grep -oe '^gcc version [0-9][0-9\.]*[0-9]' | sed 's|^.* ||;s|\..*||')"
(( gcc_version > 11 )) && git apply ../patches/jerryscript-gcc-12-build-fix.patch
python3 tools/build.py --jerry-cmdline-snapshot ON
popd
pushd fossil-hr-gbapps/watchface
export jerry=../../jerryscript/build/bin/jerry-snapshot
$jerry generate -f '' open_source_watchface.js -o openSourceWatchface.bin
$jerry generate -f '' widget_date.js -o widgetDate.bin
$jerry generate -f '' widget_weather.js -o widgetWeather.bin
$jerry generate -f '' widget_steps.js -o widgetSteps.bin
$jerry generate -f '' widget_hr.js -o widgetHR.bin
$jerry generate -f '' widget_battery.js -o widgetBattery.bin
$jerry generate -f '' widget_calories.js -o widgetCalories.bin
$jerry generate -f '' widget_2nd_tz.js -o widget2ndTZ.bin
$jerry generate -f '' widget_activemins.js -o widgetActiveMins.bin
$jerry generate -f '' widget_chanceofrain.js -o widgetChanceOfRain.bin
$jerry generate -f '' widget_uv.js -o widgetUV.bin
$jerry generate -f '' widget_spo2.js -o widgetSpO2.bin
$jerry generate -f '' widget_custom.js -o widgetCustom.bin
popd
mv fossil-hr-gbapps/watchface/*.bin ../app/src/main/assets/fossil_hr/
pushd fossil-hr-gbapps/navigationApp
mkdir -p build/files/{code,config,display_name,icons,layout}
$jerry generate -f '' app.js -o build/files/code/navigationApp
python3 ../../pack.py -i build/ -o navigationApp.wapp
popd
mv fossil-hr-gbapps/navigationApp/navigationApp.wapp ../app/src/main/assets/fossil_hr/
