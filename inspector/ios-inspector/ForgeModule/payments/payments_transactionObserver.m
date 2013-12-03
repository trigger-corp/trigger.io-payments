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

- (void)paymentQueue:(SKPaymentQueue *)queue updatedTransactions:(NSArray *)transactions
{
	static char taskKey;
    for (SKPaymentTransaction *transaction in transactions)
    {
		ForgeTask *task = nil;
        switch (transaction.transactionState) {
			case SKPaymentTransactionStatePurchasing:
				objc_setAssociatedObject(transaction, &taskKey, lastPaymentTask, OBJC_ASSOCIATION_RETAIN);
				lastPaymentTask = nil;
				[ForgeLog i:@"Starting in-app payment."];
				break;
            case SKPaymentTransactionStatePurchased: {
				// If the task is still around let send the success callback
				task = (ForgeTask *)objc_getAssociatedObject(transaction, &taskKey);
				[task success:nil];
				
				// Send an event for the transaction
				NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
				[result setValue:transaction.transactionIdentifier forKey:@"orderId"];
				[result setValue:transaction.payment.productIdentifier forKey:@"productId"];
				[result setValue:[NSString stringWithFormat:@"%.0f",[transaction.transactionDate timeIntervalSince1970]] forKey:@"purchaseTime"];
				[result setValue:@"PURCHASED" forKey:@"PurchaseState"];
				[result setValue:@"PURCHASED" forKey:@"purchaseState"];
				NSMutableDictionary *reciept = [[NSMutableDictionary alloc] initWithCapacity:2];
				[reciept setValue:[transaction.transactionReceipt base64EncodingWithLineLength:0] forKey:@"data"];
				[reciept setValue:@"iOS" forKey:@"type"];
				[result setValue:reciept forKey:@"receipt"];

				// Create UUID to reference transaction later
				CFUUIDRef uuid = CFUUIDCreate(NULL);
				NSString *uuidString = (__bridge_transfer NSString *) CFUUIDCreateString(NULL, uuid);
				if (waitingTransactions == nil) {
					waitingTransactions = [[NSMutableDictionary alloc] init];
				}
				[waitingTransactions setValue:transaction forKey:uuidString];
				[result setValue:uuidString forKey:@"notificationId"];
							
				[[ForgeApp sharedApp] event:@"payments.transactionReceived" withParam:result];
				[ForgeLog i:@"Received in-app payment transaction information."];
                break;
			}
            case SKPaymentTransactionStateFailed:
				task = (ForgeTask *)objc_getAssociatedObject(transaction, &taskKey);
				[task error:[NSString stringWithFormat:@"In-app payment failed: %@",[transaction.error localizedDescription]] type:@"EXPECTED_FAILURE" subtype:nil];
				[ForgeLog w:[NSString stringWithFormat:@"In-app payment failed: %@",[transaction.error localizedDescription]]];
				[[SKPaymentQueue defaultQueue] finishTransaction:transaction];
                break;
            case SKPaymentTransactionStateRestored: {
				// Send an event for the transaction
				NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
				[result setValue:transaction.originalTransaction.transactionIdentifier forKey:@"orderId"];
				[result setValue:transaction.originalTransaction.payment.productIdentifier forKey:@"productId"];
				[result setValue:[NSString stringWithFormat:@"%.0f",[transaction.originalTransaction.transactionDate timeIntervalSince1970]] forKey:@"purchaseTime"];
				[result setValue:@"PURCHASED" forKey:@"PurchaseState"];
				[result setValue:@"PURCHASED" forKey:@"purchaseState"];
				NSMutableDictionary *reciept = [[NSMutableDictionary alloc] initWithCapacity:2];
				[reciept setValue:[transaction.transactionReceipt base64EncodingWithLineLength:0] forKey:@"data"];
				[reciept setValue:@"iOS" forKey:@"type"];
				[result setValue:reciept forKey:@"receipt"];
				
				// Create UUID to reference transaction later
				CFUUIDRef uuid = CFUUIDCreate(NULL);
				NSString *uuidString = (__bridge_transfer NSString *) CFUUIDCreateString(NULL, uuid);
				if (waitingTransactions == nil) {
					waitingTransactions = [[NSMutableDictionary alloc] init];
				}
				[waitingTransactions setValue:transaction forKey:uuidString];
				[result setValue:uuidString forKey:@"notificationId"];
				
				[[ForgeApp sharedApp] event:@"payments.transactionReceived" withParam:result];

                NSLog(@"Restored in-app transaction.");
                break;
			}
            default:
                [ForgeLog w:@"Unknown in-app payment transaction type received."];
                break;
        }
    }
}

@end
