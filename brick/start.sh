#!/bin/sh
echo $(hostname)
res=$(ps -ef | grep "python sample.py" | grep -v grep)
if [ "$res" = "" ]; then
	python sample.py & exit
fi
