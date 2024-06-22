#!/run/current-system/sw/bin/bash
LOG_FILE=./gradle/prod.log

bash docker/tunnel/clean_ports.sh $LOG_FILE 2>&1
gradle composeProdUp $LOG_FILE 2>&1
bash docker/tunnel/run_tunnel.sh &
sleep 10 & pid=$!
kill $pid