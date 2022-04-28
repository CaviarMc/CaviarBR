#!/bin/bash
PLUGINS_PATH=build/libs/CaviarBR.jar
TEST_DIR=test_server

# local github or none
UPDATE_PLUGIN=github
GITHUB_REPO=CaviarMc/CaviarBR
GITHUB_TOKEN=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
GITHUB_JAR_FILE=CaviarBR.jar
GITHUB_URL_API=https://api.github.com/repos/$GITHUB_REPO/releases
GITHUB_USE_PRE_RELEASE=true

function usage {
	echo -e "\e[31m$0 <local|github|none> <serverDir|none>\e[0m"
	exit 1
}

if [ -n "$1" ]; then
	if [ $1 = "local" ] || [ $1 = "github" ]; then
		UPDATE_PLUGIN=$1
	elif [ $1 = "none" ] || [ $1 = "null" ] || [ $1 = "false" ]; then
		UPDATE_PLUGIN=none
	fi
fi

if [ -n "$2" ]; then
  if [ $2 = "none" ]; then
	  TEST_DIR=.
  elif [ ! -d "$2" ]; then
    usage
  else
	  TEST_DIR=$2
    cd $TEST_DIR
  fi
else
    cd $TEST_DIR
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

if [ $UPDATE_PLUGIN = "github" ]; then
	echo -e "\e[36mRécupération du fichier sur le repo github\e[0m"
	if [ $GITHUB_USE_PRE_RELEASE = "true" ]; then
		ASSETS_ID=$(curl -LJsS -H "Authorization: token $GITHUB_TOKEN" "$GITHUB_URL_API" | jq '.[0].assets[] | select(.name == "CaviarBR.jar").id')
	else
	  ASSETS_ID=$(curl -LJsS -H "Authorization: token $GITHUB_TOKEN" "$GITHUB_URL_API/tags/latest" | jq '.assets[] | select(.name == "CaviarBR.jar").id')
	fi
	if [ $? != 0 ]; then
		echo -e "\e[31m$0 ERROR > Unable to get $GITHUB_JAR_FILE from github. Check token access if repo is private\e[0m"
		exit 1
	fi
	cd plugins
	echo -e "\e[36mTéléchargement du fichier sur le repo github\e[0m"
	curl -LJRsS -H "Authorization: token $GITHUB_TOKEN" -H 'Accept: application/octet-stream' $GITHUB_URL_API/assets/$ASSETS_ID -o $GITHUB_JAR_FILE
	if [ $? != 0 ]; then
		echo -e "\e[31m$0 ERROR > Unable to download $GITHUB_JAR_FILE from github.\e[0m"
		exit 1
	fi
	echo -e "\e[36m$GITHUB_JAR_FILE mise à jour par github (asset: $ASSETS_ID)\e[0m"
	cd - &> /dev/null || exit
elif [ $UPDATE_PLUGIN = "local" ]; then
	cd ..
	echo -e "\e[36mGradle jar\e[0m"
	gradle jar
	if [ $? != 0 ]; then
		echo -e "\e[31m$0 ERROR > Build of $GITHUB_JAR_FILE failed\e[0m"
		exit 1
	fi
	cd - &> /dev/null || exit
	cp ../$PLUGINS_PATH plugins/
fi
