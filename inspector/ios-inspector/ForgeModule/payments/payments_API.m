//
//  payments_API.m
//  Forge
//
//  Created by Connor Dunn on 08/05/2012.
//  Copyright (c) 2012 Trigger Corp. All rights reserved.
//

#import <StoreKit/StoreKit.h>
#import "payments_API.h"
#import "payments_EventListener.h"
#import "payments_transactionObserver.h"


ForgeTask *lastPaymentTask;
NSMutableDictionary *waitingTransactions;


/**
 * Useful:
 *
 *   https://developer.apple.com/app-store/subscriptions/
 *   http://troybrant.net/blog/2010/01/in-app-purchases-a-full-walkthrough/
 */

@implementation payments_API

+ (void)manageSubscriptions:(ForgeTask*)task {
    NSLog(@"Opening subscriptions management screen");
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:@"https://buy.itunes.apple.com/WebObjects/MZFinance.woa/wa/manageSubscriptions"]];
    [task success:nil];
}


+ (void)purchaseProduct:(ForgeTask*)task product:(NSString*)product {
    if ([SKPaymentQueue canMakePayments]) {
        // Store reference to task
        lastPaymentTask = task;

        // request SKProduct
        [ForgeLog i:@"Requesting product information"];
        NSSet *productIdentifiers = [NSSet setWithObject:product];
        [observer requestProductData:productIdentifiers];

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
    if ([waitingTransactions objectForKey:transactionId] != nil) {
        [waitingTransactions removeObjectForKey:transactionId];
        [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
        [ForgeLog i:@"Confirmed in-app payment transaction"];
        [task success:nil];
    } else {
        [ForgeLog e:@"Transaction has already been confirmed or does not exist"];
        [task error:@"Transaction has already been confirmed or does not exist" type:@"UNEXPECTED_FAILURE" subtype:nil];
    }
}

@end
