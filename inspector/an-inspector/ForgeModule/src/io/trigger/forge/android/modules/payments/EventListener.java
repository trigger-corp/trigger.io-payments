package io.trigger.forge.android.modules.payments;

import io.trigger.forge.android.core.ForgeApp;
import io.trigger.forge.android.core.ForgeEventListener;
import android.content.Intent;
import android.os.Bundle;

public class EventListener extends ForgeEventListener { //implements ServiceConnection {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String androidPublicKey = ForgeApp.configForModule("payments").get("androidPublicKey").getAsString();
		API.delegate = new InAppBillingDelegate(ForgeApp.getActivity(), androidPublicKey);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!API.delegate.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	@Override
	public void onDestroy() {
		API.delegate.release();
	}
	
}
