# Demonstrates Signal Batching

The sample demonstrates a situation when a single deposit should be initiated for multiple 
withdrawals. For example a seller might want to be paid once per fixed number of transactions.
The sample can be easily extended to perform a payment based on a more complex criteria like a
specific time or accumulated amount.

The sample also demonstrates _signal with start_ way of starting workflows. If workflow is already
running it just receives a signal. If it is not running then it is started first and then signal is
delivered to it. You can think about _signal with start_ as a lazy way to create workflows when
signalling them.

To run a worker that hosts the workflow code execute:




