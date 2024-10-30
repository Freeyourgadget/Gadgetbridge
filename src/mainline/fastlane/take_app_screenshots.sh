#!/usr/bin/env bash
set -euo pipefail

#
#    Copyright (C) 2024 ysfchn
#
#    This file is part of Gadgetbridge.
#
#    Gadgetbridge is free software: you can redistribute it and/or modify
#    it under the terms of the GNU Affero General Public License as published
#    by the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Gadgetbridge is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU Affero General Public License for more details.
#
#    You should have received a copy of the GNU Affero General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

# Path of the ADB binary to use for ADB commands. If not set, "adb" will
# be used as the binary, meaning it will search from the PATH. Alternatively,
# if "ANDROID_HOME" variable exists, it will be used as fallback to search
# for the ADB binary.
ADB_BINARY="${ADB_BINARY:-"adb"}"

# Package name of Gadgetbridge. Wildcards are also allowed, in this
# case, will look for a matching package names. If there are more than
# 1 result, the script will not continue since we can't determine which
# Gadgetbridge flavor on the device should be used for screenshots.
#
# If there is only one Gadgetbridge flavor installed on the device, this
# variable can be kept this way, so it can launch whetever it is a Nightly 
# or not.
PACKAGE_NAME="${PACKAGE_NAME:-"nodomain.freeyourgadget.gadgetbridge.*"}"

# Additional commands that will be executed after disabling demo UI to revert
# the device in its original state (as in before running this script.)
REVERT_COMMANDS=""

# Where to save screenshots, relative to the folder 
# where this script is located. Don't prefix with "./".
OUTPUT_DIR="metadata/android/en-US/images/phoneScreenshots"

# Where to look for pngquant to optimize the taken screenshots,
# if pngquant is not available in given path, it will be ignored, so this is
# optional.
PNGQUANT_BIN="${PNGQUANT_BIN:-"pngquant"}"

# Displayed time on the screenshots in HH:MM format. It will be applied inside
# Android Demo Mode, so it won't change the actual Android system time.
#
# Set this value to "version" to automatically construct a time by reading 
# the current Gadgetbridge version code and converting it to minutes.
#
# SYSTEM_CLOCK="version"
SYSTEM_CLOCK="10:00"

# Set to 1 for debugging input actions, if set to "1", it won't run any 
# commands that causes any modifications (such as changing the theme, 
# enabling demo UI etc.) to the system.
DEBUG="${DEBUG:-0}"

# ---------------------------------------------------------------------
# Utility
# ---------------------------------------------------------------------

# Prefix output directory with current file path
OUTPUT_DIR="$(dirname $(realpath -s $0))/${OUTPUT_DIR}"

# Append semicolon for seperating new commands
if [ -n "${REVERT_COMMANDS}" ]; then
    REVERT_COMMANDS+=";" 
fi

# Read the version code of Gagdetbridge and convert it to HHMM format, 
# which will be set as the Android demo UI clock, 
# so it can hint which version of Gadgetbridge is being used 
# while taking screenshots.
#
# In some cases when the version code couldn't be read 
# or the constructed clock is invalid, it will fallback to 10:00.
get_clock() {
    if [ "${SYSTEM_CLOCK}" = "version" ]
    then
        # As how it is in build.gradle file.
        version_code=$(git rev-list HEAD --count)
        date --date "@${version_code}" -u +%M%S 2>/dev/null || echo "1000"
    else
        echo "${SYSTEM_CLOCK}" | sed "s/[^0-9]//g"
    fi
}

# Try to find the "adb" binary. First, look for "adb" specified in "ADB_BINARY" 
# variable. If variable is not specified, it will look for "adb" in PATH, 
# otherwise, check for "ANDROID_HOME" variable.
#
# It will print the path of the ADB binary to stdout if ADB is found, otherwise
# write to stderr and exit, since we can't continue without ADB.
locate_adb() {
    if [ ! -x "$(command -v ${ADB_BINARY})" ]
    then
        # Also try Android SDK environment variable.
        # https://developer.android.com/tools/variables#envar
        if [ -n "${ANDROID_HOME:-}" ]
        then
            if [ -x "${ANDROID_HOME}/platform-tools/adb" ]
            then
                echo "ADB found at: ${ANDROID_HOME}/platform-tools/adb" >&2
                echo "${ANDROID_HOME}/platform-tools/adb"
                return 0
            fi
        fi
        echo "ADB couldn't be found in '${ADB_BINARY}', exiting..." >&2
        exit 1
    else
        echo "ADB found at: ${ADB_BINARY}" >&2
        echo "${ADB_BINARY}"
        return 0
    fi
}

