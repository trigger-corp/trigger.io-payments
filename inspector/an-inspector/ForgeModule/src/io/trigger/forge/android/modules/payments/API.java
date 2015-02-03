package io.trigger.forge.android.modules.payments;

import io.trigger.forge.android.core.ForgeParam;
import io.trigger.forge.android.core.ForgeTask;

public class API {
	public static InAppBillingDelegate delegate;

	public static void purchaseProduct(final ForgeTask task, @ForgeParam("product") final String product) {
		API.delegate.purchaseProduct(task, product);
	}

	public static void startSubscription(final ForgeTask task, @ForgeParam("product") final String product) {
		API.delegate.startSubscription(task, product);
	}
	
	public static void consumePurchase(final ForgeTask task, @ForgeParam("product") final String product) {
		API.delegate.consumePurchase(task, product);
	}
	
	public static void restoreTransactions(final ForgeTask task) {
		API.delegate.restoreTransactions(task);
	}

	public static void confirmNotification(final ForgeTask task) {
		// No longer required in v3
		task.success();
	}
}
