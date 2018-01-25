#!/bin/bash
RED='\033[0;31m'
BLUE='\033[0;36m'
GREEN='\033[1;32m'
YELLOW='\033[1;33m'
ORGANGE='\033[0;33m'
CYAN='\033[1;34m'
LCYAN='\033[1;36m'
NC='\033[0m' # No Color

source build.properties

sensorDirs=( "sensors/BLEReaderSensor" "sensors/LocationSensor" "sensors/NoiseSensor" "sensors/TemperatureSensor" "sensors/WiFiSensor" )
experimentDirs=( "experiments/BleReaderExperiment"  "experiments/NoiseLevelExperiment"  "experiments/TemperatureExperiment"  "experiments/WifiScannerExperiment" )
otherDirs=( "OrganicityApp" )

android_build () {
    cd $2
    dirName=$(basename $PWD)
    echo -e "$4[$dirName]"
    echo -e "\t${BLUE}Building ${CYAN}$dirName${BLUE}...${NC}"
    rm app/build/outputs/apk/app-release-aligned.apk
    echo -e "\t${BLUE}Removed old files${NC}"
    echo -e "\t${BLUE}Starting Gradle...${NC}"
    gradle assembleRelease >> build.log
    echo -e "\t${BLUE}Gradle completed ${GREEN}successfully${NC}"
    ~/Android/Sdk/build-tools/27.0.1/zipalign -p 4 app/build/outputs/apk/app-release-unsigned.apk app/build/outputs/apk/app-release-aligned.apk >> build.log
    echo -e "\t${BLUE}APK aligned ${GREEN}successfully${NC}"
    ~/Android/Sdk/build-tools/27.0.1/apksigner sign --ks $KS --ks-pass pass:$KS_KEY --ks-key-alias $KA --key-pass pass:$KA_KEY --out app/build/outputs/apk/app-release.apk app/build/outputs/apk/app-release-aligned.apk>>build.log
    echo -e "\t${BLUE}APK signed ${GREEN}successfully${NC}"
    ~/Android/Sdk/build-tools/27.0.1/apksigner verify app/build/outputs/apk/app-release.apk>>build.log
    echo -e "\t${BLUE}APK verified ${GREEN}successfully${NC}"
    target=$1/release/$3/$(basename $PWD).apk
    cp app/build/outputs/apk/app-release.apk $target
    echo -e "\t${BLUE}APK moved to ${GREEN}$target${NC}"
    cd $1
}

for entry in "${otherDirs[@]}"
do
    android_build $PWD $entry "./" $LCYAN
done


for entry in "${sensorDirs[@]}"
do
    android_build $PWD $entry "sensors" $YELLOW
done

for entry in "${experimentDirs[@]}"
do
    android_build $PWD $entry "experiments" $ORGANGE
done