# Check if required packages are installed, and fail if its not.
check_deps() {
    if [ ! -x "$(command -v xmlstarlet)" ]
    then
        echo "'xmlstarlet' is an additional package that is required"
        echo "by this script. It may be available in your system package"
        echo "manager. Exiting..."
        exit 1
    fi
}

# Locate the ADB (see above function) and check for connected devices. If device
# count is 1, print the ADB path with a "-s DEVICE_SERIAL" suffix to stdout, so 
# we can make sure that ADB commands throughout the script will be executed only 
# for the specified device, serving as a safeguard.
# (e.g. "adb -s DEVICE_SERIAL shell echo Hello!")
#
# If deivce count is not equal to 1, exit and tell the reason to stderr.
get_adb_command() {
    adb_path=$(locate_adb)
    device_list="$("${adb_path}" devices | sed -n "s/^\([0-9a-zA-Z]*\)\s*device$/\1/p")"
    device_count="$(echo -n "${device_list}" | grep -c '^')"
    # Make sure there is an only single device connected.
    if [ $device_count != 1 ]
    then
        echo "" >&2
        echo "There must be an exactly only one device that connected to the ADB," >&2
        echo "but found $device_count devices instead! Exiting..." >&2
        exit 1
    fi
    # Construct a command line to be used in next commands.
    echo "${adb_path} -s ${device_list}"
}

# ---------------------------------------------------------------------
# ADB Commands
# ---------------------------------------------------------------------

# Changes the system theme programmatically.
# First it applies the light mode, and second, (if API level is >= 33),
# changes system colors.
set_system_theme() {
    adb_prefix="${1}"

    if [ "${DEBUG}" -eq "1" ]; then
        echo "Theme: Skipped because of debug"
        return
    fi

    # Read the existing night mode value on the system, save the original
    # value for reverting it later. And change system theme to light mode.
    # Run "adb shell cmd uimode night help" for reference.
    current_night_mode="$(${adb_prefix} shell cmd uimode night | sed "s/Night mode: //")"
    case "${current_night_mode}" in
        "no")
            echo "Theme: Already set to light, skipped"
            ;;
        "yes" | "auto" | "custom_bedtime" | "custom_schedule")
            REVERT_COMMANDS+="${adb_prefix} shell cmd uimode night ${current_night_mode} 1>/dev/null;"
            echo "Theme: Switching to light from dark"
            ${adb_prefix} shell cmd uimode night no 1>/dev/null
            ;;
        *)
            echo "Theme: Unsupported theme value, skipped"
    esac

    # For Android 13 and above, change the system colors programmatically to 
    # a red-ish color to better match with Gadgetbridge branding.
    if [ "$(${adb_prefix} shell getprop ro.build.version.sdk)" -ge "33" ]; then
        echo "Theme: Setting Dynamic color"
        # Reference for color properties:
        # https://source.android.com/docs/core/display/dynamic-color
        theme_properties='{
            "android.theme.customization.system_palette":"FFB2B5",
            "android.theme.customization.accent_color":"FFB2B5",
            "android.theme.customization.theme_style":"RAINBOW"
        }'
        original_theme="$(${adb_prefix} shell settings get secure theme_customization_overlay_packages | tr -d "[:space:]" | tr "\"" "\\\"")"
        echo "${original_theme}"
        # TODO: Reverting the colors currently doesn't work beause of the characters getting escaped somehow in ADB
        # REVERT_COMMANDS+="${adb_prefix} shell settings put secure theme_customization_overlay_packages ""'"${original_theme}"'"
        ${adb_prefix} shell settings put secure theme_customization_overlay_packages "'"$(echo "${theme_properties}" | tr -d "[:space:]" | tr "\"" "\\\"")"'"
    fi
}

