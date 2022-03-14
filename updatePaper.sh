#!/bin/bash
# Script to update PaperSpigot to last build version available for a Minecraft Version, according to PaperSpigotAPI
# Feature :
# - Download .JAR only if necessery
# - Show commit message from last local update to now
# - Show link to commit and to compare directly to github
# - Handler version change
# - Verify integrity of download file and local one
# - Check OS compatibility (Allowed on Linux & macOS) and if commands used is installed
# - Config to update links, and Minecraft version

SPIGOT_VERSION=1.18.2
SPIGOT_API=https://papermc.io/api/v2/projects/paper/versions/$SPIGOT_VERSION
SAVE_FILE=paperspigot_build_info.txt
SPIGOT_FILE=paperspigot.jar

# TO SEE CHANGELOG
GIT_REPO=https://github.com/PaperMC/Paper
COMMANDS="jq tr curl awk echo rm cat unset sha256sum"

OS=$(uname -s)
if [[ $OS != *"Linux"* ]] && [[ "$OS" != *"Darwin"* ]] && [[ "$OS" != *"Cygwin"* ]] && [[ "$OS" != *"MinGw"* ]]; then
	echo -e "\e[31mERROR > Le script est compatible uniquement avec Linux.\e[0m"
	exit 1
fi

REQUIREMENT=1
for CMD in $COMMANDS
do
	if [ ! "$(command -v "$CMD")" ]; then
		echo -e "\e[31mERROR > $CMD doit être installé.\e[0m"
		REQUIREMENT=0
	fi
done
if [ $REQUIREMENT -eq 0 ]; then
	exit 1
fi

echo -e "\e[36mRécupération de la dernière version de $SPIGOT_FILE pour la $SPIGOT_VERSION...\e[0m"

LAST_BUILD_VERSION=$(curl -s $SPIGOT_API | jq '.builds[-1]')

if [ -f $SAVE_FILE ]; then
	ACTUAL_BUILD_MCVERSION=$(cat $SAVE_FILE | jq -r '.version')
	if [ $? != 0 ]; then
		echo -e "\e[31mFichier d'information $SAVE_FILE erroné, suppression de celui-ci.\e[0m"
		rm -f $SAVE_FILE
	elif [ "$SPIGOT_VERSION" != "$ACTUAL_BUILD_MCVERSION" ]; then
		echo -e "\e[93mWARN > La version de Minecraft a été changé, vérifie si le serveur est compatible avec les plugins\e[0m"
	else
		ACTUAL_BUILD_VERSION=$(cat $SAVE_FILE | jq '.build')
		ACTUAL_COMMIT_ID=$(cat $SAVE_FILE | jq -r '.changes[0].commit')
	fi
fi
NEED_UPDATE=0

SPIGOT_BUILD_INFO_URL=$SPIGOT_API/builds/$LAST_BUILD_VERSION
if [ -f $SPIGOT_FILE ]; then
	SPIGOT_ACTUAL_SHA=$(sha256sum $SPIGOT_FILE | awk '{print $1}')
fi
SPIGOT_BUILD_INFO=$(curl -s "$SPIGOT_BUILD_INFO_URL")
SPIGOT_LAST_SHA=$(echo "$SPIGOT_BUILD_INFO" | jq -r '.downloads.application.sha256')
SPIGOT_LAST_COMMIT_ID=$(echo "$SPIGOT_BUILD_INFO" | jq -r '.changes[0].commit')
SPIGOT_DOWNLOAD=$SPIGOT_BUILD_INFO_URL/downloads/paper-$SPIGOT_VERSION-$LAST_BUILD_VERSION.jar

if [ -z "$ACTUAL_BUILD_VERSION" ] || [ "$LAST_BUILD_VERSION" -gt "$ACTUAL_BUILD_VERSION" ]; then
	if [ -n "$ACTUAL_BUILD_VERSION" ]; then
		echo -e "\e[36m$SPIGOT_FILE doit être mis à jour ...\e[0m"
		echo
		echo -e "\e[36mCommit msgs : \e[0m"
		for (( BUILD_VERSION=ACTUAL_BUILD_VERSION; BUILD_VERSION<=LAST_BUILD_VERSION; BUILD_VERSION++ ))
		do
			echo -n -e "\e[36mBuild $BUILD_VERSION > \e[0m"
			BUILD_VERSION_INFO=$(curl -s $SPIGOT_API/builds/"$BUILD_VERSION")
			for COMMIT_base64 in $(echo "$BUILD_VERSION_INFO" | jq -r '.changes[] | @base64')
			do
				COMMIT=$(echo "$COMMIT_base64" | base64 --decode)
				echo -e "\e[36mCommit $GIT_REPO/commit/$(echo "$COMMIT" | jq -r '.commit') : \e[0m"
				echo "$COMMIT" | jq -r '.message' | tr -d '\n'
				echo
			done
			echo
		done
		echo -e "\e[96mFull Changelog : $GIT_REPO/compare/$ACTUAL_COMMIT_ID...$SPIGOT_LAST_COMMIT_ID\e[0m"
	else
		echo -e "\e[36mAucune version de $SPIGOT_FILE détecté, nous allons récupérer la dernière mise à jour de la $SPIGOT_VERSION...\e[0m"
	fi
	NEED_UPDATE=1
elif [ -z "$SPIGOT_ACTUAL_SHA" ] || [ "$SPIGOT_ACTUAL_SHA" != "$SPIGOT_LAST_SHA" ]; then
	echo -e "\e[31mL'intégrité de $SPIGOT_FILE est invalide, nous allons récupérer la dernière mise à jour de la $SPIGOT_VERSION ...\e[0m"
	NEED_UPDATE=1
fi

if [ $NEED_UPDATE -eq 1 ]; then
	echo -e "\e[96mTéléchargement de la dernière version de $SPIGOT_FILE\e[0m"
	curl -o $SPIGOT_FILE "$SPIGOT_DOWNLOAD"
	if [ "$(sha256sum $SPIGOT_FILE | awk '{print $1}')" = "$SPIGOT_LAST_SHA" ]; then
		echo "$SPIGOT_BUILD_INFO" > $SAVE_FILE
		echo -e "\e[32mMise à jour effectué.\e[0m"
		exit 0
	else
		echo -e "\e[31mL'intégrité de $SPIGOT_FILE qui vient d'être télécharger est invalide. Réesaye et vérifie la qualité du réseau.\e[0m"
		exit 1
	fi
else
	echo -e "\e[32m$SPIGOT_FILE est déjà à jour. Dernière version : $(date -d "$(echo "$SPIGOT_BUILD_INFO" | jq -r '.time')")\e[0m"
	exit 0
fi
