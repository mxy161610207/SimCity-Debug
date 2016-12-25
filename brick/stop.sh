#!/bin/sh
pid=$(ps -ef | grep "python3 sample.py" | grep -v grep | awk '{print $2}')
if [ "$pid" != "" ]; then
	kill ${pid}
fi