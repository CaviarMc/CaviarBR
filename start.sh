#!/bin/sh
screen="$(basename `pwd`)"
file="paperspigot.jar"
ram_max="2G"

./updateSpigot.sh
./updatePlugin.sh

if [ -n "$1" ]; then
	screen -mS $screen java -Xmx$ram_max -jar $file
	./$0
else
	screen -dmS $screen java -Xmx$ram_max -jar $file
	if [ $? = -1 ]; then
		echo "Can't start $screen with file $file"
	fi
fi
