//
//  payments_API.m
//  Forge
//
//  Created by Connor Dunn on 08/05/2012.
//  Copyright (c) 2012 Trigger Corp. All rights reserved.
//

#import "payments_API.h"
#import <StoreKit/StoreKit.h>

ForgeTask *lastPaymentTask;
NSMutableDictionary *waitingTransactions;

@implementation payments_API

+ (void)purchaseProduct:(ForgeTask*)task product:(NSString*)product {
	if ([SKPaymentQueue canMakePayments]) {
		// Create payment
		SKPayment *payment = [SKPayment paymentWithProductIdentifier:product];
		
		// Store reference to task
		lastPaymentTask = task;
		
		[[SKPaymentQueue defaultQueue] addPayment:payment];
	} else {
		[task error:@"User unable to make in app purchases" type:@"EXPECTED_FAILURE" subtype:nil];
	}
}

+ (void)restoreTransactions:(ForgeTask*)task {
	if ([SKPaymentQueue canMakePayments]) {
		[[SKPaymentQueue defaultQueue] restoreCompletedTransactions];
		[task success:nil];
	} else {
		[task error:@"User unable to make in app purchases" type:@"EXPECTED_FAILURE" subtype:nil];
	}
}

+ (void)confirmNotification:(ForgeTask*)task id:(NSString*)transactionId {
	SKPaymentTransaction *transaction = [waitingTransactions objectForKey:transactionId];
	[waitingTransactions removeObjectForKey:transactionId];
	[[SKPaymentQueue defaultQueue] finishTransaction:transaction];
	[ForgeLog i:@"Confirmed in-app payment transaction"];
}

@end
