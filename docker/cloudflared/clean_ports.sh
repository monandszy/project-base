#!/run/current-system/sw/bin/bash

# Find the process ID (PID) using port 8080
pids=$(lsof -t -i:8080)

# Check if any processes are using the port
if [ -z "$pids" ]; then
  echo "No processes are using port 8080."
  exit 0
fi

# Kill the processes
kill $pids