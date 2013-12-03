//
//  payments_transactionObserver.h
//  Forge
//
//  Created by Connor Dunn on 11/05/2012.
//  Copyright (c) 2012 Trigger Corp. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <StoreKit/StoreKit.h>

@interface payments_transactionObserver : NSObject <SKPaymentTransactionObserver> {
	payments_transactionObserver *me;
}

- (payments_transactionObserver *) init;

@end
