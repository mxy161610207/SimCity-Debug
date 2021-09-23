#!/bin/sh
cmd="reboot"
echo "reboot Brick $1"
ssh root@192.168.31.11$1 ${cmd} &
