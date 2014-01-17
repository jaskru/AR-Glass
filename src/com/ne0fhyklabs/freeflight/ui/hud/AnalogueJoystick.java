/*
 * AnalogueJoystick
 *
 *  Created on: May 26, 2011
 *      Author: Dmytro Baryskyy
 */

package com.ne0fhyklabs.freeflight.ui.hud;

import android.content.Context;
import com.ne0fhyklabs.freeflight.R;

public class AnalogueJoystick 
	extends JoystickBase
{

	public AnalogueJoystick(Context context, Align align, boolean absolute) 
	{
		super(context, align, absolute);
	}
 
	@Override
	protected int getBackgroundDrawableId() 
	{
		return R.drawable.joystick_halo;
	}

	
	@Override
	protected int getTumbDrawableId() 
	{
		return R.drawable.joystick_manuel;
	}
}
