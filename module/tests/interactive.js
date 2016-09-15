/* global module, forge, asyncTest, askQuestion, ok, start, $ */

module("forge.payments");

/* - logging ------------------------------------------------------------ */
var pre = document.createElement("pre");
pre.setAttribute("id", "console");
var page_header = document.getElementsByClassName("page-header")[0];
page_header.insertBefore(pre, page_header.firstChild);
function log(msg) {
	$("#console").append(msg + "\n");
}
function error(where) {
	return function (e) {
		forge.logging.error("[ERROR] " + where + ": " + JSON.stringify(e));
		log("[ERROR] " + where + ": " + JSON.stringify(e));
	};
}


/* - tests -------------------------------------------------------------- */

var products = {
	non_consumable:    forge.is.ios() ? "test"     : "TODO",
	consumable:        forge.is.ios() ? "test2"    : "android.test.purchased",
	auto_subscription: forge.is.ios() ? "monthly1" : "TODO",
	subscription:      forge.is.ios() ? "monthly3" : "TODO"
};

var current_productId = null;
var restore_count = 0;

log("Adding transactionReceived listener");
forge.payments.transactionReceived.addListener(function (data, confirm) {
	var copy = JSON.parse(JSON.stringify(data));
	copy.receipt.data = copy.receipt.data.substring(0, 3);
	log("Transaction Received: " + JSON.stringify(copy));
	confirm();
	if (data.purchaseState == "PURCHASED") {
		log("Thanks for buying: " + data.productId);
	} else {
		log("Your product '" + data.productId + "' has been removed");
	}
	if (current_productId === "restore") {
		if (restore_count === 0) {
			ok(true, "Transactions restored");
		}
		restore_count++;
	} else if (current_productId === data.productId) {
		ok(true, "Received transaction");
	} else {
		ok(false, "Expected: " + current_productId + " Got: " + data.productId);
	}
});


asyncTest("Are you ready?", 1, function() {
	askQuestion("Are you ready?", {
		Yes: function () {
			ok(true, "Success");
			start();
		}
	});
});


asyncTest("Purchase consumable item", 2, function() {
	$("#console").html("");
	current_productId = products.consumable;
	forge.payments.purchaseProduct(products.consumable, function () {
		askQuestion("Were you able to purchase the consumable item?", {
			Yes: function () {
				ok(true, "Success");
				start();
			},
			No: function () {
				ok(false, "User claims failure with forge.payments.purchaseProduct");
				start();
			}
		});
	}, function (e) {
		error("forge.payments.purchaseProduct")(e);
		ok(false, "API call failure: " + e.message);
		start();
	});
});


if (forge.is.android()) {
	asyncTest("Consume purchase", 2, function() {
		$("#console").html("");
		current_productId = products.consumable;
		forge.payments.consumePurchase(products.consumable, function () {
			askQuestion("Were you able to consume the purchase?", {
				Yes: function () {
					ok(true, "Success");
					start();
				},
				No: function () {
					ok(false, "User claims failure with forge.payments.consumePurchase");
					start();
				}
			});
		}, function (e) {
			error("forge.payments.consumePurchase")(e);
			ok(false, "API call failure: " + e.message);
			start();
		});
	});
}


asyncTest("Purchase non-consumable item", 2, function() {
	$("#console").html("");
	current_productId = products.non_consumable;
	forge.payments.purchaseProduct(products.non_consumable, function () {
		askQuestion("Were you able to purchase or restore the non-consumable item?", {
			Yes: function () {
				ok(true, "Success");
				start();
			},
			No: function () {
				ok(false, "User claims failure with forge.payments.purchaseProduct");
				start();
			}
		});
	}, function (e) {
		error("forge.payments.purchaseProduct")(e);
		ok(false, "API call failure: " + e.message);
		start();
	});
});


asyncTest("Restore transactions", 2, function() {
	$("#console").html("");
	current_productId = "restore";
	forge.payments.restoreTransactions(function () {
		askQuestion("Were you able to restore the transactions?", {
			Yes: function () {
				ok(true, "Success");
				start();
			},
			No: function () {
				ok(false, "User claims failure with forge.payments.restoreTransactions");
				start();
			}
		});
	}, function (e) {
		error("forge.payments.restoreTransactions")(e);
		ok(false, "API call failure: " + e.message);
		start();
	});
});


asyncTest("Start auto-renewable subscription", 2, function() {
	$("#console").html("");
	current_productId = products.auto_subscription;
	forge.payments.startSubscription(products.auto_subscription, function () {
		askQuestion("Were you able to subscribe to the auto-renewable subscription?", {
			Yes: function () {
				ok(true, "Success");
				start();
			},
			No: function () {
				ok(false, "User claims failure with forge.payments.startSubscription");
				start();
			}
		});
	}, function (e) {
		error("forge.payments.startSubscription")(e);
		ok(false, "API call failure: " + e.message);
		start();
	});
});


asyncTest("Start non-renewable subscription", 2, function() {
	$("#console").html("");
	current_productId = products.subscription;
	forge.payments.startSubscription(products.subscription, function () {
		askQuestion("Were you able to subscribe to the non-renewable subscription?", {
			Yes: function () {
				ok(true, "Success");
				start();
			},
			No: function () {
				ok(false, "User claims failure with forge.payments.startSubscription");
				start();
			}
		});
	}, function (e) {
		error("forge.payments.startSubscription")(e);
		ok(false, "API call failure: " + e.message);
		start();
	});
});


// Does not work with iTunes test id's :-(
if (forge.is.ios()) {
	$("#console").html("");
	asyncTest("Open subscriptions management screen", 1, function() {
		forge.payments.manageSubscriptions(function () {
			askQuestion("Did the subscriptions management screen open?", {
				Yes: function () {
					ok(true, "Success");
					start();
				},
				No: function () {
					ok(false, "User claims failure with forge.payments.manageSubscriptions");
					start();
				}
			});
		}, function (e) {
			ok(false, "API call failure: " + e.message);
		});
	});
}
