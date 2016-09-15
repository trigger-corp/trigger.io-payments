//
//  payments_transactionObserver.m
//  Forge
//
//  Created by Connor Dunn on 11/05/2012.
//  Copyright (c) 2012 Trigger Corp. All rights reserved.
//

#import "payments_transactionObserver.h"
#import <objc/runtime.h>
#import <CoreFoundation/CFUUID.h>

#define kInAppPurchaseManagerProductsFetchedNotification @"kInAppPurchaseManagerProductsFetchedNotification"
#define kInAppPurchaseManagerTransactionFailedNotification @"kInAppPurchaseManagerTransactionFailedNotification"
#define kInAppPurchaseManagerTransactionSucceededNotification @"kInAppPurchaseManagerTransactionSucceededNotification"

extern ForgeTask *lastPaymentTask;
extern NSMutableDictionary *waitingTransactions;

@implementation payments_transactionObserver

- (payments_transactionObserver*) init {
	if (self = [super init]) {
		// "retain"
		me = self;
	}	
	return self;
}


- (void)requestProductData:(NSSet*)productIdentifiers {
    NSLog(@"Requesting products with identifiers: %@", productIdentifiers);
    productsRequest = [[SKProductsRequest alloc] initWithProductIdentifiers:productIdentifiers];
    productsRequest.delegate = self;
    [productsRequest start];
}


- (void)productsRequest:(SKProductsRequest *)request didReceiveResponse:(SKProductsResponse *)response {
    if ([response.products count] > 0) {
        // Create payment
        [ForgeLog i:@"Creating purchase payment"];
        SKProduct *product = [response.products firstObject];
        SKPayment *payment = [SKPayment paymentWithProduct:product];
        [[SKPaymentQueue defaultQueue] addPayment:payment];

    } else {
        // this is called if your product id is not valid, this shouldn't be called unless that happens.
        NSLog(@"Requested product is not available. The product identifier is invalid: %@", response.invalidProductIdentifiers);
        [lastPaymentTask error:@"Requested product is not available." type:@"EXPECTED_FAILURE" subtype:nil];
    }

    productsRequest.delegate = nil; // release request
}


- (void)paymentQueue:(SKPaymentQueue *)queue updatedTransactions:(NSArray *)transactions {
	static char taskKey;
    for (SKPaymentTransaction *transaction in transactions) {
		ForgeTask *task = nil;
        switch (transaction.transactionState) {
			case SKPaymentTransactionStatePurchasing:
				objc_setAssociatedObject(transaction, &taskKey, lastPaymentTask, OBJC_ASSOCIATION_RETAIN);
				lastPaymentTask = nil;
				[ForgeLog i:@"Starting in-app payment."];
				break;
            case SKPaymentTransactionStatePurchased:
                task = (ForgeTask *)objc_getAssociatedObject(transaction, &taskKey);
                [self completeTransaction:task transaction:transaction];
				break;
            case SKPaymentTransactionStateFailed:
				task = (ForgeTask *)objc_getAssociatedObject(transaction, &taskKey);
                [self failedTransaction:task transaction:transaction];
                break;
            case SKPaymentTransactionStateRestored:
                [self restoreTransaction:transaction];
				break;
            default:
                [ForgeLog w:@"Unknown in-app payment transaction type received."];
                break;
        }
    }
}


- (void)completeTransaction:(ForgeTask *)task transaction:(SKPaymentTransaction *)transaction {
    // If the task is still around let send the success callback
	[task success:nil];

    // Get transaction result and set transaction date
    NSMutableDictionary *result = [self resultForTransaction:transaction];
    [result setValue:[NSString stringWithFormat:@"%.0f", [transaction.transactionDate timeIntervalSince1970]] forKey:@"purchaseTime"];

    // Send an event for the transaction
    [[ForgeApp sharedApp] event:@"payments.transactionReceived" withParam:result];
    [ForgeLog i:@"Received in-app payment transaction information."];
}


- (void)restoreTransaction:(SKPaymentTransaction *)transaction {
    // Get transaction result and set original transaction date
    NSMutableDictionary *result = [self resultForTransaction:transaction];
    [result setValue:[NSString stringWithFormat:@"%.0f",[transaction.originalTransaction.transactionDate timeIntervalSince1970]] forKey:@"purchaseTime"];

    // Send an event for the transaction
    [[ForgeApp sharedApp] event:@"payments.transactionReceived" withParam:result];
    [ForgeLog i:@"Restored in-app transaction."];
}


- (void)failedTransaction:(ForgeTask *)task transaction:(SKPaymentTransaction *)transaction {
    [task error:[NSString stringWithFormat:@"In-app payment failed: %@",[transaction.error localizedDescription]] type:@"EXPECTED_FAILURE" subtype:nil];
    [ForgeLog w:[NSString stringWithFormat:@"In-app payment failed: %@",[transaction.error localizedDescription]]];
	[[SKPaymentQueue defaultQueue] finishTransaction:transaction];
}


- (NSMutableDictionary*)resultForTransaction:(SKPaymentTransaction *)transaction {
    // Create transaction result
    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
    [result setValue:transaction.transactionIdentifier forKey:@"orderId"];
    [result setValue:transaction.payment.productIdentifier forKey:@"productId"];
    [result setValue:@"PURCHASED" forKey:@"purchaseState"];

    // Create transaction receipt
    NSMutableDictionary *receipt = [[NSMutableDictionary alloc] initWithCapacity:2];
    NSData *receiptURL = [NSData dataWithContentsOfURL:[[NSBundle mainBundle] appStoreReceiptURL]];
    [receipt setValue:[receiptURL base64EncodingWithLineLength:0] forKey:@"data"];
    [receipt setValue:@"iOS" forKey:@"type"];
    [result setValue:receipt forKey:@"receipt"];

    // Create UUID to reference transaction later
    CFUUIDRef uuid = CFUUIDCreate(NULL);
    NSString *uuidString = (__bridge_transfer NSString *) CFUUIDCreateString(NULL, uuid);
    if (waitingTransactions == nil) {
        waitingTransactions = [[NSMutableDictionary alloc] init];
    }
    [waitingTransactions setValue:transaction forKey:uuidString];
    [result setValue:uuidString forKey:@"notificationId"];

    return result;
}

@end
