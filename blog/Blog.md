# Network Service Discovery

Multiplayer games usually require servers, but they don't have to. Peer to peer networking allows players to connect to each other without the need for a server handling communication. For games that can only be played when everyone is in the same room, peer-to-peer networking provides a low-latency, free alternative to hosting a game server. This tutorial, which follows [Google's own tutorial](https://developer.android.com/training/connect-devices-wirelessly/index), will build an app that uses NSD to discover the IPs and ports of other users on the same network.



## A Quick Overview
Network service discovery (NSD) helps devices find other devices on a local network. Devices advertise services they provide and subscribe to services available on the local net. To use NSD in an app, we must perform the following tasks:
1. Register the service with android's network manager.
   - Choose a `service name` and a `service type`
2. Listen for the service on the local net.
   - Handle newly-discovered peers.

## Implementation

The example code used in this section is available on [GitHub](https://github.com/mattsoulanille/NSDExample).

#### Registering the Service
The `service name` is a unique identifier for the service the app provides. In this case, we use `NsdChat` as the service name. The `service type` specifies the protocol and transport used by the service. This example app will be using http over tcp, so it's service type is `_http._tcp.`.

To register this service, we need to choose a port. This port should not be hard-coded as it may not be available (bound to another app's process). Instead of hardcoding it, we ask android for the first unused port 
```java
public void initializeServerSocket() throws IOException {
    // Get the next available port by asking for port 0
    mServerSocket = new ServerSocket(0);

    // Get the actual port we've been assigned
    mLocalPort = mServerSocket.getLocalPort();
}
```

After we have a port, we can register the service to that port. This tells android to advertise the service on the local net. 

```java
public void registerService(int port) {
    NsdServiceInfo serviceInfo = new NsdServiceInfo();

    // Name may change due to name conflicts with other services
    // so always make sure to check what it actually ends up being
    // This name is visible to other devices on the network that are
    // using NSD to look for local services.
    serviceInfo.setServiceName(SERVICE_NAME); // "NsdChat"

    // Specifies protocol and transport
    // _protocolname._transportname
    serviceInfo.setServiceType(SERVICE_TYPE); // "_http._tcp."
    serviceInfo.setPort(port);

    // Get the NsdManager from the system and register our service
    mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);

    initializeRegistrationListener(); // sets mRegistrationListener
    
    // This is an async process that calls
    // mRegistrationListener's onServiceRegistered function when it succeeds
    mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
}
```
Registering a service is an async process that reports success or failure through a `RegistrationListener`. In the above block of code, `mRegistrationListener` is our registration listener. When a service is registered, it saves the name it ended up with in `mServiceName` (not always `SERVICE_NAME` due to possible conflicts).
```java
...
public void onServiceRegistered(NsdServiceInfo serviceInfo) {
    String service = serviceInfo.toString();
    mServiceName = serviceInfo.getServiceName();
    Log.i(TAG, "Registered service " + mServiceName + "\n" + service);
}

...
```
#### Connecting to the service
Now that the service is registered, it should be visible to other phones looking for it. If there are other phones running this app on the same local net, we should be able to find their service as well. To do that, we ask android to look for the service:
```java
public void discoverServices() {
    initializeResolveListener(); // sets mResolveListener
    initializeDiscoveryListener(); // sets mDiscoveryListener
    mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
}
```
Similar to registering a service, discovering services is asynchronous, so a callback object `mDiscoveryListener` is used to handle errors and successes.
```java
public void initializeDiscoveryListener() {
    mDiscoveryListener = new NsdManager.DiscoveryListener() {
        ⋮         
        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            // A service was found
            Log.i(TAG, "Service discovery found service " + serviceInfo);

            if (! serviceInfo.getServiceType().equals(SERVICE_TYPE)) {
                Log.i(TAG, "Unknown service type: " + serviceInfo.getServiceType() + "\n"
                           + "Supported type is " + SERVICE_TYPE);
            }
            else if (serviceInfo.getServiceName().equals(mServiceName)) {
                // The name of the service tells you who you're connecting to.
                Log.i(TAG, "Found ourselves");
            }
            else if (serviceInfo.getServiceName().contains(SERVICE_NAME)) {
                // Found another machine
                Log.i(TAG,"Found a peer to connect to: " + serviceInfo.getServiceName());
                mNsdManager.resolveService(serviceInfo, mResolveListener);
            }
        }
        ⋮
    };
}
```
When a device providing a service is found by `mNsdManager`, `onServiceFound` is called. `mNsdManager` makes no guarantee that the service the device is advertising is the service we're looking for, so we check that the `SERVICE_TYPE` matches. Service names are unique, so if the service name of the service we found is equal to our service name, then we've found ourselves. If the service name contains `SERVICE_NAME` but does not equal our `mServiceName`, then we've found another phone running our app. To connect to them, we need to resolve their IP and port from the service which is what `mResolveListener` does.
```java
// Listener called after service is discovered
public void initializeResolveListener() {
    mResolveListener = new NsdManager.ResolveListener() {
        ⋮
        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded: " + serviceInfo);
            
			// Check service name again just to be sure
            if (serviceInfo.getServiceName().equals(mServiceName)) {
                Log.d(TAG, "Somehow got ourselves again. Same IP.");
                return;
            }

            NsdServiceInfo service = serviceInfo;
            int port = service.getPort();
            InetAddress host = service.getHost();

            Log.d(TAG, "Port: " + port);
            Log.d(TAG, "Host: " + host);

            Toast toast = Toast.makeText(mContext, "Found peer " + host, Toast.LENGTH_SHORT);
            toast.show();

            mPeers.put(host, service);
            updatePeerList();
            notifyChanged();
        }
    };

}
```
Pulling the port and IP (host) out of a service is as simple as calling the appropriate method of the `NsdServiceInfo` object. At this point, NSD has performed its function, and you can use whatever networking code you want to connect the devices to each other.


#### Cleaning Up

NSD is expensive, so it should not be run in the background or after the application is closed. The following code unregisters the service.
```java
public void tearDown() {
	// Unregister the service from Nsd
	// and stop looking for the service
	mNsdManager.unregisterService(mRegistrationListener);
	mNsdManager.stopServiceDiscovery(mDiscoveryListener);
}
```




## Images of the Example App

![Nexus](https://github.com/mattsoulanille/NSDExample/blob/master/blog/images/Nexus%205x.png)

![Motorola](https://github.com/mattsoulanille/NSDExample/tree/master/blog/images/Moto20%g4.png)





## Caveats
* Since NSD is purely peer-to-peer, it relies on broadcasting, which may not be supported on some networks. 
* New services should be registered with [IANA](http://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xml) to prevent name conflicts.





#### Bibliography
1. https://developer.android.com/training/connect-devices-wirelessly/nsd
