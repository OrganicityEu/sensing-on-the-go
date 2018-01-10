#!/bin/bash
V_CODE=$1
V_NAME=$2
for item in $(find ./ | grep build.gradle)
do
    echo $item
    sed -i -e "s/versionCode [0-9]*/versionCode $V_CODE/g" $item
    sed -i -e "s/versionName \"[0-9]*\.[0-9]*\"/versionName \"$V_NAME\"/g" $item
done
grep -e "versionCode" -e "versionName" $(find ./ | grep build.gradle) -R
