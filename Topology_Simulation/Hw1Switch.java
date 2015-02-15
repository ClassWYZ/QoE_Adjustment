/**
*    Copyright 2014, Columbia University.
*    Homework 1, COMS E6998-10 Fall 2014
*    Software Defined Networking
*    Originally created by Shangjin Zhang, Columbia University
* 
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

/**
 * Floodlight
 * A BSD licensed, Java based OpenFlow controller
 *
 * Floodlight is a Java based OpenFlow controller originally written by David Erickson at Stanford
 * University. It is available under the BSD license.
 *
 * For documentation, forums, issue tracking and more visit:
 *
 * http://www.openflowhub.org/display/Floodlight/Floodlight+Home
 **/

package edu.columbia.cs6998.sdn.hw1;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.openflow.protocol.OFError;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.HexString;
import org.openflow.util.LRULinkedHashMap;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;

/**
 * Module to perform round-robin load balancing.
 * 
 */
public class Hw1Switch implements IOFMessageListener, IFloodlightModule {

	// Interface to Floodlight core for interacting with connected switches
	protected IFloodlightProviderService floodlightProvider;
	
	// Interface to the logging system
	protected static Logger logger;
	
	// IP and MAC address for our logical load balancer
        private final static String LOAD_BALANCER_IP = "10.0.0.254";
	//private final static int LOAD_BALANCER_IP = IPv4.toIPv4Address("10.0.0.254");
	private final static byte[] LOAD_BALANCER_MAC = Ethernet.toMACAddress("00:00:00:00:00:FE");
        

        // flow-mod - for use in the cookie
        public static final int HW1_SWITCH_APP_ID = 10;
        // LOOK! This should probably go in some class that encapsulates
        // the app cookie management
        public static final int APP_ID_BITS = 12;
        public static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
        public static final long HW1_SWITCH_COOKIE = (long) (HW1_SWITCH_APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;	
	// Rule timeouts
	private final static short IDLE_TIMEOUT = 60; // in seconds
	private final static short HARD_TIMEOUT = 0; // infinite
	
	private static class Server
	{
		private String ip;
		private byte[] mac;
		private short port;
		
		public Server(String ip, String mac) {
			super();
            this.ip = ip;
			//this.ip = IPv4.toIPv4Address(ip);
			this.mac = Ethernet.toMACAddress(mac);
			//this.port = port;
		}
		
		public Server() {

                }

		public void setIP(String ip) {
			this.ip = ip;
                }
		public String getIP() {
			return this.ip;
		}
		public void setMAC(byte[] mac) {
			this.mac = mac;
                }
		public byte[] getMAC() {
			return this.mac;
		}
		public void setPort(short port) {
			this.port = port;
		}
		public short getPort() {
			return this.port;
		}
                
	}
	
	final static Server[] SERVERS = {
		new Server("10.0.0.1", "00:00:00:00:00:01"),
		new Server("10.0.0.2", "00:00:00:00:00:02")
	};
	private int lastServer = 0;
  	private Topology topology;	
	/**
	 * Provides an identifier for our OFMessage listener.
	 * Important to override!
	 * */
	@Override
	public String getName() {
		return "hw1switch";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// Auto-generated method stub
		return null;
	}

	/**
	 * Tells the module loading system which modules we depend on.
	 * Important to override! 
	 */
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService >> floodlightService = 
			new ArrayList<Class<? extends IFloodlightService>>();
		floodlightService.add(IFloodlightProviderService.class);
		return floodlightService;
	}

