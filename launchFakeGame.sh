#!/bin/bash
TEST_DIR=test_server
SPIGOT_JAR_NAME=paperspigot.jar
PLUGINS_PATH=build/libs/CaviarBR.jar
SCREEN=testServer

apt install getty -y
script /dev/null

# Create server folder and download spigot
if [ ! -d "$TEST_DIR" ]; then
	mkdir $TEST_DIR/
	cd $TEST_DIR/ || exit
	echo "eula=true" > eula.txt
	mkdir plugins/
else
	cd $TEST_DIR/ || exit
fi

if [ -f "../updatePaper.sh" ]; then
	../updatePaper.sh
else
	echo -e "\e[93mWARN > Script updatePaper.sh not found. You sould check manually the spigot jar at $TEST_DIR\$SPIGOT_JAR_NAME\e[0m"
fi

cp ../$PLUGINS_PATH plugins/

if [ ! -f "plugins/TitanBoxRFP.jar" ]; then
	cd plugins
	curl -LO https://github.com/tristiisch/ReallyFakePlayers/releases/download/v1.11.3/TitanBoxRFP.jar
	cd -
fi

function control_server {
	status=1
	while [ $status -eq 1 ]
	do
		sleep 1
		grep -E -q "Done \([0-9]+\.[0-9]+s\)! For help, type \"help\"" logs/latest.log
		status=$?
	done
	screen -S $SCREEN -p 0 -X stuff "settings minPlayers 100^M"
	screen -S $SCREEN -p 0 -X stuff "settings mapSize 1000^M"
	screen -S $SCREEN -p 0 -X stuff "gameadmin generate start^M"
	status=1
	while [ $status -eq 1 ]
	do
		sleep 30
		grep -E -q "Generating is finish" logs/latest.log
		status=$?
	done

	screen -S $SCREEN -p 0 -X stuff "trfp add 70^M"
	sleep 30
	screen -S $SCREEN -p 0 -X stuff "trfp add 30^M"

	sleep 3600
	screen -S $SCREEN -p 0 -X stuff "gameadmin finish @p confirm^M"
}

screen -dmS $SCREEN java -jar $SPIGOT_JAR_NAME & control_server & sleep 5 && screen -x $SCREEN
