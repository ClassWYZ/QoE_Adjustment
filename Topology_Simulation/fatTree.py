#! usr/bin/python
# usage: mn --custom <path to fattree.py> --topo fattree[,n] ...

"""Custom topology example

author: Haichen Shen (haichen@cs.washington.edu)

Two directly connected switches plus a host for each switch:

   host --- switch --- switch --- host
"""

from mininet.topo import Topo
from mininet.node import Host
from mininet.node import Node, Controller, RemoteController
from mininet.net import Mininet
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.util import dumpNetConnections, dumpNodeConnections
import json

class FatTree( Topo ):
    "FatTree topology of depth 3."

    def __init__( self, half_ports = 2, **opts ):
        "Create custom topo."

        # Add default members to class.
        #super( FatTree, self ).__init__()
        Topo.__init__(self, **opts)

        aggrs = []
	hnum = 0
        snum = 0
	aggrss = []
        torss = []
	hosts = [] 

        # Create Aggr switches
        for i in range(half_ports):
            snum += 1
            aggrs.append(self.addSwitch('s%s' % snum))
            aggrss.append('s%s' % snum)
	
	base = len(aggrs)
	connections = [["0" for x in range(2 * base)] for x in range(3 * base)]
	pointer = [0 for x in range(3 * base)]

        # Create Tor switches
        for i in range(half_ports*2):
            snum += 1
            sw = self.addSwitch('s%s' % snum)
	    torss.append('s%s' % snum)

            # Connect Tor to Aggr
            for j in range(half_ports):
                self.addLink(sw, aggrs[j])
		
		#print torss[i] + "-" + aggrss[j]
		connections [snum - 1][pointer[snum -1]] = aggrss[j] + "-eth" + str(pointer[j] + 1)
		connections[j][pointer[j]] = torss[i] + "-eth" + str(pointer[snum - 1] + 1)
		pointer[j] += 1
		pointer[snum - 1] += 1
            
	    # Create hosts and links
            for j in range(half_ports):
                hnum += 1
                host = self.addHost('h%s' % hnum)
                self.addLink(sw, host)
		
		#print torss[i] +"-" + "h" + str(hnum)
		connections[snum - 1][pointer[snum - 1]] = "h" + str(hnum) + "-eth0"
		pointer[snum - 1] += 1
	    #print "At the end of the record ..."
	    #print "Let's see what we have here =>"
	    #print connections
	    #print pointer
	
	routemap = {}
	for x in range(3 * base):
	    temp = "s" + str(x + 1)
	    routemap[temp] = {}
	    for y in range(2 * base):
	    	routemap[temp][temp + "-eth" + str(y + 1)] = connections[x][y]
	# Open a file for writing
	output = open("connections.json", "w")
	json.dump(routemap, output, indent = 4)

	# Close the file
	output.close()


def simpleStart():
	"Get things running"
	N = 2 
	topo = FatTree(half_ports = N)
	net = Mininet(topo=topo, controller = None)
	net.addController('c0', controller=RemoteController, ip='127.0.0.1', port=6633)
	net.start()

	#open a file for writing
	out_file = open("detail.json","w")
	detail = {}

	#Build dictionary
	for i in range(N * 4):
		temp = "h" + str(i+1)
		temphost = net.get(temp)

		ip = temphost.IP()
		mac = "00:00:00:00:00:" + format(int(ip[7:]), '02x')
		temphost.setMAC(mac)
	
		#print "writing mac for host " + str(i+1)	
		
		for j in range (N * 4):
			otherhost = net.get("h" + str(j + 1))
			otherhost.setARP(ip, mac)
		detail[temp] = {}
		detail[temp]["ip"] = ip 
		detail[temp]["mac"] =temphost.MAC()
		detail[temp]["interface"] = temphost.intfNames()
		neighbor = []
		for j in range (N * 3):
			dst = "s" + str(j + 1)
			dstnode = net.get(dst)
			if (temphost.connectionsTo(dstnode)):
				#srcintf = temphost.connectionsTo(dst).pop()[0]
				#dstintf = temphost.connectionsTo(dst).pop()[1]
				neighbor.append(dst)
		
		detail[temp]["neighbor"] = neighbor

	for i in range(N * 3):
		temp = "s" + str(i+1)
		tempsw = net.get(temp)
		detail[temp] = {}
		intf = tempsw.intfNames()
		for j in range (len(intf)-1):
			interface = intf[j+1]
			detail[temp][interface] = tempsw.MAC(interface)
		neighbor = []
		for j in range (N * 3):
			if (i != j):
				dst = "s" + str(j+1)
				dstnode = net.get(dst)
				if (tempsw.connectionsTo(dstnode)):
					neighbor.append(dst)
		for j in range (N * 4):
			dst = "h" + str(j + 1)
			dstnode = net.get(dst)
			if (tempsw.connectionsTo(dstnode)):
				neighbor.append(dst)
		detail[temp]["neighbor"] = neighbor
	json.dump(detail, out_file, indent = 4)
	
	#Close the file
	out_file.close()
	CLI(net)

	#topos = { 'fattree': FatTree }

if __name__ == '__main__':
	setLogLevel("info")
	simpleStart()

