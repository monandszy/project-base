#!/run/current-system/sw/bin/bash
LOG_FILE=./gradle/dev.log

gradle composeDevUp >> $LOG_FILE 2>&1