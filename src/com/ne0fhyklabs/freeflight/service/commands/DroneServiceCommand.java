/*
 * DroneServiceCommand
 * 
 * Created on: May 5, 2011
 * Author: Dmytro Baryskyy
 */

package com.ne0fhyklabs.freeflight.service.commands;

import com.ne0fhyklabs.freeflight.service.DroneControlService;

public abstract class DroneServiceCommand
{
    protected DroneControlService context;


    public DroneServiceCommand(DroneControlService context)
    {
        this.context = context;
    }


    public abstract void execute();

}
