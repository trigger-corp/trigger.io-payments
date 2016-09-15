//
//  payments_transactionObserver.h
//  Forge
//
//  Created by Connor Dunn on 11/05/2012.
//  Copyright (c) 2012 Trigger Corp. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <StoreKit/StoreKit.h>



@interface payments_transactionObserver : NSObject <SKProductsRequestDelegate, SKPaymentTransactionObserver> {
	payments_transactionObserver *me;
    SKProductsRequest *productsRequest;
}

- (payments_transactionObserver *) init;
- (void) requestProductData:(NSSet*)productIdentifiers;

@end
