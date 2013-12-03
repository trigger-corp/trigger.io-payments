package io.trigger.forge.android.modules.payments;

import io.trigger.forge.android.core.ForgeApp;
import io.trigger.forge.android.core.ForgeLog;
import io.trigger.forge.android.core.ForgeTask;

import java.util.HashMap;
import java.util.LinkedList;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IMarketBillingService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PaymentsService extends Service implements ServiceConnection {
	// XXX: Horrible way of accessing the activity context from the service.
	public static Context activity = null;

	private static IMarketBillingService mService;
	private static boolean available = false;
	private static boolean unavailable = false;
	private static LinkedList<PaymentTask> tasks = new LinkedList<PaymentTask>();
	private static HashMap<Long, ForgeTask> waitingTasks = new HashMap<Long, ForgeTask>();

	private class PaymentTask {
		public Bundle bundle;
		public PaymentTaskCallback callback;

		public PaymentTask(Bundle bundle, PaymentTaskCallback callback) {
			this.bundle = bundle;
			this.callback = callback;
		}
	}

	private abstract class PaymentTaskCallback {
		abstract public void success(Bundle response);

		abstract public void failure(String error, String type, String subtype);

		public void failure(String error) {
			failure(error, null, null);
		}
	}

	private Bundle makeRequestBundle(String method) {
		Bundle request = new Bundle();
		request.putString("BILLING_REQUEST", method);
		request.putInt("API_VERSION", 2);
		request.putString("PACKAGE_NAME", getPackageName());
		return request;
	}

	private void processTasks() {
		while (tasks.size() > 0 && available == true) {
			PaymentTask task = tasks.remove();
			try {
				Bundle response = mService.sendBillingRequest(task.bundle);
				task.callback.success(response);
				ForgeLog.d("Sending MarketBilling request: " + task.bundle.getString("BILLING_REQUEST") + " with id: " + response.getLong("REQUEST_ID"));

			} catch (RemoteException e) {
				task.callback.failure(e.getLocalizedMessage());
			}
		}
		while (tasks.size() > 0 && available == false && unavailable == true) {
			// Market isn't available and won't become available by itself
			PaymentTask task = tasks.remove();
			task.callback.failure("MarketBillingService currently unavailable on this device", "EXPECTED_FAILURE", null);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().equals("io.trigger.forge.android.modules.payments.fromActivity")) {
			// Intent from API

			Bundle bundle = (Bundle) intent.getExtras().get("io.trigger.forge.android.modules.payments.taskDetails");
			JsonObject params = (JsonObject) (new JsonParser().parse(bundle.getString("params")));

			final ForgeTask task = new ForgeTask(bundle.getString("callid"), params, ForgeApp.getActivity().webView);

			if (bundle.getString("method").equals("payments.restoreTransactions")) {
				Bundle request = makeRequestBundle("RESTORE_TRANSACTIONS");
				request.putLong("NONCE", Security.generateNonce());
				PaymentTask paymentTask = new PaymentTask(request, new PaymentTaskCallback() {
					@Override
					public void success(Bundle response) {
						switch (Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE"))) {
						case RESULT_OK:
							waitingTasks.put(response.getLong("REQUEST_ID"), task);
							break;
						case RESULT_ERROR:
						case RESULT_DEVELOPER_ERROR:
						default:
							task.error("Error calling RESTORE_TRANSACTIONS: " + Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE")).toString(), "EXPECTED_FAILURE", null);
							ForgeLog.w("Error calling RESTORE_TRANSACTIONS: " + Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE")).toString());
							break;
						}
					}

					@Override
					public void failure(String error, String type, String subtype) {
						task.error(error, type, subtype);
					}
				});
				tasks.add(paymentTask);
			} else if (bundle.getString("method").equals("payments.confirmNotification")) {
				Bundle request = makeRequestBundle("CONFIRM_NOTIFICATIONS");
				request.putStringArray("NOTIFY_IDS", new String[] { task.params.get("id").getAsString() });
				PaymentTask paymentTask = new PaymentTask(request, new PaymentTaskCallback() {
					@Override
					public void success(Bundle response) {
						switch (Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE"))) {
						case RESULT_OK:
							ForgeLog.i("Sent transaction confirmation.");
							waitingTasks.put(response.getLong("REQUEST_ID"), null);
							break;
						case RESULT_ERROR:
						case RESULT_DEVELOPER_ERROR:
						default:
							ForgeLog.w("Error calling CONFIRM_NOTIFICATIONS: " + Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE")).toString());
							break;
						}
					}

					@Override
					public void failure(String error, String type, String subtype) {
						ForgeLog.w("Failed to confirm transaction.");
					}
				});
				tasks.add(paymentTask);
			} else if (bundle.getString("method").equals("payments.purchaseProduct")) {
				Bundle request = makeRequestBundle("REQUEST_PURCHASE");

				String product = task.params.get("product").getAsString();
				request.putString("ITEM_ID", product);
				if (task.params.has("type")) {
					request.putString("ITEM_TYPE", task.params.get("type").getAsString());
				}

				request.putString("DEVELOPER_PAYLOAD", task.callid);
				PaymentTask paymentTask = new PaymentTask(request, new PaymentTaskCallback() {
					@Override
					public void success(Bundle response) {
						switch (Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE"))) {
						case RESULT_OK:
							PendingIntent pendingIntent = response.getParcelable("PURCHASE_INTENT");
							try {
								activity.startIntentSender(pendingIntent.getIntentSender(), null, 0, 0, 0);
								waitingTasks.put(response.getLong("REQUEST_ID"), task);
							} catch (SendIntentException e) {
							}
							break;
						case RESULT_ERROR:
						case RESULT_DEVELOPER_ERROR:
						default:
							task.error("Error calling REQUEST_PURCHASE: " + Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE")).toString(), "EXPECTED_FAILURE", null);
							ForgeLog.w("Error calling REQUEST_PURCHASE: " + Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE")).toString());
							break;
						}
					}

					@Override
					public void failure(String error, String type, String subtype) {
						task.error(error, type, subtype);
					}
				});
				tasks.add(paymentTask);
			} else {
				ForgeLog.w("Unknown intent sent to forge.payments service.");
			}
		} else if (intent.getAction().equals("io.trigger.forge.android.modules.payments.fromReceiver")) {
			// Intent from BroadcastReceiver
			Bundle bundle = (Bundle) intent.getExtras().get("io.trigger.forge.android.modules.payments.intentDetails");
			ForgeLog.d("Received MarketBilling intent: " + bundle.getString("action"));
			ForgeLog.d("with keys: " + bundle.getBundle("extras").keySet().toString());
			if (bundle.getString("action").equals("com.android.vending.billing.IN_APP_NOTIFY")) {
				// Purchase statuses have changed - get details.
				Bundle request = makeRequestBundle("GET_PURCHASE_INFORMATION");
				request.putLong("NONCE", Security.generateNonce());
				request.putStringArray("NOTIFY_IDS", new String[] { bundle.getBundle("extras").getString("notification_id") });
				PaymentTask paymentTask = new PaymentTask(request, new PaymentTaskCallback() {
					@Override
					public void success(Bundle response) {
						switch (Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE"))) {
						case RESULT_OK:
							ForgeLog.i("Successfully requested purchase state details");
							waitingTasks.put(response.getLong("REQUEST_ID"), null);
							break;
						case RESULT_ERROR:
						case RESULT_DEVELOPER_ERROR:
						default:
							ForgeLog.w("Error calling GET_PURCHASE_INFORMATION: " + Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE")).toString());
							break;
						}
					}

					@Override
					public void failure(String error, String type, String subtype) {
						ForgeLog.w("Error calling GET_PURCHASE_INFORMATION: " + error);
					}
				});
				tasks.add(paymentTask);
			} else if (bundle.getString("action").equals("com.android.vending.billing.PURCHASE_STATE_CHANGED")) {
				// New purchase state data
				String inapp_signed_data = bundle.getBundle("extras").getString("inapp_signed_data");
				String inapp_signature = bundle.getBundle("extras").getString("inapp_signature");

				if (inapp_signed_data.length() < 5) {
					ForgeLog.d("Recieved empty MarketBilling data.");
					return START_NOT_STICKY;
				}

				ForgeLog.d("Recieved MarketBilling data: " + inapp_signed_data);

				boolean signed = false;
				if (Security.checkSignature(ForgeApp.configForModule("payments").get("androidPublicKey").getAsString(), inapp_signed_data, inapp_signature)) {
					ForgeLog.i("Valid signature with MarketBilling data.");
					signed = true;
				} else {
					ForgeLog.w("Invalid signature with MarketBilling data.");
				}

				JsonObject data = (JsonObject) new JsonParser().parse(inapp_signed_data);
				
				long nonce = data.get("nonce").getAsLong();
				if (!Security.goodNonce(nonce)) {
					ForgeLog.w("Invalid nonce with MarketBilling data.");
				} else {
					ForgeLog.i("Valid nonce with MarketBilling data.");

					JsonArray orders = data.getAsJsonArray("orders");

					for (int i = 0; i < orders.size(); i++) {
						JsonObject order = orders.get(i).getAsJsonObject();
						JsonObject result = new JsonObject();

						JsonObject receipt = new JsonObject();
						receipt.addProperty("type", "android");
						receipt.addProperty("data", inapp_signed_data);
						receipt.addProperty("signature", inapp_signature);
						receipt.addProperty("signed", signed);
						receipt.add("purchaseToken", order.get("purchaseToken"));
						result.add("receipt", receipt);

						// Transaction data
						result.add("orderId", order.get("orderId"));
						result.add("productId", order.get("productId"));
						if (order.has("notificationId")) {
							result.add("notificationId", order.get("notificationId"));
						}
						result.add("purchaseTime", order.get("purchaseTime"));
						result.addProperty("purchaseState", Consts.PurchaseState.valueOf(order.get("purchaseState").getAsInt()).toString());

						ForgeApp.event("payments.transactionReceived", result);
					}
				}

			} else if (bundle.getString("action").equals("com.android.vending.billing.RESPONSE_CODE")) {
				// Async response code
				if (waitingTasks.containsKey(bundle.getBundle("extras").getLong("request_id"))) {
					ForgeTask task = waitingTasks.remove(bundle.getBundle("extras").getLong("request_id"));
					// Response code to waiting task
					switch (Consts.ResponseCode.valueOf(bundle.getBundle("extras").getInt("response_code"))) {
					case RESULT_OK:
						if (task != null) {
							task.success();
						}
						ForgeLog.d("Completed MarketBilling task successfully: " + bundle.getBundle("extras").getLong("request_id"));
						break;
					case RESULT_ERROR:
					case RESULT_DEVELOPER_ERROR:
					default:
						if (task != null) {
							task.error("Async error completing MarketBilling task: " + Consts.ResponseCode.valueOf(bundle.getBundle("extras").getInt("response_code")).toString(), "EXPECTED_FAILURE", null);
						}
						ForgeLog.w("Async error completing MarketBilling task " + bundle.getBundle("extras").getLong("request_id") + " : " + Consts.ResponseCode.valueOf(bundle.getBundle("extras").getInt("response_code")).toString());
						break;
					}
				} else {
					ForgeLog.w("Response code received from MarketBilling which was not handled.");
				}
			} else {
				ForgeLog.w("Unknown intent sent to forge.payments service.");
			}
		} else {
			ForgeLog.w("Unknown intent sent to forge.payments service.");
		}
		// Process any added tasks
		processTasks();
		return START_NOT_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			boolean bindResult = bindService(new Intent("com.android.vending.billing.MarketBillingService.BIND"), this, Context.BIND_AUTO_CREATE);
			if (bindResult) {
				ForgeLog.i("Sucessfully bound to MarketBillingService.");
			} else {
				unavailable = true;
				ForgeLog.w("Could not bind to the MarketBillingService.");
			}
		} catch (SecurityException e) {
			ForgeLog.e("Security exception: " + e);
		}
	}

	@Override
	public void onDestroy() {
		try {
			unbindService(this);
		} catch (IllegalArgumentException e) {
			// This might happen if the service was disconnected
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void onServiceConnected(ComponentName name, IBinder service) {
		ForgeLog.i("MarketBillingService connected.");
		mService = IMarketBillingService.Stub.asInterface(service);

		Bundle request = makeRequestBundle("CHECK_BILLING_SUPPORTED");
		try {
			Bundle response = mService.sendBillingRequest(request);
			switch (Consts.ResponseCode.valueOf(response.getInt("RESPONSE_CODE"))) {
			case RESULT_OK:
				available = true;
				ForgeLog.i("MarketBillingService is available.");
				processTasks();
				break;
			default:
				available = false;
				// Mark as actively unavailable, not just unready
				unavailable = true;
				ForgeLog.w("MarketBillingService is unavailable.");
			}
		} catch (RemoteException e) {

		}
	}

	public void onServiceDisconnected(ComponentName name) {
		ForgeLog.i("MarketBillingService disconnected.");
		mService = null;
		available = false;
		unavailable = false;
	}

}
