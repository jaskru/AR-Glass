/*
 * DisconnectCommand
 * 
 * Created on: May 5, 2011
 * Author: Dmytro Baryskyy
 */

package com.ne0fhyklabs.freeflight.service.commands;

import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import com.ne0fhyklabs.freeflight.drone.DroneProxy;
import com.ne0fhyklabs.freeflight.drone.DroneProxyConnectionFailedReceiver;
import com.ne0fhyklabs.freeflight.drone.DroneProxyConnectionFailedReceiverDelegate;
import com.ne0fhyklabs.freeflight.drone.DroneProxyDisconnectedReceiver;
import com.ne0fhyklabs.freeflight.drone.DroneProxyDisconnectedReceiverDelegate;
import com.ne0fhyklabs.freeflight.service.DroneControlService;

public class DisconnectCommand
        extends DroneServiceCommand
        implements DroneProxyDisconnectedReceiverDelegate,
        DroneProxyConnectionFailedReceiverDelegate
{
    private DroneProxy droneProxy;

    private LocalBroadcastManager bm;

    private DroneProxyDisconnectedReceiver disconnectedReceiver;
    private DroneProxyConnectionFailedReceiver connFailedReceiver;


    public DisconnectCommand(DroneControlService context)
    {
        super(context);
        droneProxy = DroneProxy.getInstance(context.getApplicationContext());

        bm = LocalBroadcastManager.getInstance(context.getApplicationContext());

        disconnectedReceiver = new DroneProxyDisconnectedReceiver(this);
        connFailedReceiver = new DroneProxyConnectionFailedReceiver(this);
    }


    @Override
    public void execute()
    {
        registerListeners();
        droneProxy.doResume();
        droneProxy.doDisconnect();
    }


    public void onToolConnectionFailed(int reason)
    {
        unregisterListeners();
        // Ignore this event
    }


    public void onToolDisconnected()
    {
        unregisterListeners();
        context.onCommandFinished(this);
    }


    private void registerListeners()
    {
        bm.registerReceiver(disconnectedReceiver, new IntentFilter(DroneProxy.DRONE_PROXY_DISCONNECTED_ACTION));
        bm.registerReceiver(connFailedReceiver, new IntentFilter(DroneProxy.DRONE_PROXY_CONNECTION_FAILED_ACTION));
    }


    private void unregisterListeners()
    {
        bm.unregisterReceiver(disconnectedReceiver);
        bm.unregisterReceiver(connFailedReceiver);
    }

}
