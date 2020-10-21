#!/bin/sh
file="sample.py start.sh"
for i in {0..9}
do
	echo "scp to Brick $i"
	scp ${file} robot@192.168.31.11${i}:~/
done
