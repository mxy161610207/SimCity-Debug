#!/usr/bin/env python
# -*- coding: utf-8 -*-

from ev3dev.auto import *
import socket
import time

IP = '192.168.1.121'
PORT = 9999
ADDR = (IP, PORT)

BID = socket.gethostname()
if BID == '0' or BID == '9':
	SENSORS = 2
elif BID == '1' or BID == '4' or BID == '5' or BID == '8':
	SENSORS = 3
else:
	SENSORS = 4
	
print 'Brick %s plugged in by %d IR sensors' % (BID, SENSORS)

pre = [-1, -1, -1, -1]
sent = [0.0, 0.0, 0.0, 0.0]
sensor = []
for i in range(1, SENSORS + 1):
	sensor.append(InfraredSensor('%s%d' % ('in', i)))

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

#asc = [0, 0, 0, 0]
while True:
	for i in range(SENSORS):
		v = min(99, sensor[i].value())
		cur = time.time()
		if pre[i] != v or cur - sent[i] >= 3:
			try:
				s.sendto('%s%d%02d' % (BID, i, v), ADDR)
				pre[i] = v
				sent[i] = cur
				#asc[i] = (asc[i] + 1) % 100
			except socket.error:
				print "%s: socket.error" % BID
	time.sleep(0.05)
		