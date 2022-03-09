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

if [ "$1" = "stay" ]; then
	screen -S test_mc java -jar $SPIGOT_JAR_NAME
else
	# Open server for 30 sec minimum then stop it properly
	(sleep 30 && screen -S test_mc -p 0 -X stuff "stop^M") & screen -S test_mc java -jar $SPIGOT_JAR_NAME
fi
