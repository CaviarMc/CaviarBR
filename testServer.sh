#!/bin/bash

# Create server folder if not exist and run the newly created plugin

TEST_DIR=test_server
SPIGOT_JAR_NAME=paperspigot.jar
PLUGINS_PATH=build/libs/CaviarBR.jar
DATA_PACK=caviarbr_datapack.zip

# Create server folder and download spigot
if [ ! -d "$TEST_DIR" ]; then
	mkdir $TEST_DIR/
	cd $TEST_DIR/ || exit
	echo "eula=true" > eula.txt
	mkdir plugins/
else
	cd $TEST_DIR/ || exit
	find logs/ -type f -mtime +2 -delete
fi

if [ -f "../updatePaper.sh" ]; then
	../updatePaper.sh
else
	echo -e "\e[93mWARN > Script updatePaper.sh not found. You sould check manually the spigot jar at $TEST_DIR\$SPIGOT_JAR_NAME\e[0m"
fi

#if [ ! -f "plugins/CommandAPI.jar" ]; then
#	cd plugins
#	curl -LO https://github.com/JorelAli/CommandAPI/releases/latest/download/CommandAPI.jar
#	cd -
#fi

cp ../$PLUGINS_PATH plugins/
if [ ! -d "world/datapacks/" ]; then
	mkdir -p world/datapacks/
fi
cp ../$DATA_PACK world/datapacks/
rm -f log.txt error.txt

function kill_server {
	i=1
	status=1

	while [ $i -lt 300 ] && [ $status -eq 1 ]
	do
		sleep 1
		grep -E -q "Done \([0-9]+\.[0-9]+s\)! For help, type \"help\"" log.txt
		status=$?
		(( i++ ))
	done
	echo "Try to stop Java"
	kill -2 $(pgrep -f paper)
}

if [ "$1" = "stay" ]; then
	java -jar $SPIGOT_JAR_NAME
else
	kill_server & java -jar $SPIGOT_JAR_NAME | tee log.txt

	# Wait 5 mins before force shutdown
	JAVA_OPEN=0
	i=0
	while [ $JAVA_OPEN -eq 0 ]
	do
		echo "Wait for Java to stop ..."
		sleep 1
		pgrep -f paper &> /dev/null
		JAVA_OPEN=$?
		(( i++ ))
		if [ "$i" -eq 300 ]; then
			echo "ForceKill Java"
			kill -9 $(pgrep -f paper)
			break
		fi
	done

	# Exit status if error
	! (grep -P "(ERROR|^\tat |Exception|^Caused by: |\t... \d+ more)") &> error.txt < log.txt
fi
