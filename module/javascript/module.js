/* global forge */

forge['payments'] = {
	'manageSubscriptions': function (success, error) {
		if (forge.is.ios()) {
			forge.internal.call("payments.manageSubscriptions", {}, success, error);
		} else {
			forge.log.warning("forge.payments.manageSubscriptions is only available for iOS");
			success();
		}
	},
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
						forge.internal.call("payments.confirmNotification", {
							id: data.notificationId
						}, function() {}, function() {});
					}
				};
				callback(data, confirm);
			});
		}
	}
};
