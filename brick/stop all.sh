#!/bin/sh
cmd="ps ux | grep sample.py | grep -v grep | awk '{print \$2}' | xargs kill"
for i in {0..9}
do
	echo "killing Brick $i"
	ssh robot@192.168.1.11$i $cmd
done
