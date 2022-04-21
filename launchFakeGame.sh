#!/bin/bash
TEST_DIR=test_server
SPIGOT_JAR_NAME=paperspigot.jar
PLUGINS_PATH=build/libs/CaviarBR.jar
SCREEN=testServer
MAP_SIZE=1000
FAKE_PLAYERS=50

UPDATE_PLUGIN=local
GITHUB_REPO=CaviarMc/CaviarBR
GITHUB_TOKEN=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
GITHUB_JAR_FILE=CaviarBR.jar
GITHUB_URL_API=https://api.github.com/repos/$GITHUB_REPO/releases
GITHUB_USE_PRE_RELEASE=true

function usage {
	echo -e "\e[31m$0 <local|github|none> <mapSize> <players>\e[0m"
	exit 1
}

if [ -n "$1" ]; then
	if [ $1 = "local" ] || [ $1 = "github" ]; then
		UPDATE_PLUGIN=$1
	elif [ $1 = "none" ] || [ $1 = "null" ] || [ $1 = "false" ]; then
		UPDATE_PLUGIN=none
	fi
fi

if [ $UPDATE_PLUGIN = "none" ]; then
	echo -e "\e[93mWARN > D'après les pramètres, il n'aura pas de mise à jour de $GITHUB_JAR_FILE:\e[0m"

elif [ $UPDATE_PLUGIN = "local" ]; then
	echo -e "\e[36mCréation de $GITHUB_JAR_FILE grâce au repo $UPDATE_PLUGIN:\e[0m"

elif [ $UPDATE_PLUGIN = "github" ]; then
	if [ $GITHUB_USE_PRE_RELEASE = "true" ]; then
		echo -e "\e[36mRécupération de la dernière pre-release de $GITHUB_JAR_FILE sur $UPDATE_PLUGIN:\e[0m"
	else
		echo -e "\e[36mRécupération de la dernière pre-release de $GITHUB_JAR_FILE sur $UPDATE_PLUGIN:\e[0m"
	fi
fi


re='^[0-9]+$'
if [ -n "$2" ]; then
	if ! [[ $2 =~ $re ]] ; then
		usage
	fi
	MAP_SIZE=$2
fi
if [ -n "$3" ]; then
	if ! [[ $3 =~ $re ]] ; then
		usage
	fi
	FAKE_PLAYERS=$3
fi

#apt install getty -y
#getty tty
# script /dev/null

if [ ! -d "$TEST_DIR" ]; then
	echo -e "\e[36mInitialisation du dossier du serveur:\e[0m"
	mkdir $TEST_DIR/
	cd $TEST_DIR/ || exit
else
	cd $TEST_DIR/ || exit
fi
if [ ! -d "plugins/" ]; then
	echo -e "\e[36mOn crée le dossier plugin\e[0m"
	mkdir plugins/
fi
if ! grep -q "eula=true" eula.txt; then
	echo -e "\e[36mOn accepte Eula <3\e[0m"
	echo "eula=true" > eula.txt
fi

if [ $UPDATE_PLUGIN = "github" ]; then
	echo -e "\e[36mRécupération du fichier sur le repo github\e[0m"
	if [ $GITHUB_USE_PRE_RELEASE = "true" ]; then
		ASSETS_ID=$(curl -LJ -H "Authorization: token $GITHUB_TOKEN" "$GITHUB_URL_API" | jq '.[0].assets[] | select(.name == "CaviarBR.jar").id')
	else
		ASSETS_ID=$(curl -LJ -H "Authorization: token $GITHUB_TOKEN" "$GITHUB_URL_API/tags/latest" | jq '.assets[] | select(.name == "CaviarBR.jar").id')
	fi
	if [ $? != 0 ]; then
		echo -e "\e[31m$0 ERROR > Unable to get $GITHUB_JAR_FILE from github. Check token acces if private repo\e[0m"
		exit 1
	fi
	cd plugins
	echo -e "\e[36mTéléchargement du fichier sur le repo github\e[0m"
	curl -LJ -RJ -H "Authorization: token $GITHUB_TOKEN" -H 'Accept: application/octet-stream' $GITHUB_URL_API/assets/$ASSETS_ID -o $GITHUB_JAR_FILE
	if [ $? != 0 ]; then
		echo -e "\e[31m$0 ERROR > Unable to download $GITHUB_JAR_FILE from github.\e[0m"
		exit 1
	fi
	echo -e "\e[36m$GITHUB_JAR_FILE mise à jour par github (asset: $ASSETS_ID)\e[0m"
	cd -
