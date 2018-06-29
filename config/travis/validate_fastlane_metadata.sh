#!/usr/bin/env bash

exitcode=0

for file in fastlane/metadata/android/*/full_description.txt
do
    chars=$(cat $file | wc -m)
    if [ "$chars" -gt 4000 ]
    then
        echo "$file too long"
        let exitcode++
    fi
done

for file in fastlane/metadata/android/*/short_description.txt
do
    chars=$(cat $file | wc -m)
    if [ "$chars" -gt 80 ]
    then
        echo "$file too long"
        let exitcode++
    fi
done

exit $exitcode
