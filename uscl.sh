#!/bin/bash

basedir=$HOME/bots/USCL-Bot

botname=USCL-Bot
buildJarFile=$basedir/USCL-Bot.jar
runJarFile=$basedir/USCL-Bot-run.jar

logfile=$basedir/logs/$botname.log
settingsFile=$HOME/.secure/$botname.properties

if [[ "$1" != '--console' ]]; then
	 exec $0 --console $* <&- > $logfile 2>&1 &
	 exit
fi

#while ./build.sh; do
while true; do
	if [[ -f $buildJarFile ]]; then
		/bin/mv $buildJarFile $runJarFile
	fi
	nice -n 5 java -Dusclbot.settingsFile="$settingsFile" -jar $runJarFile
	case $? in
		0)
			echo "Exit code 0: Sleeping 20 seconds and restarting."
			sleep 20
			;;
		1)
			echo "Exit code 1: Sleeping 20 seconds and restarting."
			sleep 20
			;;
		2)
			echo "Exit code 2: Rebooting."
			;;
		3)
			echo "Exit code 3: Shutting down."
			exit;
			;;
		4)
			echo "Exit code 4: Running backups and then restarting."
			svn commit -m "Player data backup" .
			;;
		*)
			echo "Exit code $?: Sleeping 20 seconds and restarting."
			sleep 20
			;;
	esac
done