# Enables Android Demo Mode and mock status bar icons.
#
# See the reference for list of properties used below:
# https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/docs/demo_mode.md
#
# Note that the demo UI may a little bit problematic, e.g. when network 
# signal level is faked to 4, the signal level might be changed still
# if the actual signal level has changed even if demo UI is shown at the
# moment, so better to take screenshots quickly as possible before status
# icons are changed.
set_status_bar() {
    adb_prefix="${1}"

    if [ "${DEBUG}" -eq "1" ]; then
        echo "Demo mode: Skipped because of debug"
        return
    fi

    echo "Demo mode: Enabling"
    ${adb_prefix} shell settings put global sysui_demo_allowed 1

    echo "Demo mode: Setting status icons"

    # Enter the demo mode
    ${adb_prefix} shell am broadcast -a com.android.systemui.demo --es command enter 1>/dev/null

    # Wi-Fi and mobile data for each SIM slot
    # (couldn't manage to turn off the second slot for dual SIM)
    ${adb_prefix} shell am broadcast -a com.android.systemui.demo --es command network \
        --es wifi show --es level 4 1>/dev/null
    ${adb_prefix} shell am broadcast -a com.android.systemui.demo --es command network \
        --es mobile show --es level 4 1>/dev/null

    # Battery
    ${adb_prefix} shell am broadcast -a com.android.systemui.demo --es command battery \
        --es level 100 --es plugged false --es powersave false 1>/dev/null

    # Explictly disable status icons
    ${adb_prefix} shell am broadcast -a com.android.systemui.demo --es command status \
        --es volume hide \
        --es bluetooth hide \
        --es location hide \
        --es alarm hide \
        --es sync hide \
        --es tty hide \
        --es eri hide \
        --es mute hide \
        --es speakerphone hide 1>/dev/null

    # No notification icons
    ${adb_prefix} shell am broadcast -a com.android.systemui.demo --es command notifications \
        --es visible false 1>/dev/null

    # Set system clock
    ${adb_prefix} shell am broadcast -a com.android.systemui.demo --es command clock \
        --es hhmm $(get_clock) 1>/dev/null

    # Also disable demo UI itself when reverting the preferences.
    REVERT_COMMANDS+="${adb_prefix} shell am broadcast -a com.android.systemui.demo --es command exit 1>/dev/null;"
    REVERT_COMMANDS+="${adb_prefix} shell settings put global sysui_demo_allowed 0;"
}

# In addition to the Android Demo Mode (as above), we can mock more system
# status icons with LineageOS (or ROMs that based on LineageOS) specific 
# settings, so we can keep the AOSP-like look for screenshots.
#
# This function will be executed for all devices regardless of their ROM,
# since this function already checks if running under LineageOS, and if it
# is not, it is a no-op.
set_status_bar_lineage_os() {
    adb_prefix="${1}"

    if [ "${DEBUG}" -eq "1" ]; then
        echo "LineageOS: Skipped because of debug"
        return
    fi

    # As its name implies, "ro.lineage.device" property is found on
    # LineageOS (or based on it) ROMs, so we check for its existence.
    if [ -n "$(${adb_prefix} shell getprop ro.lineage.device)" ]
    then
        echo "LineageOS: Setting ROM-specific icons"

        # LineageOS allows changing the battery icon style, so we set it
        # to portrait and hide the battery percent temporarily. Note the
        # "--lineage" flag here, since it is a LineageOS addition, not
        # Android's, so it won't work without that flag.
        #
        # See also for all system properties:
        # https://lineageos.github.io/android_lineage-sdk/reference/lineageos/providers/LineageSettings.System.html

        battery_style="$(${adb_prefix} shell settings get --lineage system status_bar_battery_style)"
        battery_percent="$(${adb_prefix} shell settings get --lineage system status_bar_show_battery_percent)"

        # Save the original values to a variable, so we can execute these commands after
        # screenshots are taken to revert the changed system preferences to its original state.
        REVERT_COMMANDS+="${adb_prefix} shell settings put --lineage system status_bar_battery_style ${battery_style};"
        REVERT_COMMANDS+="${adb_prefix} shell settings put --lineage system status_bar_show_battery_percent ${battery_percent};"

        ${adb_prefix} shell settings put --lineage system status_bar_battery_style 0
        ${adb_prefix} shell settings put --lineage system status_bar_show_battery_percent 0
    else
        echo "LineageOS: Not a LineageOS ROM, skipped"
    fi
}

