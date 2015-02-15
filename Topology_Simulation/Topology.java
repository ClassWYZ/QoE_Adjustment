/*

*/
package edu.columbia.cs6998.sdn.hw1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.util.Iterator;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;

interface ITopology {
    short getNextHop(String ipAddress, String name);
}

class RouteRREntity {
    private List<FinalRoute> routes;
    private short currentInstance;

    RouteRREntity(List<FinalRoute> routes, short currentInstance) {
        this.routes = routes;
        this.currentInstance = currentInstance;
    }

    public List<FinalRoute> getRoutes() {
        return routes;
    }
    
   

    public short getCurrentInstance() {
        return currentInstance;
    }

    public void incrementInstance() {
        currentInstance = (short) ((currentInstance + 1) % routes.size());
    }

    @Override
    public String toString() {
        String retVal = "";
        int i = 0;
        for (FinalRoute route : routes) {
            retVal += "Route# " + i + "=\n" + route.toString();
        }
        return retVal;
    }
}

class FinalRoute {
    private ArrayList<Link> route;
    private int cost;

    FinalRoute() {
        route = new ArrayList<>();
    }

    public FinalRoute(FinalRoute tempRoute) {
        route = new ArrayList<>(tempRoute.getRoute());
        cost = tempRoute.getCost();
    }

    public ArrayList<Link> getRoute() {
        return route;
    }

    public int getCost() {
        return cost;
    }

    short getFirstHopPort() {
        if (!route.isEmpty())
            return route.get(1).getSrcPort();
        else
            return -1; //This should never happen.
    }

    String getFirstHopName() {
        if (!route.isEmpty()) {
	    //System.out.println("route"+route);
            return route.get(1).getPair().getDstEndHost().getName();
	}
        else
            return null; //This should never happen.
    }

    String getLastName() {
        if (!route.isEmpty()) {
	    //System.out.println("getlast"+route.get(route.size()-1).getPair());
            return route.get(route.size() - 1).getPair().getDstEndHost().getName();
	}
        else
            return null; //This should never happen.
    }

    void append(NodeHS node) {
        if(route.isEmpty()) {
            route.add(new Link(new NodeNodePair(null, node)));
        } else {
            NodeHS dstEndHost = route.get(route.size() - 1).getPair().getDstEndHost();
            Link link = new Link(new NodeNodePair(dstEndHost, node));
            for (Map.Entry<Short, Link> entry : dstEndHost.getNeighbors().entrySet()) {
                if (entry.getValue().getPair().getDstEndHost().getName().equals(node.getName())) {
                    link.setSrcPort(entry.getKey());
                    link.setDstPort(entry.getValue().getDstPort());
                }
            }
            route.add(link);
        }
    }

    public void append(ArrayList<Link> param_route) {
        route.addAll(param_route);
    }

    @Override
    public String toString() {
        String retVal = "";
        for (Link link : route) {
            retVal += link.toString();
        }
        return retVal;
    }
}

class Link {
    private NodeNodePair pair;
    private short srcPort;
    private short dstPort;
    private Double cost;

    public Link(NodeNodePair nodeNodePair, Double cost, short srcPort, short dstPort) {
        pair = nodeNodePair;
        this.cost = cost;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
    }

    public Link(NodeNodePair nodeNodePair, short srcPort, short dstPort) {
        this(nodeNodePair, 0.0, srcPort, dstPort);
    }

    public short getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(short srcPort) {
        this.srcPort = srcPort;
    }

    public short getDstPort() {
        return dstPort;
    }

    public void setDstPort(short dstPort) {
        this.dstPort = dstPort;
    }

    Link(NodeNodePair pair) {
        this(pair, 0.0, (short) 0, (short) 0);
    }

    public NodeNodePair getPair() {
        return pair;
    }

    public Double getCost() {
        return cost;
    }

    @Override
    public String toString() {
        String retVal = "(";
        NodeHS srcNode = getPair().getSrcNode();
        NodeHS dstEndHost = getPair().getDstEndHost();
        if(srcNode != null) {
            retVal += srcNode.getName() + "," + getSrcPort();
        }
        retVal += ")->(" + dstEndHost.getName() + "," + getDstPort() + ")";
        return retVal;
    }
}

class NodeNodePair {
    private NodeHS srcNode;
    private NodeHS dstEndHost;

    NodeNodePair(NodeHS srcNode, NodeHS dstEndHost) {
        this.srcNode = srcNode;
        this.dstEndHost = dstEndHost;
    }

    public NodeHS getSrcNode() {
        return srcNode;
    }

