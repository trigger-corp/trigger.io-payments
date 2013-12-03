package io.trigger.forge.android.modules.payments;

import io.trigger.forge.android.core.ForgeApp;
import io.trigger.forge.android.core.ForgeTask;
import android.content.Intent;
import android.os.Bundle;

public class API {
	private static void sendMethodToService(final ForgeTask task, final String method) {
		PaymentsService.activity = ForgeApp.getActivity();
		Intent intent = new Intent("io.trigger.forge.android.modules.payments.fromActivity");
		Bundle bundle = new Bundle();
		bundle.putString("method", method);
		bundle.putString("params", task.params.toString());
		bundle.putString("callid", task.callid);
		intent.putExtra("io.trigger.forge.android.modules.payments.taskDetails", bundle);
		intent.setClass(ForgeApp.getActivity(), PaymentsService.class);
		ForgeApp.getActivity().startService(intent);
	}

	public static void purchaseProduct(final ForgeTask task) {
		sendMethodToService(task, "payments.purchaseProduct");
	}

	public static void restoreTransactions(final ForgeTask task) {
		sendMethodToService(task, "payments.restoreTransactions");
	}

	public static void confirmNotification(final ForgeTask task) {
		sendMethodToService(task, "payments.confirmNotification");
	}
}
