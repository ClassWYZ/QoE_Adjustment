Basic environment:
Run Flodlight: java -jar $path/floodlight.jar
Run OVS: sh  start_ovs.sh
Run Ruby(option): (1) rvm 2.0.0 (2) rails s

Program environment:
Use VLC to transmit video from a client to another client(Windows) through an open vswitch installed in Linux(remote server):
Use VLC to transmit video:
Example:
Server(172.16.1.1):vlc -vvv sample1.avi --sout udp:172.16.1.100:1234 --ttl 10
Client(172.16.1.100):vlc udp://@:1234

run socketserver.c on the remote server

run the Client MFC on a Windows based client, building a socket connection with remote server's ip using Client_Demo.exe

Option: you can also use service_controller.rb to work as a remote server controller to control Floodlight.

