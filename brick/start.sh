#!/bin/sh
echo $(hostname)
res=$(ps -ef | grep "python3 sample.py" | grep -v grep)
if [ "$res" = "" ] && [ $# = 1 ]
then
	python3 sample.py $1 & exit
fi
