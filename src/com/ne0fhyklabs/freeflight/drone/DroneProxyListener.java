/*
 * DroneProxyListener
 *
 *  Created on: May 5, 2011
 *      Author: Dmytro Baryskyy
 */

package com.ne0fhyklabs.freeflight.drone;

public interface DroneProxyListener 
{
	public void onToolConnected();
	public void onToolConnectionFailed(int reason);
	public void onToolDisconnected();
	public void onConfigChanged();
}
