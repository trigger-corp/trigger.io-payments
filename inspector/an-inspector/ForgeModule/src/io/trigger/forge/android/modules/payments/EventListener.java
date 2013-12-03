package io.trigger.forge.android.modules.payments;

import io.trigger.forge.android.core.ForgeApp;
import io.trigger.forge.android.core.ForgeEventListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

public class EventListener extends ForgeEventListener implements ServiceConnection {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent service = new Intent();
		service.setClass(ForgeApp.getActivity(), PaymentsService.class);
		ForgeApp.getActivity().bindService(service, this, Context.BIND_AUTO_CREATE);
	}

	public void onServiceConnected(ComponentName name, IBinder service) {
	}

	public void onServiceDisconnected(ComponentName name) {
	}

}