	/**
	 * Loads dependencies and initializes data structures.
	 * Important to override! 
	 */
	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(Hw1Switch.class);
		topology = Topology.getInstance();
	}

	/**
	 * Tells the Floodlight core we are interested in PACKET_IN messages.
	 * Important to override! 
	 * */
	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		floodlightProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
		floodlightProvider.addOFMessageListener(OFType.ERROR, this);
	}
	
        
	/**
	 * Receives an OpenFlow message from the Floodlight core and initiates the appropriate control logic.
	 * Important to override!
	 */
	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		
		switch (msg.getType()) {
            	case PACKET_IN:
                    try {
                       
                	return this.processPacketInMessage(sw, (OFPacketIn) msg, cntx);
                    } catch (Exception e1) {
                       e1.printStackTrace();
                    }
                    break;

            
            case FLOW_REMOVED:
               try {
                return this.processFlowRemovedMessage(sw, (OFFlowRemoved) msg);
               } catch (Exception e) {
                    e.printStackTrace();

               }
               break;
            case ERROR:
                	logger.info("received an error {} from switch {}", (OFError) msg, sw);
               	     return Command.CONTINUE;
            	 default:
               		 break;
        	}
        	logger.error("received an unexpected message {} from switch {}", msg, sw);
        	return Command.CONTINUE;		
        
       
    }
	private Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) throws Exception {

                // Parse the received packet            
                 OFMatch match = new OFMatch();
                 match.loadFromPacket(pi.getPacketData(), pi.getInPort());
                 
                // We only care about TCP packets
                 if (match.getDataLayerType() != Ethernet.TYPE_IPv4 && match.getDataLayerType() != Ethernet.TYPE_ARP) {
                        // Allow the next module to also process this OpenFlow message
                     return Command.CONTINUE;
                 }
              
                 Integer destIPaddress = match.getNetworkDestination();
                 String destIPString = InetAddress.getByAddress( convertto(destIPaddress) ).getHostAddress();
                 Integer srcIPaddress = match.getNetworkSource();
                 String srcIPString = InetAddress.getByAddress( convertto((srcIPaddress))).getHostAddress();
                
                 //System.out.println("Inside ProcessPacketIn");
                 //System.out.println("SrcIP:" + srcIPString + ", DstIP" + destIPString);
                 //System.out.println(match.getDataLayerType());
                 
                 
                 // We only care about packets which are sent to the logical load balancer
                 // System.out.println("PacketIn: switch:"+sw.getId());
                 if ((match.getDataLayerType() == Ethernet.TYPE_ARP)&&(destIPString.equals(LOAD_BALANCER_IP))) {
                     // Receive an ARP request
                     logger.info("Received an ARP request for the load balancer");
                     handleARPRequest(sw, pi, cntx);
                  }else if (destIPString.equals(LOAD_BALANCER_IP)) {
                           // Allow the next module to also process this OpenFlow message
                           logger.info("Received an IPv4 packet destined for the load balancer");
                           loadBalanceFlow(sw, pi, cntx);
                  }else if (!destIPString.equals("255.255.255.255")){
                    	  	// Integer destIPaddress = match.getNetworkDestination();
                          	//String destIPString = InetAddress.getByAddress(convertto(destIPaddress)).getHostAddress();
                          	if (srcIPString.equals(SERVERS[0].getIP()) || srcIPString.equals(SERVERS[1].getIP())) {
                                logger.info("Received an IPv4 Reverse packet asking for next hop");
                                reversecomputeHop(sw, pi, cntx, destIPaddress);
                              }
                            else{
                            		logger.info("Received an IPv4 packet asking for next hop");
                            		computeHop(sw, pi, cntx, destIPaddress);
                            	}
                  }
                // Do not continue processing this OpenFlow message
                 return Command.CONTINUE;
        }
        
	 /**
     * Processes a flow removed message. 
     * @param sw The switch that sent the flow removed message.
     * @param flowRemovedMessage The flow removed message.
     * @return Whether to continue processing this message or stop.
     */
	
     private Command processFlowRemovedMessage(IOFSwitch sw, OFFlowRemoved flowRemovedMessage) {
        if (flowRemovedMessage.getCookie() != Hw1Switch.HW1_SWITCH_COOKIE) {
            return Command.CONTINUE;
        }
        Long sourceMac = Ethernet.toLong(flowRemovedMessage.getMatch().getDataLayerSource());
        Long destMac = Ethernet.toLong(flowRemovedMessage.getMatch().getDataLayerDestination());
        
        if (logger.isTraceEnabled()) {
            logger.trace("{} flow entry removed {}", sw, flowRemovedMessage);
        }
        return Command.CONTINUE;
    }

	/**
	 * Sends a packet out to the switch
	 */
	private void pushPacket(IOFSwitch sw, OFPacketIn pi, 
			ArrayList<OFAction> actions, short actionsLength) {
		
		// create an OFPacketOut for the pushed packet
        OFPacketOut po = (OFPacketOut) floodlightProvider.getOFMessageFactory()
                		.getMessage(OFType.PACKET_OUT);        
        
        // Update the inputPort and bufferID
        po.setInPort(pi.getInPort());
        po.setBufferId(pi.getBufferId());
                
        // Set the actions to apply for this packet		
		po.setActions(actions);
		po.setActionsLength(actionsLength);
	        
        // Set data if it is included in the packet in but buffer id is NONE
        if (pi.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
            byte[] packetData = pi.getPacketData();
            po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
                    + po.getActionsLength() + packetData.length));
            po.setPacketData(packetData);
        } else {
            po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
                    + po.getActionsLength()));
        }        
        
        // Push the packet to the switch
        try {
            sw.write(po, null);
        } catch (IOException e) {
            logger.error("failed to write packetOut: ", e);
        }
	}

	/**
	 * Handle ARP Request and reply it with load balancer's MAC address
	 */
	private void handleARPRequest(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		
		System.out.println("Handle ARP request");
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		if (! (eth.getPayload() instanceof ARP)){
			System.out.println("Not an instance of ARP!");
			return;
		}
		ARP arpRequest = (ARP) eth.getPayload();
        long srcIPAddress = IPv4.toIPv4Address(arpRequest.getSenderProtocolAddress());
        long srcMACAddress = Ethernet.toLong(arpRequest.getSenderHardwareAddress());
        long targetIPAddress = IPv4.toIPv4Address(arpRequest.getTargetProtocolAddress());
		String ipaddress = null;
                
		try {       
	         ipaddress = InetAddress.getByAddress(convertto((int)targetIPAddress)).getHostAddress();
             //ipaddressbyte = ipaddress.getAddress();	
        } catch (UnknownHostException e) {
			 e.printStackTrace();
        }
        long targetMACAddress = Ethernet.toLong(Ethernet.toMACAddress(topology.getMacAddressFromIP(ipaddress)));
        // logger.info("arprequest"+ipaddressbyte);
		
       
        // generate ARP reply
		IPacket arpReply = new Ethernet()
			.setSourceMACAddress(Ethernet.toByteArray(targetMACAddress))
			.setDestinationMACAddress(Ethernet.toByteArray(srcMACAddress))
			.setEtherType(Ethernet.TYPE_ARP)
			.setPriorityCode(eth.getPriorityCode())
			.setPayload(
				new ARP()
				.setHardwareType(ARP.HW_TYPE_ETHERNET)
				.setProtocolType(ARP.PROTO_TYPE_IP)
				.setHardwareAddressLength((byte) 6)
				.setProtocolAddressLength((byte) 4)
				.setOpCode(ARP.OP_REPLY)
				.setSenderHardwareAddress(Ethernet.toByteArray(targetMACAddress))
				.setSenderProtocolAddress(IPv4.toIPv4AddressBytes((int)targetIPAddress))
				.setTargetHardwareAddress(arpRequest.getSenderHardwareAddress())
				.setTargetProtocolAddress(arpRequest.getSenderProtocolAddress()));
		
		sendARPReply(arpReply, sw, OFPort.OFPP_NONE.getValue(), pi.getInPort());
	}
	
	/**
	 * Sends ARP reply out to the switch
	 */
	private void sendARPReply(IPacket packet, IOFSwitch sw, short inPort, short outPort) {
		System.out.println("sendARPReply!");
		// Initialize a packet out
		OFPacketOut po = (OFPacketOut) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.PACKET_OUT);
		po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		po.setInPort(inPort);
		
		// Set output actions
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(new OFActionOutput(outPort, (short) 0xffff));
		po.setActions(actions);
		po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);
		
		// Set packet data and length
		byte[] packetData = packet.serialize();
		po.setPacketData(packetData);
		po.setLength((short) (OFPacketOut.MINIMUM_LENGTH + po.getActionsLength() + packetData.length));
		
		// Send packet
		try {
			sw.write(po, null);
			sw.flush();
		} catch (IOException e) {
			logger.error("Failure writing packet out", e);
		}
	}
        byte[] convertto(int bytes) {
              return new byte[] {
                     (byte)((bytes >>> 24) & 0xff),
                     (byte)((bytes >>> 16) & 0xff),
                     (byte)((bytes >>> 8) & 0xff),
                     (byte)((bytes     ) & 0xff)
              };
 
        }
        
        int convertfrom(byte[] bytes) {
        	int val = 0;
        	for (int i = 0; i < bytes.length; i++ ) {
        		val <<= 8;
        		val |= bytes[i] & 0xff;
        	}
        	return val;
        }

        /**
         * Give next hop switch
        */
        private void reversecomputeHop(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx, int destIPaddress) throws Exception {
                String destIPString = null;
                try {
                    destIPString = Inet4Address.getByAddress(convertto(destIPaddress)).getHostAddress();

                } catch (UnknownHostException e) {

                        e.printStackTrace();
                }
                System.out.println("switch: "+sw.getId()+", reversecomputeHop - DstIP: "+destIPString);
                short switchport = topology.getNextHop(destIPString, "s" + sw.getId());
                //
                System.out.println("s" + sw.getId()+", outport: " + switchport);
                //System.out.println("sw"+sw.getId());
                Server server = new Server();
                server.setPort(switchport);
                server.setIP(destIPString);
                String destMac = topology.getMacAddressFromIP(destIPString);
                server.setMAC(Ethernet.toMACAddress(destMac));
                Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                                IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

                // Create a flow table modification message to add a rule
                OFFlowMod rule = new OFFlowMod();
                rule.setType(OFType.FLOW_MOD);
                rule.setCommand(OFFlowMod.OFPFC_ADD);

                // Create match 
                OFMatch match = new OFMatch();
                //        .setDataLayerDestination(destMac)
                //        .setDataLayerSource(eth.getSourceMACAddress())
                //        .setDataLayerType(Ethernet.TYPE_IPv4)
                //        .setNetworkDestination(destIPaddress)
                //        .setNetworkSource(((IPv4) eth.getPayload()).getSourceAddress())
                //        .setInputPort(pi.getInPort());

                // Set wildcards for Network protocol
                //not sure
                match.setWildcards(OFMatch.OFPFW_NW_PROTO);
                rule.setMatch(match);

                // Specify the timeouts for the rule
                rule.setIdleTimeout(IDLE_TIMEOUT);
                rule.setHardTimeout(HARD_TIMEOUT);

                // Set the buffer id to NONE -- implementation artifact
                rule.setBufferId(OFPacketOut.BUFFER_ID_NONE);

                // Initialize list of actions
                ArrayList<OFAction> actions = new ArrayList<OFAction>();

                  // Add action to re-write destination MAC to the MAC of the chosen server
                OFAction rewriteMAC = new OFActionDataLayerSource(server.getMAC());
                actions.add(rewriteMAC);

                // Add action to re-write destination IP to the IP of the chosen server
                OFAction rewriteIP = new OFActionNetworkLayerSource( convertfrom(InetAddress.getByName(server.getIP()).getAddress()));
                actions.add(rewriteIP);


                // Add action to output packet
                OFAction outputTo = new OFActionOutput(server.getPort());
                actions.add(outputTo);

                // Add actions to rule
                rule.setActions(actions);
               short actionsLength = (short)(OFActionDataLayerSource.MINIMUM_LENGTH
                                + OFActionNetworkLayerSource.MINIMUM_LENGTH
                                + OFActionOutput.MINIMUM_LENGTH);
                // short actionsLength = (short) (OFActionOutput.MINIMUM_LENGTH);
                // Specify the length of the rule structure
                rule.setLength((short) (OFFlowMod.MINIMUM_LENGTH + actionsLength));

                //logger.debug("Actions length="+ (rule.getLength() - OFFlowMod.MINIMUM_LENGTH));


                try {
                        sw.write(rule, null);
                } catch (Exception e) {
                        e.printStackTrace();
                }
                pushPacket(sw, pi, actions, actionsLength);

        }




	/**
         * Give next hop switch
        */
        private void computeHop(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx, int destIPaddress) throws Exception {
		
        	String destIPString = null;
            try {
            		destIPString = Inet4Address.getByAddress(convertto(destIPaddress)).getHostAddress();
            } catch (UnknownHostException e) {
            		e.printStackTrace();
            }
            System.out.println();
            System.out.println("Inside switch: "+sw.getId() + " => Dst:"+destIPString);
            System.out.println();
            short switchport = topology.getNextHop(destIPString, "s" + sw.getId());
            //System.out.println("after return getNextHop:");
            System.out.println(switchport);
       
            Server server = new Server();
            server.setPort(switchport);
            server.setIP(destIPString);
            String destMac = topology.getMacAddressFromIP(destIPString);
            server.setMAC(Ethernet.toMACAddress(destMac));
            System.out.println("Server: " + server);
            Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                                IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

            // Create a flow table modification message to add a rule
            OFFlowMod rule = new OFFlowMod();
            rule.setType(OFType.FLOW_MOD);
            rule.setCommand(OFFlowMod.OFPFC_ADD);
                
            // Create match 
            OFMatch match = new OFMatch();
            //        .setDataLayerDestination(destMac)
            //        .setDataLayerSource(eth.getSourceMACAddress())
            //        .setDataLayerType(Ethernet.TYPE_IPv4)
            //        .setNetworkDestination(destIPaddress)
            //        .setNetworkSource(((IPv4) eth.getPayload()).getSourceAddress())
            //        .setInputPort(pi.getInPort());

            // Set wildcards for Network protocol
            //not sure
            match.setWildcards(OFMatch.OFPFW_NW_PROTO);
            rule.setMatch(match);

            // Specify the timeouts for the rule
            rule.setIdleTimeout(IDLE_TIMEOUT);
            rule.setHardTimeout(HARD_TIMEOUT);

            // Set the buffer id to NONE -- implementation artifact
            rule.setBufferId(OFPacketOut.BUFFER_ID_NONE);

            // Initialize list of actions
            ArrayList<OFAction> actions = new ArrayList<OFAction>();

            // Add action to output packet
            OFAction outputTo = new OFActionOutput(server.getPort());
            actions.add(outputTo);

            // Add actions to rule
            rule.setActions(actions);
            // short actionsLength = (short)(OFActionDataLayerDestination.MINIMUM_LENGTH
            //                 + OFActionNetworkLayerDestination.MINIMUM_LENGTH
            //                + OFActionOutput.MINIMUM_LENGTH);
            short actionsLength = (short)(OFActionOutput.MINIMUM_LENGTH);
            // Specify the length of the rule structure
            rule.setLength((short) (OFFlowMod.MINIMUM_LENGTH + actionsLength));

            //logger.debug("Actions length="+ (rule.getLength() - OFFlowMod.MINIMUM_LENGTH));
            //System.out.println("Actions length= " + (rule.getLength() - OFFlowMod.MINIMUM_LENGTH));

            //logger.debug("Install rule for forward direction for flow: " + rule);

            try {
                	sw.write(rule, null);
            } catch (Exception e) {
                    e.printStackTrace();
            }
       
                /*
                // Create a flow table modification message to add a rule for the reverse direction
                OFFlowMod reverseRule = new OFFlowMod();
                reverseRule.setType(OFType.FLOW_MOD);
                reverseRule.setCommand(OFFlowMod.OFPFC_ADD);

                // Create match 
                OFMatch reverseMatch = new OFMatch()
                        .setDataLayerSource(server.getMAC())
                        .setDataLayerDestination(match.getDataLayerSource())
                        .setDataLayerType(Ethernet.TYPE_IPv4)
                        .setNetworkSource(destIPaddress)
                        .setNetworkDestination(match.getNetworkSource())
                        .setInputPort(server.getPort());
                // Set wildcards for Network protocol
                reverseMatch.setWildcards(OFMatch.OFPFW_NW_PROTO);
                reverseRule.setMatch(reverseMatch);

                // Specify the timeouts for the rule
                reverseRule.setIdleTimeout(IDLE_TIMEOUT);
                reverseRule.setHardTimeout(HARD_TIMEOUT);

                // Set the buffer id to NONE -- implementation artifact
                reverseRule.setBufferId(OFPacketOut.BUFFER_ID_NONE);

                // Initialize list of actions
                ArrayList<OFAction> reverseActions = new ArrayList<OFAction>();


                // Add action to output packet
                OFAction reverseOutputTo = new OFActionOutput(pi.getInPort());
                reverseActions.add(reverseOutputTo);

                // Add actions to rule
                reverseRule.setActions(reverseActions);

                // Specify the length of the rule structure
                reverseRule.setLength((short) (OFFlowMod.MINIMUM_LENGTH
                                + OFActionDataLayerSource.MINIMUM_LENGTH
                                + OFActionNetworkLayerSource.MINIMUM_LENGTH
                                + OFActionOutput.MINIMUM_LENGTH));

                logger.debug("Install rule for reverse direction for flow: " + reverseRule);

                try {
                        sw.write(reverseRule, null);
                } catch (Exception e) {
                        e.printStackTrace();
                }
                */
           pushPacket(sw, pi, actions, actionsLength);

        }

     /**
	 * Performs load balancing based on a packet-in OpenFlow message for an 
	 * IPv4 packet destined for our logical load balancer.
	 */
	private void loadBalanceFlow(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) throws Exception {
		
		Server server = getNextServer();
	    String destIPString= server.getIP(); 
        System.out.println("loadbalancer: request switch: "+sw.getId()+" DestIP: " + destIPString);
                
        short outport = topology.getNextHop(destIPString, "s"+sw.getId());
		System.out.println("next Hop output port: " + outport);
		server.setPort(outport);
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		// Create a flow table modification message to add a rule
    	OFFlowMod rule = new OFFlowMod();
		rule.setType(OFType.FLOW_MOD); 			
		rule.setCommand(OFFlowMod.OFPFC_ADD);
					
		// Create match 
		OFMatch match = new OFMatch();
	/*      	.setDataLayerDestination(new String(Hw1Switch.LOAD_BALANCER_MAC, "UTF-8"))
			.setDataLayerSource(eth.getSourceMACAddress())
			.setDataLayerType(Ethernet.TYPE_IPv4)
			.setNetworkDestination(convertfrom(InetAddress.getByName(Hw1Switch.LOAD_BALANCER_IP).getAddress()))
			.setNetworkSource(((IPv4) eth.getPayload()).getSourceAddress())
			.setInputPort(pi.getInPort());
*/
		match.loadFromPacket(pi.getPacketData(), pi.getInPort());
        // Set wildcards for Network protocol
		match.setWildcards(OFMatch.OFPFW_ALL);
		rule.setMatch(match);
			
		// Specify the timeouts for the rule
		rule.setIdleTimeout(IDLE_TIMEOUT);
		rule.setHardTimeout(HARD_TIMEOUT);
	        
	    // Set the buffer id to NONE -- implementation artifact
		rule.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	       
        // Initialize list of actions
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		
		// Add action to re-write destination MAC to the MAC of the chosen server
		OFAction rewriteMAC = new OFActionDataLayerDestination(server.getMAC());
		actions.add(rewriteMAC);
		
		// Add action to re-write destination IP to the IP of the chosen server
		OFAction rewriteIP = new OFActionNetworkLayerDestination( convertfrom(InetAddress.getByName(server.getIP()).getAddress()));
		actions.add(rewriteIP);
			
		// Add action to output packet
		OFAction outputTo = new OFActionOutput(server.getPort());
		actions.add(outputTo);
		
		// Add actions to rule
		rule.setActions(actions);
		short actionsLength = (short)(OFActionDataLayerDestination.MINIMUM_LENGTH
				+ OFActionNetworkLayerDestination.MINIMUM_LENGTH
				+ OFActionOutput.MINIMUM_LENGTH);
		
		// Specify the length of the rule structure
		rule.setLength((short) (OFFlowMod.MINIMUM_LENGTH + actionsLength));
		
		
		try {
			sw.write(rule, null);
		} catch (Exception e) {
			e.printStackTrace();
		}	
/*
		// Create a flow table modification message to add a rule for the reverse direction
    	      OFFlowMod reverseRule = new OFFlowMod();
         	reverseRule.setType(OFType.FLOW_MOD); 			
    	        reverseRule.setCommand(OFFlowMod.OFPFC_ADD);
			
		// Create match 
		OFMatch reverseMatch = new OFMatch()
			.setDataLayerSource(server.getMAC())
			.setDataLayerDestination(match.getDataLayerSource())
			.setDataLayerType(Ethernet.TYPE_IPv4)
			.setNetworkSource(convertfrom(InetAddress.getByName(server.getIP()).getAddress()))
			.setNetworkDestination(match.getNetworkSource())
			.setInputPort(server.getPort());
        
		// Set wildcards for Network protocol
		reverseMatch.setWildcards(OFMatch.OFPFW_NW_PROTO);
		reverseRule.setMatch(reverseMatch);
			
		// Specify the timeouts for the rule
		reverseRule.setIdleTimeout(IDLE_TIMEOUT);
		reverseRule.setHardTimeout(HARD_TIMEOUT);
	        
	    // Set the buffer id to NONE -- implementation artifact
		reverseRule.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	       
        // Initialize list of actions
		ArrayList<OFAction> reverseActions = new ArrayList<OFAction>();
	        int LOAD_BALANCER_IP_int = convertfrom(InetAddress.getByName(LOAD_BALANCER_IP).getAddress());	
		// Add action to re-write destination MAC to the MAC of the chosen server
		OFAction reverseRewriteMAC = new OFActionDataLayerSource(LOAD_BALANCER_MAC);
		reverseActions.add(reverseRewriteMAC);
		
		// Add action to re-write destination IP to the IP of the chosen server
		OFAction reverseRewriteIP = new OFActionNetworkLayerSource(LOAD_BALANCER_IP_int);
		reverseActions.add(reverseRewriteIP);
			
		// Add action to output packet
		OFAction reverseOutputTo = new OFActionOutput(pi.getInPort());
		reverseActions.add(reverseOutputTo);
		
		// Add actions to rule
		reverseRule.setActions(reverseActions);
		
		// Specify the length of the rule structure
		reverseRule.setLength((short) (OFFlowMod.MINIMUM_LENGTH
				+ OFActionDataLayerSource.MINIMUM_LENGTH
				+ OFActionNetworkLayerSource.MINIMUM_LENGTH
				+ OFActionOutput.MINIMUM_LENGTH));
		
		logger.debug("Install rule for reverse direction for flow: " + reverseRule);
			
		try {
			sw.write(reverseRule, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
*/		
		pushPacket(sw, pi, actions, actionsLength);
	}
	
	/**
	 * Determines the next server to which a flow should be sent.
	 */
	private Server getNextServer() {
		lastServer = (lastServer + 1) % SERVERS.length;
		return SERVERS[lastServer];
	}

}
