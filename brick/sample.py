#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from ev3dev.ev3 import *
import socket
import time, threading, sys

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

print('Brick %s plugged in by %d IR sensors' % (BID, SENSORS))

pre = [-1, -1, -1, -1]
sent = [0.0, 0.0, 0.0, 0.0]
sensor = []
for i in range(1, SENSORS + 1):
    sensor.append(InfraredSensor('in%d' % i))

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

sync = False
offset = 0.0
sync_sent = 0
sync_id = 0
# use Cristian's algorithm to synchronize clock
def recv_handler():
    global sync, offset, sync_sent, sync_id
    rtt = sys.maxsize
    while True:
        data, addr = s.recvfrom(20)
        sync_recv = time.time()
        recv_id = int(data[0:2].decode('utf-8'))
        if sync_id == recv_id and sync_recv - sync_sent < rtt:
            rtt = sync_recv - sync_sent
            offset = int(data[2:].decode('utf-8')) + rtt/2 - sync_recv*1000
            sync = True

t = threading.Thread(target=recv_handler)
t.setDaemon(True)
t.start()

count = 0
while True:
    if sync:
        for i in range(SENSORS):
            v = min(98, sensor[i].value())
            cur = time.time()
            if pre[i] != v or cur - sent[i] >= 3:
                try:
                    s.sendto('{0}{1}{2:02d}{3}'.format(BID, i, v, int(cur*1000+offset)).encode(), ADDR)
                    pre[i] = v
                    sent[i] = cur
                except socket.error:
                    print("%s: socket.error" % BID)

    if count == 0:
        sync_id = sync_id + 1 if sync_id <= 98 else 0
        sync_sent = time.time()
        s.sendto('{0:02d}{1:02d}'.format(sync_id, 99).encode(), ADDR) # clock synchronization request
    count = count + 1 if count <= 198 else 0 # one request per 200 * 0.05 s
    time.sleep(0.05)
