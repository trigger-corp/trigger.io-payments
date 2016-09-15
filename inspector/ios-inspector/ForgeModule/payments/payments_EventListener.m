//
//  payments_EventListener.m
//  Forge
//
//  Created by Connor Dunn on 11/05/2012.
//  Copyright (c) 2012 Trigger Corp. All rights reserved.
//

#import <StoreKit/StoreKit.h>

#import "payments_EventListener.h"

payments_transactionObserver *observer = nil;

@implementation payments_EventListener

+ (void)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
	observer = [[payments_transactionObserver alloc] init];
	[[SKPaymentQueue defaultQueue] addTransactionObserver:observer];
}

@end
