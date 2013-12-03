//
//  payments_API.h
//  Forge
//
//  Created by Connor Dunn on 08/05/2012.
//  Copyright (c) 2012 Trigger Corp. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface payments_API : NSObject

+ (void)purchaseProduct:(ForgeTask*)task product:(NSString*)product;
+ (void)restoreTransactions:(ForgeTask*)task;
+ (void)confirmNotification:(ForgeTask*)task id:(NSString*)transactionId;

@end
