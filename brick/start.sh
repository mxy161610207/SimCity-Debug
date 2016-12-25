#!/bin/sh
echo $(hostname)
res=$(ps -ef | grep "python3 sample.py" | grep -v grep)
if [ "$res" = "" ]; then
	python3 sample.py & exit
fi
