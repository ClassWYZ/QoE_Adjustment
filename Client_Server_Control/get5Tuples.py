#get5Tuples(windows): used to get network running information from operating system

import os
os.system('netstat -abn > ./netstat.txt')

f = open("netstat.txt","r")
lines = f.readlines()
f.close()
f = open("netstat.txt","w")
flag=0;
for line in lines:
	
	if flag==1:
		flag=0
		continue
	if  not "127.0.0.1" in line:  
		f.write(line)
	else:
		flag=1;

f.close()

f = open("netstat.txt","r")
lines = f.readlines()
f.close()
f = open("netstat.txt","w")
flag=0;
for line in lines:
	
	if flag==1:
		flag=0
		continue
	if  not "::" in line:  
		f.write(line)
	else:
		flag=1;

f.close()

f = open("netstat.txt","r")
lines = f.readlines()
f.close()
f = open("netstat.txt","w")
flag=0;
for line in lines:
	
	if flag==1:
		f.write(line)
		flag=0
		continue
	if  "ESTABLISHED" in line:  
		f.write(line)
		flag=1

f.close()

