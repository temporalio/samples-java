# SpringBoot Synchronous Update Sample

1. Start SpringBoot from main samples repo directory:

       ./gradlew :springboot:bootRun

2. In your browser navigate to:

       http://localhost:3030/update

Pick one of the fishing items you want to purchase from the inventory drop down list.
Next pick the amount of this item you want to purchase. 
The inventory is presented in the table below the form.
For each item you can see the current availble stock count.
Try first picking an item and then an amount that is less or equal to the items in 
inventory. You will see that the purchase goes through and the inventory table is updated
dynamically.

Now try to pick and item and amount that is greater than what's in our inventory.
You will see that the update fails and you see the "Unable to perform purchase" 
message that shows the underlying "ProductNotAvailableForAmountException" exception
raised in the update handler. 

Updating our inventory is done via local activities. The check if item and amount 
of the fishing item you want to purchase is in inventory is also done by local 
activity.

## Note
Make sure that you enable the synchronous update feature on your Temporal cluster.
This can be done in dynamic config with

        frontend.enableUpdateWorkflowExecution:
           - value: true

If you don't have this enabled you will see error shown when you try to make any purchase.