#!/bin/sh
cmd="poweroff"
for i in {101..110}
do
	let bid=$i-101
	echo "shutdown Brick $bid"
	ssh root@192.168.1.$i $cmd &
done
