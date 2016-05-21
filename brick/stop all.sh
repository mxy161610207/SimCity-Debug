#!/bin/sh
cmd="ps ux | grep sample.py | grep -v grep | awk '{print \$2}' | xargs kill"
for i in {101..110}
do
	let bid=$i-101
	echo "killing Brick $bid"
	ssh robot@192.168.1.$i $cmd
done
