package io.trigger.forge.android.modules.payments;

import io.trigger.forge.android.core.ForgeApp;
import io.trigger.forge.android.core.ForgeLog;
import io.trigger.forge.android.core.ForgeTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.vending.billing.IInAppBillingService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InAppBillingDelegate {
	private String androidPublicKey;
	private WeakReference<Context> contextRef;	
	private HashMap<String, ForgeTask> pendingTasks = new HashMap<String, ForgeTask>();
	
	/** - Life cycle -------------------------------------------------------- */
	public InAppBillingDelegate(Context context, String androidPublicKey) {
		this.contextRef = new WeakReference<Context>(context);
		this.androidPublicKey = androidPublicKey;
		if (InAppBillingDelegate.billingService == null) {
			try {
				Intent intent = new  Intent("com.android.vending.billing.InAppBillingService.BIND");
				intent.setPackage("com.android.vending");
				this.getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
			} catch (Exception e) {
				ForgeLog.e("Failed to initialize billing service: " + e);
			}
		}
	}
	
	public void release() {
		if (InAppBillingDelegate.serviceConnection != null && this.getContext() != null) {
			try {
				this.getContext().unbindService(InAppBillingDelegate.serviceConnection);
			} catch (Exception e) {
				ForgeLog.e("Failed to shutdown billing service: " + e);
			}
			InAppBillingDelegate.serviceConnection = null;
		}
		if (this.contextRef != null) {
			this.contextRef.clear();
		}
	}
	
	private Context getContext() {
		return contextRef.get();
	}
	
	
	/** - Public interface -------------------------------------------------- */
	public void purchaseProduct(final ForgeTask task, final String product) {
		serviceRequest(task, product, "inapp");
	}

	public void startSubscription(final ForgeTask task, final String product) {
		serviceRequest(task, product, "subs");
	}
	
	public void restoreTransactions(final ForgeTask task) {
		HashMap<String, JsonObject> restoredTransactions = fetchTransactions();
		for (JsonObject transaction : restoredTransactions.values()) {
			ForgeApp.event("payments.transactionReceived", transaction);
		}
		task.success();
	}

	public void consumePurchase(final ForgeTask task, String product) {
		HashMap<String, JsonObject> restoredTransactions = fetchTransactions();
		JsonObject transaction = restoredTransactions.get(product);
		if (transaction == null) {
			task.error("Product is not owned");
			return;
		}
		try {
			String purchaseToken = transaction.getAsJsonObject("receipt").get("purchaseToken").getAsString();
			ForgeLog.i("Consuming Purchase token: " + purchaseToken);
			int response = billingService.consumePurchase(Consts.GOOGLE_API_VERSION, getContext().getApplicationContext().getPackageName(), purchaseToken);
			Consts.ResponseCode responseCode = Consts.ResponseCode.valueOf(response);
			if (responseCode == Consts.ResponseCode.RESULT_OK) {
				ForgeLog.d("Successfully consumed " + product);
			} else {
				ForgeLog.e(String.format("Failed to consume %s: error %s", product, responseCode));
				task.error(String.format("Failed to consume %s: error %s", product, responseCode));
				return;
			}
		} catch (Exception e) {
			ForgeLog.e(String.format("Failed to consume %s: %s", product, e));
			task.error(String.format("Failed to consume %s: %s", product, e));
			return;
		}
		task.success();
	}
	
	
	/** - Helpers ----------------------------------------------------------- */
	
	/**
	 * serviceRequest
	 * @param task
	 * @param product
	 * @param type
	 */
	private void serviceRequest(final ForgeTask task, final String product, final String type) {
		if (billingService == null) {
			task.error("Billing service is not initialized");
			return;
		} else if (TextUtils.isEmpty(product) || TextUtils.isEmpty(type)) {
			task.error("Invalid parameters");
			return;
		}

		String payload = type + ":" + task.callid;
		String packageName = getContext().getApplicationContext().getPackageName();
		Bundle bundle;		
		try {
			bundle = billingService.getBuyIntent(Consts.GOOGLE_API_VERSION, packageName, product, type, payload);
			if (bundle == null) {
				task.error("Failed to create transaction bundle", "UNEXPECTED_FAILURE", null);
				return;
			}
		} catch (RemoteException e) {
			task.error("Error creating transaction bundle: " + e, "UNEXPECTED_FAILURE", null);
			return;
		}
		
		Consts.ResponseCode responseCode = Consts.ResponseCode.valueOf(bundle.getInt("RESPONSE_CODE"));
		switch (responseCode) {
		case RESULT_OK:
			PendingIntent pendingIntent = bundle.getParcelable("BUY_INTENT");			
			try {
				ForgeApp.getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(), PURCHASE_FLOW_REQUEST_CODE, new Intent(), 0, 0, 0);
				pendingTasks.put(payload, task);
			} catch (SendIntentException e) {
				e.printStackTrace();
				task.error("Error calling PURCHASE_FLOW_REQUEST: " + e, "UNEXPECTED_FAILURE", null);
				return;
			}
			break;
		case RESULT_ITEM_ALREADY_OWNED:
			ForgeLog.w("Already own: " + type + "." + product);
			task.error("Item already owned", "EXPECTED_FAILURE", null);
			break;
		default:
			task.error("Error creating purchase intent: " + Consts.ResponseCode.valueOf(bundle.getInt("RESPONSE_CODE")).toString(), "EXPECTED_FAILURE", null);
			ForgeLog.w("Error creating purchase intent: " + Consts.ResponseCode.valueOf(bundle.getInt("RESPONSE_CODE")).toString());
			return;
		}
	}
	
	/**
	 * handleActivityResult
	 * @param requestCode
	 * @param resultCode
	 * @param intent
	 * @return
	 */
	public boolean handleActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode != PURCHASE_FLOW_REQUEST_CODE) {
			return false;
		}
		
		Consts.ResponseCode responseCode = Consts.ResponseCode.valueOf(intent.getIntExtra("RESPONSE_CODE", 
				Consts.ResponseCode.RESULT_ERROR.value()));		
		if (responseCode != Consts.ResponseCode.RESULT_OK) {
			// no way to get a handle to the original tasks :-/
			for (ForgeTask task : pendingTasks.values()) {
				ForgeLog.w("Async error completing MarketBilling task: " + responseCode.toString());
				task.error("Async error completing MarketBilling task: " + responseCode.toString(), "EXPECTED_FAILURE", null);
			}
			pendingTasks.clear();
			return true;
		}
		
		// parse intent
		String signature = intent.getStringExtra("INAPP_DATA_SIGNATURE");
		String json = intent.getStringExtra("INAPP_PURCHASE_DATA");
		if (json == null) {
			// probably a refund or canceled task, no way to know with v3 API :-/
			// no way to get a handle to the original tasks with v3 either :-/
			for (ForgeTask task : pendingTasks.values()) {
				ForgeLog.w("Processed transaction with no body, probably a refund or cancelation: " + resultCode);
				task.success();
			}
			pendingTasks.clear();
			return true;
		}
		JsonObject transaction = parseTransaction(signature, json);
		String payload = transaction.get("developerPayload").getAsString();
		if (!pendingTasks.containsKey(payload)) {
			ForgeLog.w("No pending tasks found for: " + payload);
		} else {
			// was approved by the user
			ForgeTask task = pendingTasks.remove(payload);
			task.success(); 
		}
		
		ForgeApp.event("payments.transactionReceived", transaction);
		
		return true;
	}
	
	private JsonObject parseTransaction(final String signature, final String json) {		
		JsonObject order = (JsonObject) new JsonParser().parse(json);

		// receipt
		JsonObject receipt = new JsonObject();
        receipt.addProperty("type", "android");
        receipt.addProperty("data", json);
        receipt.addProperty("signature", signature); 
		boolean signed = false;
		if (Security.verifyPurchase(order.get("productId").getAsString(), this.androidPublicKey, json, signature)) {
			ForgeLog.i("Valid signature with InAppBilling data.");
			signed = true;
		} else {
			ForgeLog.w("Invalid signature with InAppBilling data.");
		}                                                                                      
        receipt.addProperty("signed", signed);
        receipt.add("purchaseToken", order.get("purchaseToken"));
        
        // result
		JsonObject transaction = new JsonObject();        
        transaction.add("receipt", receipt);   
		transaction.add("orderId", order.get("orderId"));
		transaction.add("productId", order.get("productId"));
		if (order.has("notificationId")) {
			transaction.add("notificationId", order.get("notificationId"));
		}
		transaction.add("purchaseTime", order.get("purchaseTime"));
		transaction.addProperty("purchaseState", Consts.PurchaseState.valueOf(order.get("purchaseState").getAsInt()).toString());
		transaction.add("developerPayload", order.get("developerPayload"));		
		
		return transaction;
	}
	
	private HashMap<String, JsonObject> fetchTransactions() {
		HashMap<String, JsonObject> transactions = new HashMap<String, JsonObject>();
		for (String type : Arrays.asList("inapp", "subs")) {
			try {
				Bundle bundle = billingService.getPurchases(Consts.GOOGLE_API_VERSION, getContext().getApplicationContext().getPackageName(), type, null);
				Consts.ResponseCode responseCode = Consts.ResponseCode.valueOf(bundle.getInt("RESPONSE_CODE"));				
				if (responseCode == Consts.ResponseCode.RESULT_OK) {
					ArrayList<String> purchaseList = bundle.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
					ArrayList<String> signatureList = bundle.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
					for (int i = 0; i < purchaseList.size(); i++) {
						String json = purchaseList.get(i);
						String signature = signatureList != null && signatureList.size() > i ? signatureList.get(i) : null;					
						JsonObject transaction = parseTransaction(signature, json);
						transactions.put(transaction.get("productId").getAsString(), transaction);
					}
				}
			} catch (Exception e) {
				ForgeLog.w("Could not fetch " + type + " transactions: " + e.toString());
			}
		}		
		return transactions;
	}


	/** - Google In App Billing v3 ------------------------------------------ */
	private static final int PURCHASE_FLOW_REQUEST_CODE = 2061984;
	private static IInAppBillingService billingService;
	private static ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			InAppBillingDelegate.billingService = null;
		}
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			InAppBillingDelegate.billingService = IInAppBillingService.Stub.asInterface(service);
			ForgeLog.i("In App Billing Service Initialized");
		}
	};
}
