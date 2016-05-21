#!/bin/sh
file=~/workspace/SimCity/sample.py
for i in {101..110}
do
	let bid=$i-101
	echo "scp to Brick $bid"
	scp $file robot@192.168.1.$i:~/
done
