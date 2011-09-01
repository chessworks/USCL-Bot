#!/bin/bash

basedir=$HOME/bots/USCL-Bot

botname=USCL-Bot
latestJarFile=$basedir/USCL-Bot-latest.jar
stableJarFile=$basedir/USCL-Bot-stable.jar
currentJarFile=$basedir/USCL-Bot-active.jar
runJarFile=$basedir/USCL-Bot-locked.jar

logfile=$basedir/logs/$botname.log
settingsFile=$HOME/.secure/$botname.properties

if [[ "$1" != '--console' ]]; then
	 exec $0 --console $* <&- > $logfile 2>&1 &
	 exit
fi

while true; do
	/bin/cp $currentJarFile $runJarFile
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
			rm $runJarFile
			exit;
			;;
		4)
			echo "Exit code 4: Running backups and then restarting."
			svn commit -m "Player data backup" .
			;;
		5)
			echo "Exit code 5: Recompiling and restarting."
			svn update .
			./build.sh
			if [[ -f $latestJarFile ]]; then
				/bin/cp $latestJarFile $currentJarFile
			fi
			;;
		6)
			echo "Exit code 6: Reverting to prior stable release."
			if [[ -f $stableJarFile ]]; then
				/bin/cp $stableJarFile $currentJarFile
			fi
			;;
		*)
			echo "Exit code $?: Sleeping 20 seconds and restarting."
			sleep 20
			;;
	esac
	/bin/rm $runJarFile
done

