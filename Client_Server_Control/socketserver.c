/*************************************
Filename£ºserver.c 
Function: Remote Server
Server Port: 1888

Wenyu Zhang
*************************************/

#include <stdlib.h>
#include <sys/types.h>
#include <stdio.h>
#include <sys/socket.h>
#include <linux/in.h>
#include <string.h>

int main()
{
	/*Define two description charater*/
	int sfp,nfp; 
	struct sockaddr_in s_add,c_add;
	int sin_size;
	
	/*Server port number*/
	unsigned short portnum=1888;

	printf("Hello,welcome to my server !\r\n");
	sfp = socket(AF_INET, SOCK_STREAM, 0);
	if(-1 == sfp)
	{
		printf("socket fail ! \r\n");
		return -1;
	}
	printf("socket ok !\r\n");

	/* Fill in the information of Socket */
	bzero(&s_add,sizeof(struct sockaddr_in));
	s_add.sin_family=AF_INET;
	/*Use all zero address*/
	s_add.sin_addr.s_addr=htonl(INADDR_ANY);
	s_add.sin_port=htons(portnum);
	/* Bind the ports */
	if(-1 == bind(sfp,(struct sockaddr *)(&s_add), sizeof(struct sockaddr)))
	{
		printf("bind fail !\r\n");
		return -1;
	}
	printf("bind ok !\r\n");
	/* Listen port */
	if(-1 == listen(sfp,5))
	{
		printf("listen fail !\r\n");
		return -1;
	}
	printf("listen ok\r\n");

	while(1)
	{
		char ch[1000];
		char ch1[1000];
		char ch2[1000];
		sin_size = sizeof(struct sockaddr_in);
		/* 
		Accept server side function, and wait for the connection from user host (client)
		The 2nd parameter of accept is related information of socket connection.
		*/
		nfp = accept(sfp, (struct sockaddr *)(&c_add), &sin_size);
		if(-1 == nfp)
		{
			printf("accept fail !\r\n");
			return -1;
		}
		read(nfp,&ch,1000);
		int len=atoi(ch);
		int i;
		int len_count=len;
		//Store bit numbers
		int count = 0;
		
		do
		{
			++count;
			len_count/= 10;
		}
		while(len_count>=1);

		for (i=0;i<len;++i) ch[i]=ch[i+count+1];
		ch[len]='\0';
		printf("%d\r\n",len);
		printf("accept ok!\r\nServer start get connect from %#x : %#x\r\n",ntohl(c_add.sin_addr.s_addr),ntohs(c_add.sin_port));
		//printf("%s\r\n",ch);

		/*Execute system command*/
		char *token=strtok(ch,"!");
		sprintf(ch1,"%s\r\n",token);
		system(ch1);
		printf("%s",ch1);
		token=strtok(NULL,"!");
		sprintf(ch2,"%s\r\n",token);
		token=strtok(NULL,"!");
		system(ch2);
		printf("%s",ch2);

		/* Use write to send message to the client */
		if(-1 == write(nfp,"hello,welcome to my server \r\n",32))
		{
			printf("write fail!\r\n");
			return -1;
		}
		printf("write ok!\r\n");

		/*
		system("ovs-vsctl -- set port eth3 qos=@newqos  -- --id=@newqos create qos type=linux-htb other-config:max-rate=10000000 queues=0=@q0,1=@q1 -- --id=@q0 create Queue other-config:min-rate=10000 other-config:max-rate=10000000 other-config:priority=1  -- --id=@q1 create Queue other-config:min-rate=9000000 other-config:max-rate=10000000 other-config:priority=2");
		char second[]="curl -d '{\"switch\": \"00:00:00:1b:21:62:55:b1\", \"name\":\"matchAppThroughUdpPort\", \"cookie\":\"0\",  \"priority\":\"1\",\"protocol\":\"17\", \"src-port\":\"55024\", \"dst-port\":\"1234\", \"ether-type\":\"0x800\",\"dst-ip\":\"192.168.12.11\",\"src-ip\":\"192.168.12.13\",\"active\":\"true\", \"actions\":\"enqueue=3:1\"}' http://192.168.12.178:8080/wm/staticflowentrypusher/json";
		system(second);*/
		close(nfp);
	}
	close(sfp);
	return 0;
}