    public NodeHS getDstEndHost() {
        return dstEndHost;
    }

    @Override
    public String toString() {
        String retVal = "(";
        if (srcNode != null)
            retVal += srcNode.getName();
        retVal += "->";
        if (dstEndHost != null) 
        	retVal += dstEndHost.getName();
        retVal += ")";
        return retVal;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((dstEndHost == null) ? 0 : dstEndHost.getName().hashCode());
		result = prime * result + ((srcNode == null) ? 0 : srcNode.getName().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		NodeNodePair other = (NodeNodePair) obj;
		if (dstEndHost == null) {
			if (other.dstEndHost != null)
				return false;
		} else if (!dstEndHost.getName().equals(other.dstEndHost.getName()))
			return false;
		if (srcNode == null) {
			if (other.srcNode != null)
				return false;
		} else if (!srcNode.getName().equals(other.srcNode.getName()))
			return false;
		return true;
	}
}

class NodeHS {
    //IP Address of the host
    String ipAddress;
    //Check if the node is an end hostflowMod
    Boolean isHost;

    //Check if the switch is a border switch
    Boolean isBorderSwitch;

    //List of ports this node has
    Map<Short, String> ports;

    //Map of port to neighboring nodes
    Map<Short, Link> neighbors;

    String name;

    NodeHS() {
        ports = new HashMap<>();
        neighbors = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Boolean getIsHost() {
        return isHost;
    }

    public Boolean getIsBorderSwitch() {
        return isBorderSwitch;
    }

    public Map<Short, String> getPorts() {
        return ports;
    }

    public Map<Short, Link> getNeighbors() {
        return neighbors;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addPort(short i, String macAddress) {
        ports.put(i, macAddress);
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setIsHost(Boolean isHost) {
        this.isHost = isHost;
    }

    public void addNeighbor(short port, Link link) {
        neighbors.put(port, link);
    }

    @Override
    public String toString() {
        String retval;
        retval = name + " ->\n";
        for (Map.Entry<Short, Link> entry : getNeighbors().entrySet()) {
            retval += "(" + entry.getKey() + "," +
                    entry.getValue().getPair().getSrcNode().getName() + "-" +
                    entry.getValue().getPair().getDstEndHost().getName() + ")";
        }
        retval += "\n";
        return retval;
    }
}
public class Topology implements ITopology {

	private HashMap<String, NodeHS> topology;
	private HashMap<String, String> ipToNodeMap;
	private HashMap<String, String> macToNodeMap;
	private ArrayList<NodeHS> endHosts;
	private ArrayList<NodeHS> switches;
	private HashMap<NodeNodePair, RouteRREntity> routes;
	//private ArrayList<AppServer> appServers; 

	public final static String LOAD_BALANCER_IP = "10.0.0.254";
	public final static String LOAD_BALANCER_MAC =  "00:00:00:00:00:FE";
	private static final String filePath1 = "/home/mengxue/test/connections.json";
	private static final String filePath2 = "/home/mengxue/test/detail.json";
	private static final int base = 2;

	private static Topology instance;

	public HashMap<String, NodeHS> getTopology() {
		return topology;
	}
	
	 public ArrayList<NodeHS> getSwitches(){
			return switches;
	}

	public static Topology getInstance() {
		if (instance == null) {
			instance = new Topology();
		}
		return instance;
	}

	public HashMap<NodeNodePair, RouteRREntity> getRoutes() {
		return routes;
	}

	private Topology() {
		topology = new HashMap<>();
		ipToNodeMap = new HashMap<>();
		macToNodeMap = new HashMap<>();
		endHosts = new ArrayList<>();
		switches = new ArrayList<>();
		routes = new HashMap<>();
	//	appServers = new ArrayList<>();

		initialize();
	}


	private void initialize() {
		readFromTopoFile();
	//	readFromAppservFile();
		preprocessLinks();
		calculateRoutes();
	}

	private void calculateRoutes() {
		for (NodeHS swtch : switches) {
			for (NodeHS host : endHosts) {
				List<FinalRoute> localRoutes = calcRoutes(swtch, host);
				//System.out.println("localroute"+localRoutes);
				routes.put(new NodeNodePair(swtch, host), new RouteRREntity(localRoutes, (short) 0));
			}
		}
	}

	private void preprocessLinks() {
		for (NodeHS value : topology.values()) {
			if (!value.getIsHost()) {
				switches.add(value);
			} else {
				endHosts.add(value);
			}
		}
	}
/*
	private void readFromAppservFile() {

		URL url=null;

		try {
			url = new File(FILE_APPSERV_INFO).toURI().toURL();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split(":");
				appServers.add(new AppServer(split[1], Short.parseShort(split[2])));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
*/
	private void readFromTopoFile() {
		ArrayList<String> residence = new ArrayList<>();

		try {
			// read the json file
			FileReader reader1 = new FileReader(filePath1);
			FileReader reader2 = new FileReader(filePath2);

			JSONParser jsonParser1 = new JSONParser();
			JSONObject connections = (JSONObject) jsonParser1.parse(reader1);

			JSONParser jsonParser2 = new JSONParser();
			JSONObject detail = (JSONObject) jsonParser2.parse(reader2);

			
			// handle switch connections in the json object
			for (int j = 1; j <= (3 * base); j++ ) {
				String tempName = "s" + Integer.toString(j);
				JSONObject tempsw1 = (JSONObject) connections.get(tempName);
				JSONObject tempsw2 = (JSONObject) detail.get(tempName);
				
				for(int i = 1; i <= (2 * base); i++){
					
					NodeHS node1, node2;
					String intfName = tempName + "-eth" + Integer.toString(i);
					String dstIntfName = tempsw1.get(intfName).toString();
					String dstNode = dstIntfName.substring(0, 2);
					JSONObject tempDstNode = (JSONObject) detail.get(dstNode);
					
					// add source switch
					if (!topology.containsKey(tempName)){
						//add new node pair...
						node1 = addNewNode(tempName, Integer.toString(i), tempsw2.get(intfName).toString(), "127.0.0.1");
						residence.add(intfName);
						if (topology.containsKey(dstNode)){
							node2 = topology.get(dstNode);
							node2.addPort((short)Integer.parseInt(dstIntfName.substring(6)), (String)tempDstNode.get("mac"));
						}
						else{
							if (dstNode.indexOf("h") != (-1)){    
								// then this is a host
								node2 = addNewNode(dstNode, dstIntfName.substring(6), (String)tempDstNode.get("mac"), (String)tempDstNode.get("ip"));
							}
							else{
								node2 = addNewNode(dstNode, dstIntfName.substring(6), (String)tempDstNode.get(dstIntfName), "127.0.0.1");
                       		}
						}
						
						residence.add(dstIntfName);
						Link link1 = new Link(new NodeNodePair(node1, node2), (short)i, (short)Integer.parseInt(dstIntfName.substring(6)));
						Link link2 = new Link(new NodeNodePair(node2, node1), (short)Integer.parseInt(dstIntfName.substring(6)), (short)i);
						node1.addNeighbor(link1.getSrcPort(), link1);
						node2.addNeighbor(link2.getSrcPort(), link2);						
					}
					else if (!residence.contains(intfName)){
						node1= topology.get(tempName);
						node1.addPort((short)i, tempsw2.get(intfName).toString());
						residence.add(intfName);
						if (topology.containsKey(dstNode)){
							node2 = topology.get(dstNode);
							node2.addPort((short)Integer.parseInt(dstIntfName.substring(6)), (String)tempDstNode.get("mac"));
						}
						else{
							if (dstNode.indexOf("h") != (-1)){    
								// then this is a host
								node2 = addNewNode(dstNode, dstIntfName.substring(6), (String)tempDstNode.get("mac"), (String)tempDstNode.get("ip"));
							}
							else{
								node2 = addNewNode(dstNode, dstIntfName.substring(6), (String)tempDstNode.get(dstIntfName), "127.0.0.1");
                       		}
						}
						residence.add(dstIntfName);
						Link link1 = new Link(new NodeNodePair(node1, node2), (short)i, (short)Integer.parseInt(dstIntfName.substring(6)));
						Link link2 = new Link(new NodeNodePair(node2, node1), (short)Integer.parseInt(dstIntfName.substring(6)), (short)i);
						node1.addNeighbor(link1.getSrcPort(), link1);
						node2.addNeighbor(link2.getSrcPort(), link2);	
					}
					
				/*	if (!residence.contains(intfName)){
       					node1 = addNewNode(tempName, Integer.toString(i), tempsw2.get(intfName).toString(), "127.0.0.1");
       					residence.add(intfName);
       					residence.add(dstIntfName);

       					//System.out.print("Name: " + node1.getName() + ", Port: " + node1.getPorts());
       					//System.out.println(" ,Is Host: " + node1.getIsHost()+ "Is border sw: " + node1.getIsBorderSwitch());
       					System.out.print("neighbors: " + node1.getNeighbors());
                        if (dstNode.indexOf("h") != (-1)){    
                        	// then this is a host
							node2 = addNewNode(dstNode, dstIntfName.substring(6), (String)tempDstNode.get("mac"), (String)tempDstNode.get("ip"));
                        }
                        else{
                       		node2 = addNewNode(dstNode, dstIntfName.substring(6), (String)tempDstNode.get(dstIntfName), "127.0.0.1");
                       	}
						System.out.println();
						System.out.println("Node1 Name: " + node1.getName() + ", Port: " + node1.getPorts());
						System.out.println("Node2 Name: " + node2.getName() + ", Port: " + node2.getPorts());
						
						Link link1 = new Link(new NodeNodePair(node1, node2), (short)i, (short)Integer.parseInt(dstIntfName.substring(6)));
						System.out.println("Add link: "+ intfName + "->" + dstIntfName +": Src Port:" + Integer.toString(i) + ", Dst Port:" + dstIntfName.substring(6));
						Link link2 = new Link(new NodeNodePair(node2, node1), (short)Integer.parseInt(dstIntfName.substring(6)), (short)i);
						System.out.println("Add link: "+dstIntfName+"->"+ intfName);
						System.out.println("Src Port:" + dstIntfName.substring(6) +", Dst Port:" + Integer.toString(i));
						
						//addNeighbor(short port, Link link)
						System.out.println("Size before add:" +node1.getNeighbors().size());
						node1.addNeighbor(link1.getSrcPort(), link1);
						System.out.println(node1.getNeighbors());
						System.out.println("Size after add:" +node1.getNeighbors().size());
						System.out.println("add neighbor: Port:" +link1.getSrcPort() + "link:" + link1 );
						node2.addNeighbor(link2.getSrcPort(), link2);
					}
				*/
					
					//System.out.println("Interface: " + intfName + " : " + tempsw.get(intfName));
				}
				
			}
			System.out.println("Reading Topo Ends...Checking Neighbors:");
			for (int j = 1; j <= (3 * base); j++ ) {
				String tempName = "s" + Integer.toString(j);
				NodeHS node = topology.get(tempName);
				System.out.println(tempName + ":" + node.getNeighbors());
			}
			for (int j = 1; j <= (4 * base); j++ ) {
				String tempName = "h" + Integer.toString(j);
				NodeHS node = topology.get(tempName);
				System.out.println(tempName + ":" + node.getNeighbors());
			}
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ParseException ex) {
			ex.printStackTrace();
		} catch (NullPointerException ex) {
			ex.printStackTrace();
		}
	}

	
	private NodeHS addNewNode(String name, String port, String mac, String ip) {
		NodeHS node = new NodeHS();
		node.setName(name);
		node.setIpAddress(ip);
		if (node.getIpAddress().equals("127.0.0.1")){
			node.setIsHost(false);
		}
		else{
			node.setIsHost(true);
		}
		node.addPort(Short.parseShort(port), mac);
		topology.put(node.getName(), node);
		ipToNodeMap.put(node.getIpAddress(),node.getName());
		return node;
	}
/*
	public ArrayList<AppServer> getAppServList(short port) {
		ArrayList<AppServer> ret = new ArrayList<AppServer>();
		
		for(AppServer serv : appServers){
			//short = serv.getPort();
//			System.out.println(serv.getPort()+" "+serv.getIp());
			if(serv.getPortClass() == port) {
				ret.add(serv);
			}
		}
		
		return ret;
		
	}
	
	public short getAppServerPort(String ip) {
		for(AppServer app: appServers){
			if(app.getIp().equals(ip))
				return app.getPort();
		}
		return -1;
	}
*/	
	/* Returns the port number if its the app server else returns -1 
	public short isAppServer(String ip,short port) {
		for(AppServer app : appServers) {
			if(app.getIp().equals(ip) && app.getPort() == port){
				return (short) app.getPortClass();
			}
		}
		return -1;
	}
	*/
	public String getMacFromPort(String name, short port) {
		return topology.get(name).getPorts().get(port);
	}

	public String getMacAddressFromIP(String ipAddress) {
		if (ipAddress.equals(LOAD_BALANCER_IP))
			return LOAD_BALANCER_MAC;

		NodeHS node = getTopology().get(ipToNodeMap.get(ipAddress));
		ArrayList<String> macAddresses = new ArrayList<>(node.getPorts().values());
		return macAddresses.get(0);
	}

	//Ideally this should be calculated from the route
	//and not the neighbor info, but will work for now
	public boolean isNextHop(NodeHS node, String ipAddress) {
		boolean found = false;
		Map<Short, Link> neighbors = node.getNeighbors();
		for (Link link : neighbors.values()) {
			if(link.getPair().getDstEndHost().getName().equals(ipToNodeMap.get(ipAddress))) {
				found = true;
				break;
			}
		}
		return found;
	}

	public static void main(String args[]) {
		HashMap<String, NodeHS> nodeHashMap = getInstance().getTopology();
		System.out.println(nodeHashMap);
		HashMap<NodeNodePair, RouteRREntity> rrEntityHashMap = getInstance().getRoutes();
		System.out.println(rrEntityHashMap);
	}

	private List<FinalRoute> calcRoutes(NodeHS swtch, NodeHS host) {
		List<FinalRoute> retVal = new ArrayList<>();
		Deque<FinalRoute> queue = new ArrayDeque<>();
		String last;
		FinalRoute tempRoute = new FinalRoute();
		FinalRoute newRoute;
		tempRoute.append(swtch);
		queue.push(tempRoute);

		while (!queue.isEmpty()) {
			tempRoute = queue.remove();
			//System.out.println("tempRoute = queue.remove();");
			//System.out.println(tempRoute);
			last = tempRoute.getLastName();
			//System.out.println("Last: " + last);
			
			if (last == host.getName()) {
				retVal.add(new FinalRoute(tempRoute));
				//System.out.println("list"+retVal);
				continue;
			}
			
			for (Link link : getTopology().get(last).getNeighbors().values()) {
				String id = link.getPair().getDstEndHost().getName();
				if (!routeContainsNode(tempRoute, id)) {
					//System.out.println("temproute"+tempRoute.getRoute());
					newRoute = new FinalRoute();
					newRoute.append(tempRoute.getRoute());
					newRoute.append(getTopology().get(id));
					//System.out.println("newRoute: " + newRoute);
					queue.push(newRoute);
				}
			}
		}
		return retVal;
	}

	private boolean routeContainsNode(FinalRoute tempRoute, String id) {
		ArrayList<Link> route = tempRoute.getRoute();
		for (Link link : route) {
			if (link.getPair().getDstEndHost().getName().equals(id)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public short getNextHop(String dstIP, String name) {
		NodeHS srcNode = topology.get(name);

		NodeNodePair pair = new NodeNodePair(srcNode, topology.get(ipToNodeMap.get(dstIP)));
		RouteRREntity routeRREntity = routes.get(pair);
		
	    //System.out.println(routeRREntity);
		List<FinalRoute> finalRoutes = routeRREntity.getRoutes();
        
         //System.out.println(finalRoutes);
		if (finalRoutes.size() == 1) {
			return finalRoutes.get(0).getFirstHopPort();
		} else {
			return selectNext(routeRREntity, srcNode).getFirstHopPort();
		}
	}

	private FinalRoute selectNext(RouteRREntity rrEntity, NodeHS srcNode) {
		/*short currentInstance = rrEntity.getCurrentInstance();
		System.out.println("selectNext: currentInstance:" + String.valueOf(currentInstance));
		FinalRoute finalRoute;
		//Search for available legal path
		do {
			finalRoute = rrEntity.getRoutes().get(currentInstance);
			System.out.println("finalRoute = rrEntity.getRoutes().get(currentInstance);");
			System.out.println(finalRoute = rrEntity.getRoutes().get(currentInstance));
			System.out.println("finalRoute.getFirstHopName()");
			System.out.println(finalRoute.getFirstHopName());
			if (finalRoute.getFirstHopName() != srcNode.getName())
				break;
			rrEntity.incrementInstance();
			finalRoute = null;
			System.out.println("rrEntity.getCurrentInstance()");
			System.out.println(rrEntity.getCurrentInstance());
		} while (rrEntity.getCurrentInstance() != currentInstance);
*/		
		int pathnum = rrEntity.getRoutes().size();
		int minPathHop = 100;
		int pointer = 100;
		for (int i = 0; i < pathnum; i++){
			int size = rrEntity.getRoutes().get(i).getRoute().size();
			if (size < minPathHop){
				minPathHop = size;
				pointer = i;
			}
		}
		FinalRoute finalRoute = rrEntity.getRoutes().get(pointer);

		//Return path same as incoming
		if (finalRoute == null) {
			//Should not happen;
		}
        System.out.println("Inside Final Route");
		System.out.println("finalroute" + finalRoute);
        System.out.println();
		return finalRoute;
	}
}

