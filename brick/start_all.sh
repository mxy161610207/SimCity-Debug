#!/bin/sh
cmd='./start.sh'
for i in {0..9}
do
	echo "start Brick $i"
	ssh robot@192.168.31.11${i} ${cmd}
done
