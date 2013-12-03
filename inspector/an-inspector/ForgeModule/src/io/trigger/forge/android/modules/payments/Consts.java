package io.trigger.forge.android.modules.payments;

/**
 * This class holds global constants that are used throughout the application to support in-app billing.
 */
public class Consts {
	// The response codes for a request, defined by Android Market.
	public enum ResponseCode {
		RESULT_OK, RESULT_USER_CANCELED, RESULT_SERVICE_UNAVAILABLE, RESULT_BILLING_UNAVAILABLE, RESULT_ITEM_UNAVAILABLE, RESULT_DEVELOPER_ERROR, RESULT_ERROR;

		// Converts from an ordinal value to the ResponseCode
		public static ResponseCode valueOf(int index) {
			ResponseCode[] values = ResponseCode.values();
			if (index < 0 || index >= values.length) {
				return RESULT_ERROR;
			}
			return values[index];
		}
	}

	// The possible states of an in-app purchase, as defined by Android Market.
	public enum PurchaseState {
		// Responses to requestPurchase or restoreTransactions.
		PURCHASED, // User was charged for the order.
		CANCELED, // The charge failed on the server.
		REFUNDED, // User received a refund for the order.
		EXPIRED; // Subscription expired

		// Converts from an ordinal value to the PurchaseState
		public static PurchaseState valueOf(int index) {
			PurchaseState[] values = PurchaseState.values();
			if (index < 0 || index >= values.length) {
				return CANCELED;
			}
			return values[index];
		}
	}
}
