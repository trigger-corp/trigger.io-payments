forge['payments'] = {
	'purchaseProduct': function (product, success, error) {
		forge.internal.call("payments.purchaseProduct", {product: product}, success, error);
	},
	'startSubscription': function (product, success, error) {
		if (forge.is.ios()) {
			forge.internal.call("payments.purchaseProduct", {product: product, type: 'subs'}, success, error);
		} else {
			forge.internal.call("payments.startSubscription", {product: product}, success, error);
		}
	},
	'restoreTransactions': function (success, error) {
		forge.internal.call("payments.restoreTransactions", {}, success, error);
	},
	'consumePurchase': function (product, success, error) {
		if (forge.is.android()) {
			forge.internal.call("payments.consumePurchase", {product: product}, success, error);
		} else {
			forge.logging.warning("forge.payments.consumePurchase is only available for Android");
			success();
		}
	},	
	'transactionReceived': {
		addListener: function (callback, error) {
			forge.internal.addEventListener('payments.transactionReceived', function (data) {
				var confirm = function () {
					if (data.notificationId) {
						forge.internal.call("payments.confirmNotification", {id: data.notificationId});
					}
				};
				callback(data, confirm);
			});
		}
	}
};

setTimeout(function () {
	// TODO: forge.internal.listeners probably isn't exposed right now
	if (!forge.internal.listeners['payments.transactionReceived']) {
		forge.logging.warning("Payments module enabled but no 'forge.payments.transactionReceived' listener, see the 'payments' module documentation for more details.");
	}
}, 5000);
