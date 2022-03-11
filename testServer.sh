#!/bin/bash

# Create server folder if not exist and run the newly created plugin

TEST_DIR=test_server
SPIGOT_VERSION=1.17.1
SPIGOT_BUILD_ID=409

SPIGOT_URL=https://papermc.io/api/v2/projects/paper/versions
SPIGOT_JAR_NAME=paper-$SPIGOT_VERSION-$SPIGOT_BUILD_ID.jar
SPIGOT_FULL_URL=$SPIGOT_URL/$SPIGOT_VERSION/builds/$SPIGOT_BUILD_ID/downloads/$SPIGOT_JAR_NAME

# Create server folder and download spigot
if [ ! -d "$TEST_DIR" ]; then
	mkdir $TEST_DIR/
	cd $TEST_DIR/
	curl -O $SPIGOT_FULL_URL
	echo "eula=true" > eula.txt
	mkdir plugins/
else
	cd $TEST_DIR/
fi

cp ../build/*.jar plugins/
cp ../vanilla_worldgen_no_ocean.zip world/datapacks/
rm -f log.txt error.txt

function kill_server {
	i=1
	status=1

	while [ $i -lt 60 ] && [ $status -eq 1 ]
	do
		sleep 1
		cat log.txt | grep "Timings Reset" &> /dev/null
		status=$?
		let "i++"
	done
	echo "Try to stop Java"
	kill -2 $(pgrep -f paper)
}

if [ "$1" = "stay" ]; then
	java -jar $SPIGOT_JAR_NAME
else
	kill_server & java -jar $SPIGOT_JAR_NAME | tee log.txt

	# Wait 60 secondes before force shutdown
	JAVA_OPEN=0
	i=0
	while [ $JAVA_OPEN -eq 0 ]
	do
		echo "Wait for Java to stop ..."
		sleep 2
		pgrep -f paper &> /dev/null
		JAVA_OPEN=$?
		let "i++"
		if [ $i -eq 30 ]; then
			echo "ForceKill Java"
			kill -9 $(pgrep -f paper)
			break
		fi
	done

	# Exit status if error
	cat log.txt | (! grep -P "(ERROR|^\tat |Exception|^Caused by: |\t... \d+ more)") &> error.txt
fi

