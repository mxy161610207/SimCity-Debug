#!/bin/sh
cmd="poweroff"
for i in {0..9}
do
	echo "shutdown Brick $i"
	ssh root@192.168.31.11${i} ${cmd} &
done
