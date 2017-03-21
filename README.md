# SimCity
It's actually a personal project.
## Flash ev3dev image to Lego EV3 bricks
1. [Getting Started with ev3dev](http://www.ev3dev.org/docs/getting-started/)
1. [Setting Up Wi-Fi Using The Command Line](http://www.ev3dev.org/docs/tutorials/setting-up-wifi-using-the-command-line/)
(or you can just set up Wi-Fi using the interface on the EV3 brick)
1. Update .ssh/known_hosts on PC
(connect EV3 via SSH at the first time to add it to the list of known hosts)
1. Update runtime/known_hosts<br/>
   `user@pc:~/IdeaProjects/SimCity/brick$ ssh-keyscan $IP >> known_hosts`
1. Copy PC's ssh public key to EV3 (both robot's and root's home)<br/>
   `robot@ev3dev:~$ mkdir .ssh`<br/>
   `user@pc:~/IdeaProjects/SimCity/brick$ scp ~/.ssh/id_rsa.pub robot@ev3dev:~/.ssh/authorized_keys`<br/>
   `robot@ev3dev:~$ sudo su`<br/>
   `root@ev3dev:/home/robot# mkdir ~/.ssh`<br/>
   `root@ev3dev:/home/robot# cp .ssh/authorized_keys ~/.ssh/`<br/>
1. Copy sample.py, start.sh, stop.sh to EV3<br/>
   `user@pc:~/IdeaProjects/SimCity/brick$ scp sample.py start.sh stop.sh robot@ev3dev:~`
1. Change root password of EV3<br/>
   `robot@ev3dev:~$ sudo passwd`
1. Change hostname of EV3 by editing /etc/hostname (reboot to take effect)<br/>
   `root@ev3dev:/home/robot# echo $hostname > /etc/hostname`