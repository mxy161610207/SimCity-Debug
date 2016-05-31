#!/bin/sh
cmd="./stop.sh"
for i in {0..9}
do
	echo "stop Brick $i"
	ssh robot@192.168.1.11$i $cmd
done
