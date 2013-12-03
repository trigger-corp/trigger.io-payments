package io.trigger.forge.android.modules.payments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class PaymentsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// Forward intent to service to be processed.
		Intent sendIntent = new Intent("io.trigger.forge.android.modules.payments.fromReceiver");
		Bundle bundle = new Bundle();
		bundle.putString("action", intent.getAction());
		bundle.putBundle("extras", intent.getExtras());
		sendIntent.putExtra("io.trigger.forge.android.modules.payments.intentDetails", bundle);
		sendIntent.setClass(context, PaymentsService.class);
		context.startService(sendIntent);
	}
}
