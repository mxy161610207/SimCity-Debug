#!/bin/sh
cmd='python sample.py & exit'
for i in {101..110}
do
	let bid=$i-101
	echo "starting Brick $bid"
	ssh robot@192.168.1.$i $cmd &
done
