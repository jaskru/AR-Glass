/*
 * PauseCommand
 * 
 * Created on: May 5, 2011
 * Author: Dmytro Baryskyy
 */

package com.ne0fhyklabs.freeflight.service.commands;

import com.ne0fhyklabs.freeflight.drone.DroneProxy;
import com.ne0fhyklabs.freeflight.service.DroneControlService;

public class PauseCommand extends DroneServiceCommand
{

    private DroneProxy droneProxy;


    public PauseCommand(DroneControlService context)
    {
        super(context);
        droneProxy = DroneProxy.getInstance(context.getApplicationContext());
    }


    @Override
    public void execute()
    {
        droneProxy.doPause();

        context.onCommandFinished(this);
    }

}
