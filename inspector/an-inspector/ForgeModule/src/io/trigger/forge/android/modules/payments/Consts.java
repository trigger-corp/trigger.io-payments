package io.trigger.forge.android.modules.payments;

/**
 * This class holds global constants that are used throughout the application to support in-app billing.
 */
public class Consts {
	public static final int GOOGLE_API_VERSION = 3;
	
	// The response codes for a request, defined by Android Market.
	public enum ResponseCode { 
		RESULT_OK(0),                   // Success
		RESULT_USER_CANCELED(1),        // User pressed back or canceled a dialog
		RESULT_SERVICE_UNAVAILABLE(2), 
		RESULT_BILLING_UNAVAILABLE(3),  // Billing API version is not supported for the type requested
		RESULT_ITEM_UNAVAILABLE(4),     // Requested product is not available for purchase
		RESULT_DEVELOPER_ERROR(5),      // Invalid arguments provided to the API. This error can also indicate that the application was not correctly signed or properly set up for In-app Billing in Google Play, or does not have the necessary permissions in its manifest
		RESULT_ERROR(6),                // Fatal error during the API action
		RESULT_ITEM_ALREADY_OWNED(7),   // Failure to purchase since item is already owned
		RESULT_ITEM_NOT_OWNED(8); 	    // Failure to consume since item is not owned
		
		// Converts from an ordinal value to the ResponseCode
		public static ResponseCode valueOf(int index) {
			ResponseCode[] values = ResponseCode.values();
			if (index < 0 || index >= values.length) {
				return RESULT_ERROR;
			}
			return values[index];
		}
		
		private final int value;
	    private ResponseCode(int value) {
	        this.value = value;
	    }
	    public int value() {
	        return value;
	    }
	}

	// The possible states of an in-app purchase, as defined by Android Market.
	public enum PurchaseState {
		// Responses to requestPurchase or restoreTransactions.
		PURCHASED, // User was charged for the order.
		CANCELED,  // The charge failed on the server.
		REFUNDED,  // User received a refund for the order.
		EXPIRED;   // Subscription expired

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
