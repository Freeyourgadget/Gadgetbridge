#!/bin/bash
pushd jerryscript
python3 tools/build.py --jerry-cmdline-snapshot ON
popd
pushd fossil-hr-watchface
export jerry=../jerryscript/build/bin/jerry-snapshot
$jerry generate -f '' open_source_watchface.js -o openSourceWatchface.bin
$jerry generate -f '' widget_date.js -o widgetDate.bin
$jerry generate -f '' widget_weather.js -o widgetWeather.bin
$jerry generate -f '' widget_steps.js -o widgetSteps.bin
$jerry generate -f '' widget_hr.js -o widgetHR.bin
popd
mv fossil-hr-watchface/*.bin ../app/src/main/assets/fossil_hr/
