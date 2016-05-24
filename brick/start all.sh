#!/bin/sh
cmd='python sample.py & exit'
for i in {0..9}
do
	echo "starting Brick $i"
	ssh robot@192.168.1.11$i $cmd &
done