# Filters the installed packages on the device and prints the matched
# Gadgetbridge package name that specified in "PACKAGE_NAME" variable.
get_gadgetbridge_app() {
    adb_prefix="${1}"

    # Convert the "PACKAGE_NAME" variable to a valid regex format, change periods to
    # literal periods, and change wildcards to a valid wildcards.
    package_filter="$(echo "${PACKAGE_NAME}" | tr "." "\\." | tr "*" ".*")"

    # List installed & enabled ("-e" flag) packages, and find the packages that
    # matches with the pattern.
    packages="$(${adb_prefix} shell cmd package list packages -e | sed -rn "s/^package:(${package_filter})/\1/p")"
    package_count="$(echo -n "${packages}" | grep -c '^')"

    # There can be multiple Gadgetbridge flavors installed on the device, since we
    # can't determine which one should be launched, we exit.
    if [ $package_count != 1 ]
    then
        echo "" >&2
        echo "There are none or more than one Gadgetbridge flavors found on the device," >&2
        echo "since it can't be determined which Gadgetbridge flavor should be used, " >&2
        echo "you will need to specifically set the preferred flavor with PACKAGE_NAME" >&2
        echo "variable to continue!" >&2
        exit 1
    fi

    echo "Chosen app: ${packages}" >&2
    echo "${packages}"
}

# ---------------------------------------------------------------------
# Functions
# ---------------------------------------------------------------------

optimize_screenshots() {
    if [ -x "$(command -v ${PNGQUANT_BIN})" ]
    then
        echo "Optimize: Running pngquant"
        "$(command -v ${PNGQUANT_BIN})" --strip --force --ext .png $(find "${OUTPUT_DIR}" -name "*.png")
        echo "Optimize: Done"
    else
        echo "Optimize: Couldn't find pngquant at '${PNGQUANT_BIN}', skipped"
    fi
}

# Takes a screenshot of the screen and saves to a given name. Optionally,
# it also can be prompted to pressing a key before taking a screenshot.
grab_screenshot() {
    adb_prefix="${1}"
    file_name="${2}"
    wait_for_key_prompt="${3:-}"

    if [ -n "${wait_for_key_prompt}" ]; then
        read -s -r -N 1 -p "[screenshot]: Waiting for '${file_name}' - ${wait_for_key_prompt} ** [Press any key to continue]"
        echo ""
    fi
    ${adb_prefix} shell screencap -p > "${OUTPUT_DIR}/${file_name}"
    # Play a bell sound to indicate the screenshot has been saved.
    echo -en '\a'
    echo "[screenshot]: Took '${file_name}'."
}

# Gets the XY touch positions of a UI element that hinted by a specific
# name by filtering from the layout data. The layout can be obtained by 
# "dump_screen_layout" function. 
get_position() {
    dump_output="${1}"
    name="${2}"

    # There can be multiple elements that matching the same "name", so
    # we limit to printing one line only by specifying which n-th element
    # to return. If not given, will print the coordinates of the first
    # matching (1st) element.
    index="${3:-1}"
    echo "${dump_output}" | sed -rn "s/name:${name},position:([0-9]+):([0-9]+)/\1 \2/p" | sed -n ${index}p
}

launch_gadgetbridge() {
    adb_prefix="${1}"
    package_name="${2}"

    # Check if Gadgetbridge is launched already, and warn the user to close
    # Gadgetbridge, so we can start in the home screen when we launch
    # Gadgetbridge.
    status=$(${adb_prefix} shell dumpsys window windows \
        | grep -E 'mCurrentFocus|mFocusedApp|mInputMethodTarget|mSurface' \
        | grep -qs "${package_name}" || echo $?)
    
    if [ -z $status ]; then
        echo "[!] Gadgetbridge is currently shown on the device. Close it " >&2
        echo "(and delete it from the recents) and press any key to continue." >&2
        read -s -r -N 1
        launch_gadgetbridge "${adb_prefix}" "${package_name}"
    else
        echo "Launching Gadgetbridge..."

        # Launch Gadgetbridge with monkey command, since it doesn't require us
        # to specify the activity class name along with the package name.
        ${adb_prefix} shell monkey -p "${package_name}" 1 1 1>/dev/null 2>&1

        # Give it some time before continuing.
        sleep 2
    fi
}