elif [ $UPDATE_PLUGIN = "local" ]; then
	cd ..
	echo -e "\e[36mGradle jar\e[0m"
	gradle jar
	if [ $? != 0 ]; then
		echo -e "\e[31m$0 ERROR > Build of $GITHUB_JAR_FILE failed\e[0m"
		exit 1
	fi
	cd -
	cp ../$PLUGINS_PATH plugins/
fi

if [ -f "../updatePaper.sh" ]; then
	../updatePaper.sh
else
	echo -e "\e[93mWARN > Script updatePaper.sh not found. You sould check manually the spigot jar at $TEST_DIR\$SPIGOT_JAR_NAME\e[0m"
fi

if [ ! -f "plugins/TitanBoxRFP.jar" ]; then
	cd plugins
	echo -e "\e[36mTéléchargement du plugin de Fake Player\e[0m"
	curl -LO https://github.com/tristiisch/ReallyFakePlayers/releases/download/v1.11.3/TitanBoxRFP.jar
	if [ $? != 0 ]; then
		echo -e "\e[31m$0 ERROR > Unable to download Fake Player plugin from github.\e[0m"
		exit 1
	fi
	cd -
fi

function control_server {
	sleep 20
	status=1
	while [ $status -eq 1 ]
	do
		sleep 1
		grep -E -q "Done \([0-9]+\.[0-9]+s\)! For help, type \"help\"" logs/latest.log
		status=$?
	done
	
	#echo -e "\e[36mSet mapSize $MAP_SIZE\e[0m"
	#screen -S $SCREEN -p 0 -X stuff "settings mapSize $MAP_SIZE^M"
	#echo -e "\e[36mStart generating\e[0m"
	#screen -S $SCREEN -p 0 -X stuff "gameadmin generate start^M"
	if [ $FAKE_PLAYERS = "0" ] || [ $FAKE_PLAYERS = "-1" ]; then
		exit 0
	else
		screen -S $SCREEN -p 0 -X stuff "settings minPlayers $FAKE_PLAYERS^M"
		status=1
		while [ $status -eq 1 ]
		do
			sleep 30
			grep -E -q "Generating is finish" logs/latest.log
			status=$?
			if [ $status -eq 1 ]; then
				grep -E -q "Generating is stopped" logs/latest.log
				status=$?
			fi
			if [ $status -eq 1 ]; then
				grep -E -q "World is already generate" logs/latest.log
				status=$?
			fi
		done
		echo -e "\e[36mGenerating is finish\e[0m"
		screen -S $SCREEN -p 0 -X stuff "trfp add $FAKE_PLAYERS^M"
		echo -e "\e[36mAdded $FAKE_PLAYERS players\e[0m"

		status=1
		while [ $status -eq 1 ]
		do
			sleep 30
			grep -E -q "The game has started" logs/latest.log
			status=$?
		done
		T=300 # 5 mins # 3600 = 1h
		echo -e "\e[36mThe game has started for $(T / 1000) secondse[0m"
		sleep $T
		echo -e "\e[36mThe game will stop with random winner\e[0m"
		screen -S $SCREEN -p 0 -X stuff "gameadmin finish @p confirm^M"
		echo -e "\e[36mThe game is going to stop\e[0m"
	fi
}

screen -dmS $SCREEN java -jar $SPIGOT_JAR_NAME && control_server & sleep 30 && screen -x $SCREEN
