# Network Service Discovery

Multiplayer games usually require servers, but they don't have to. Peer to peer networking allows players to connect to each other without the need for a server handling communication. For games that can only be played when everyone is in the same room, peer-to-peer networking provides a low-latency, free alternative to hosting a game server. This tutorial, which follow's [google's own tutorial](https://developer.android.com/training/connect-devices-wirelessly/index), will build an app that uses NSD to discover the IPs and ports of other users on the same network.



## A Quick Overview
Network service discovery (NSD) helps devices find other devices on a local network. Devices advertise services they provide and subscribe to services available on the local net. To use NSD in an app, we must perform the following tasks:
1. Register our service with android's network manager.
   - Choose a `service name` and a `service type`
2. Listen for the service on the local net.
   - Handle newly-discovered peers.

## Implementation

The example code used in this section is available on [GitHub](https://github.com/mattsoulanille/NSDExample).

fLyMd-mAkEr




## Caveats
* Since NSD is purely peer-to-peer, it relies on broadcasting, which may not be supported on some networks. 
* New services should be registered with [IANA](http://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xml) to prevent name conflicts.





#### Bibliography
1. https://developer.android.com/training/connect-devices-wirelessly/nsd
