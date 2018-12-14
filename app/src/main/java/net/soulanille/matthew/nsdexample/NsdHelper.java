package net.soulanille.matthew.nsdexample;

import android.content.Context;
import android.database.DataSetObservable;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;


public class NsdHelper extends DataSetObservable {

    private static String SERVICE_TYPE = "_http._tcp.";
    private static String SERVICE_NAME = "NsdChat";
    private static String TAG = "NsdHelper";
    private ServerSocket mServerSocket;
    public int mLocalPort;
    private NsdManager.RegistrationListener mRegistrationListener;
    private String mServiceName;
    private Context mContext;
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private HashMap<InetAddress, NsdServiceInfo> mPeers;
    private ArrayList<String> addressList;


    public NsdHelper(Context context) throws IOException {
        mContext = context;
        initializeServerSocket();
        mPeers = new LinkedHashMap<InetAddress, NsdServiceInfo>();
        addressList = new ArrayList<String>();


        // These are called by MainActivity
        //registerService(mLocalPort);
        //discoverServices();
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        // Name may change due to name conflicts with other services
        // so always make sure to check again what it actually ends up being
        // This name is visible to other devices on the network that are
        // using NSD to look for local services.
        serviceInfo.setServiceName(SERVICE_NAME);

        // Specifies protocol and transport
        // _protocolname._transportname
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);

        // Get the NsdManager from the system and register our service
        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);

        initializeRegistrationListener();
        // This is an async process that calls
        // mRegistrationListener's onServiceRegistered function when it succeeds
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void initializeServerSocket() throws IOException {
        // Get the next available port by asking for port 0
        mServerSocket = new ServerSocket(0);

        // Get the actual port we've been assigned
        mLocalPort = mServerSocket.getLocalPort();
    }

    // Makes an Nsd registration listener that is called when the service is registered
    public void initializeRegistrationListener() {
        // RegistrationListener is an abstract class whose methods we implement here
        mRegistrationListener = new NsdManager.RegistrationListener() {


            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "onRegistrationFailed: Got error code " + errorCode);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "onUnregistrationFailed: Got error code " + errorCode);
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                String service = serviceInfo.toString();
                mServiceName = serviceInfo.getServiceName();
                Log.i(TAG, "Registered service " + mServiceName + "\n" + service);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                // When NsdManager.unRegisterService() is called, it calls this?
                String service = serviceInfo.toString();
                Log.i(TAG, "Unregistered service " + service);
            }
        };
    }

    /**
     * Service Discovery
     */

    public void discoverServices() {
        // This is also an async process
        initializeDiscoveryListener();
        initializeResolveListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }


    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.i(TAG, "Service Discovery Started");
            }

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

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "service lost: " + serviceInfo);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.e(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed. Error code " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed. Error code " + errorCode);
            }
        };
    }

    // Listener called after service is discovered
    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded: " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Somehow got ourselves again. Same IP.");
                    return;
                }

                // Can only attach to one service at a time!
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
    public void tearDown() {
        // Unregister the service from Nsd
        // and stop looking for the service
        mNsdManager.unregisterService(mRegistrationListener);
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        mPeers.clear();
        updatePeerList();
    }

    public HashMap<InetAddress, NsdServiceInfo> getPeers() {
        return mPeers;
    }

    private void updatePeerList() {
        Iterator<InetAddress> addressIterator = mPeers.keySet().iterator();

        addressList.clear(); // Use the same list so adapters work
        addressIterator.forEachRemaining(address -> {
            addressList.add(address.toString());
        });
    }

    public List<String> getPeerList() {
        return addressList;
    }


}