# Creates a dump of the current layout that is shown on the device screen.
# So this allow us to programmatically get touch positions of UI elements.
# 
# It will output a string that contains element descriptions and their
# X/Y positions, for each valid element:
# "name:Open navigation drawer,position:70:159"
dump_screen_layout() {
    adb_prefix="${1}"

    # "uiautomator" service prints a XML document, but it also prints a 
    # "UI hierchary dumped to..." message in the end, so we strip this
    # message from the string to make it a valid XML text.
    char_count=$(echo "UI hierchary dumped to: /dev/tty" | wc -c)
    xml_document="$(${adb_prefix} exec-out uiautomator dump /dev/tty | head -c -${char_count})"

    # XPath to list UI nodes listed in the current screen (we specifically
    # only list nodes in the Gadgetbridge app to be explicit), and print
    # their bound positions ("bounds") and UI names ("content-desc"), so we
    # can filter elements by their names.
    package_name="$(echo "${PACKAGE_NAME}" | sed "s/*$//")"
    xml_path="./hierarchy/node[contains(@package, \"${package_name}\") and @class=\"android.widget.FrameLayout\"]//node/@*[name() = \"bounds\" or name() = \"content-desc\"]"

    index=0
    previous_ui_name=""
    while IFS="" read -r line || [ -n "$line" ]
    do
        # The output from XPath query, we will have attribute values printed
        # one by one, so since we look for both "bounds" and "content-desc" attribute,
        # it will print:
        # - bounds
        # - content-desc
        # - bounds
        # - content-desc
        #
        # So to group each node, we check if it is divisible by 2, the count of the
        # attributes that we are listing.
        if [ "$(( $index % 2 ))" -eq 0 ]; then
            # This will be the "content-desc", so we pass it as-is.
            previous_ui_name="${line}"
        else
            # This will be the "bounds", so we convert this value to a X/Y positions.
            # Each bound value will contain 4 points, START_X, START_Y, END_X and END_Y,
            # and is formatted as: "[0,0][1080,2219]", since we only need a single position
            # to touch it, we calculate the center of both X and Y here.
            #
            # To read the values, we need to convert these bounds to a Bash array,
            # so remove all comma and square bracket characters to only leave the numbers.
            # "[0,0][1080,2219]" -> array of 4: "0 0 1080 2219"
            bounds=($(echo "${line}" | sed "s/^\[//; s/\[/ /g; s/\]//g; s/,/ /g"))

            # Now we can calculate the center position of the element.
            # X_CENTER = X_START + ((X_END - X_START) / 2)
            # Y_CENTER = Y_START + ((Y_END - Y_START) / 2)
            center_x=$((( ${bounds[0]} + ((${bounds[2]} - ${bounds[0]}) / 2) )))
            center_y=$((( ${bounds[1]} + ((${bounds[3]} - ${bounds[1]}) / 2) )))

            # Only output if there is a UI name for the element.
            if [ -n "${previous_ui_name}" ]
            then
                echo "name:${previous_ui_name},position:${center_x}:${center_y}"
            fi
        fi
        index=$(( index + 1 ))
    done < <(echo "${xml_document}" | xmlstarlet sel --template --value-of "${xml_path}")
    if [ $index -eq 0 ]
    then
        echo "[!] Couldn't find valid elements on the screen, is Gadgetbridge currently shown on the screen?" >&2
    fi
}

perform_input() {
    adb_prefix="${1}"
    input_type="${2}"

    case "${input_type}" in
        # Swipe for a specific direction.
        "swipe_left" | "swipe_right")
            # Get screen dimensions.
            dimensions="$(${adb_prefix} shell wm size | rev | cut -d " " -f 1 | rev)"
            screen_x=$(echo "$dimensions" | cut -d "x" -f 1)
            screen_y=$(echo "$dimensions" | cut -d "x" -f 2)
            # If swiping to the left, swipe to the left edge of the screen.
            swipe_x=0
            # Or if swiping to the right, swipe to the right edge of the screen.
            if [ "${input_type}" = "swipe_right" ]; then
                swipe_x=$screen_x
            fi
            # Perform the swipe (starting form the center of the screen) 
            # to given direction in 100 milliseconds.
            ${adb_prefix} shell input swipe $((screen_x / 2)) $((screen_y / 2)) $swipe_x $((screen_y / 2)) 100
            sleep 1
            ;;
        # Send "back" button key.
        "go_back")
            # 111 = KEYCODE_ESCAPE
            # 4 = KEYCODE_BACK
            ${adb_prefix} shell input keyevent 4
            sleep 0.5
            ;;
        # Touch to a UI element which hinted with a given element name.
        "touch_element")
            element_name="${3}"
            xml_layout="$(dump_screen_layout "${adb_prefix}")"
            element_index="${4:-1}"
            # Get the X/Y position of given element name, and touch it.
            ${adb_prefix} shell input tap $(get_position "${xml_layout}" "${element_name}" "${element_index}")
            sleep 0.5
            ;;
        *)
            echo "[!] Unsupported input type!"
            exit 1
    esac
}

# Open Gadgetbridge and start taking screenshots of different screens.
# To find out the touch positions;
# Developer Options > Under "Input" category > Enable "Pointer location"
#
# TODO: Calculate the exact position from screen density and dimensions to make X/Y positions adaptive with other screens.
take_screenshots() {
    adb_prefix="${1}"

    echo; echo "! - If system preferences are not reverted automatically after exiting, you can manually run these commands:"
    echo "${REVERT_COMMANDS}"
    echo; echo "Taking screenshots, it might take some time;"

    # Dashboard
    perform_input "${adb_prefix}" "swipe_right"
    grab_screenshot "${adb_prefix}" "10-Dashboard.png" "Change the day or keep today"

    # Calendar
    perform_input "${adb_prefix}" "touch_element" "Calendar"
    grab_screenshot "${adb_prefix}" "30-Calendar.png" "Change the month of keep as-is"
    perform_input "${adb_prefix}" "go_back"

    # Devices
    perform_input "${adb_prefix}" "swipe_left"
    grab_screenshot "${adb_prefix}" "11-Devices.png"

    # Activity chart
    perform_input "${adb_prefix}" "touch_element" "Your activity"
    grab_screenshot "${adb_prefix}" "20-Activity.png" "Adjust the chart"

    # Sleep chart (Day)
    perform_input "${adb_prefix}" "touch_element" "Sleep"
    perform_input "${adb_prefix}" "touch_element" "Day"
    grab_screenshot "${adb_prefix}" "40-SleepPerDay.png" "Change the day or keep today"

    # Sleep chart (Week)
    perform_input "${adb_prefix}" "touch_element" "Week"
    grab_screenshot "${adb_prefix}" "41-SleepPerWeek.png" "Change the day or keep today"

    # Steps chart (Day)
    perform_input "${adb_prefix}" "touch_element" "Steps"
    perform_input "${adb_prefix}" "touch_element" "Day"
    grab_screenshot "${adb_prefix}" "50-StepsPerDay.png" "Change the day or keep today"

    # Steps chart (Week)
    perform_input "${adb_prefix}" "touch_element" "Week"
    grab_screenshot "${adb_prefix}" "51-StepsPerWeek.png" "Change the day or keep today"

    # Steps chart (Month)
    perform_input "${adb_prefix}" "touch_element" "Month"
    grab_screenshot "${adb_prefix}" "52-StepsPerMonth.png" "Change the day or keep today"

    # Stress
    perform_input "${adb_prefix}" "touch_element" "Stress"
    grab_screenshot "${adb_prefix}" "60-Stress.png" "Change the day or keep today"

    # Live activity
    perform_input "${adb_prefix}" "touch_element" "Live activity"
    grab_screenshot "${adb_prefix}" "70-LiveActivity.png" "Start your activity on the watch"

    # Back to the home screen
    perform_input "${adb_prefix}" "go_back"

    # Sport activities
    perform_input "${adb_prefix}" "touch_element" "Your activity tracks"
    grab_screenshot "${adb_prefix}" "70-SportActivities.png"

    # Sport activity filter
    perform_input "${adb_prefix}" "touch_element" "Sports Activities Filter"
    grab_screenshot "${adb_prefix}" "71-SportActivitiesFilter.png"

    # Back to the sport activities screen
    perform_input "${adb_prefix}" "go_back"

    # Sport activity summary
    perform_input "${adb_prefix}" "touch_element" "Device image" 10
    grab_screenshot "${adb_prefix}" "72-SportActivitiesSummary.png"
}

# Disable demo UI, and revert all applied settings by eval-ing all
# revert commands that is saved before.
revert() {
    if [ -n "${REVERT_COMMANDS}" ]; then
        echo; echo "[*] Reverting modifications..."
    fi
    eval "${REVERT_COMMANDS}"
    exit 0
}

start() {
    check_deps

    echo "[*] Searching for devices..."
    adb_command=$(get_adb_command)
    use_package=$(get_gadgetbridge_app "${adb_command}")

    echo; echo "[*] Applying preferences..."
    set_system_theme "${adb_command}"
    set_status_bar_lineage_os "${adb_command}"
    set_status_bar "${adb_command}"

    launch_gadgetbridge "${adb_command}" "${use_package}"

    echo; echo "[*] Taking screenshots..."
    take_screenshots "${adb_command}"
    optimize_screenshots
}

# Run "revert" function to revert everything if received an SIGINT or SIGTERM signal.
# Also with "EXIT" signal, run this function whenever the script exits.
trap revert SIGINT SIGTERM EXIT

start