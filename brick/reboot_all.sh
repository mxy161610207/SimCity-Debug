#!/bin/sh
cmd="reboot"
for i in {0..9}
do
	echo "reboot Brick $i"
	ssh root@192.168.1.11${i} ${cmd} &
done