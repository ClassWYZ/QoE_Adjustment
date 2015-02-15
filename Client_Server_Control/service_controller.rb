=begin
An Online QoE Controller Tool based on Floodlight Flow Control in Ruby on Rails
(Ruby Runtime Environment Required)

Wenyu Zhang
=end
class BodsController < ApplicationController
  def index
  end

  def create
#    if params[:protocol].present? || params[:srcIP].present? || params[:srcPort].present?
#       || params[:dstIP].present? || params[:dstPort].present? || params[:minBW].present? || params[:maxBW].present?
      @protocol = params[:protocol] 
      @srcIP =  params[:srcIP]
      @srcPort = params[:srcPort]
      @dstIP = params[:dstIP]
      @dstPort = params[:dstPort]
      @minBW = params[:minBW]
      @maxBW = params[:maxBW]
#    end
    setQueue = %Q{ovs-vsctl -- set port eth3 qos=@newqos  -- --id=@newqos create qos type=linux-htb other-config:max-rate=10000000 queues=0=@q0,1=@q1 -- --id=@q0 create Queue other-config:min-rate=10000 other-config:max-rate=10000000 other-config:priority=1  -- --id=@q1 create Queue other-config:min-rate=#{@minBW} other-config:max-rate=#{@maxBW} other-config:priority=2}
    enqueue = %Q{curl -d '{"switch": "00:00:00:1b:21:62:55:b1", "name":"matchAppThroughUdpPort", "cookie":"0",  "priority":"1","protocol":"#{@protocol}", "src-port":"#{@srcPort}", "dst-port":"#{@dstPort}", "ether-type":"0x800","dst-ip":"#{@dstIP}","src-ip":"#{@srcIP}","active":"true", "actions":"enqueue=1:1"}' http://192.168.12.178:8080/wm/staticflowentrypusher/json}
    system setQueue
    system enqueue
  end
  
end
