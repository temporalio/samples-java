/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.nexusEarlyReturn.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;
import io.temporal.samples.earlyreturn.TransactionRequest;
import io.temporal.samples.earlyreturn.TxResult;

/**
 * TransactionService is a Nexus service that allows starting a transfer between accounts and
 * retrieving the result of that transfer asynchronously. Each transaction is identified by a
 * transaction token, which is an opaque string that can be used to query the status of the
 * transaction.
 */
@Service
public interface TransactionService {
  class StartTransactionRequest {
    private final TransactionRequest transactionRequest;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public StartTransactionRequest(
        @JsonProperty("transactionRequest") TransactionRequest transactionRequest) {
      this.transactionRequest = transactionRequest;
    }

    @JsonProperty("transactionRequest")
    public TransactionRequest getTransactionRequest() {
      return transactionRequest;
    }
  }

  class StartTransactionResponse {
    private final String transactionToken;
    private final TxResult txResult;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public StartTransactionResponse(
        @JsonProperty("transactionToken") String transactionToken,
        @JsonProperty("txResult") TxResult txResult) {
      this.txResult = txResult;
      this.transactionToken = transactionToken;
    }

    /**
     * Returns the transaction token that can be used to retrieve the result of the transaction.
     * This is an opaque string that is unique for each transaction in the {@link
     * TransactionService}.
     *
     * @return The transaction token.
     */
    @JsonProperty("transactionToken")
    public String getTransactionToken() {
      return transactionToken;
    }

    /**
     * Returns the initial result of the transaction, which may include details such as the
     * transaction ID and status.
     *
     * @return The initial transaction result.
     */
    @JsonProperty("txResult")
    public TxResult getTxResult() {
      return txResult;
    }
  }

  class GetTransactionResultRequest {
    private final String transactionToken;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GetTransactionResultRequest(@JsonProperty("transactionToken") String transactionToken) {
      this.transactionToken = transactionToken;
    }

    @JsonProperty("transactionToken")
    public String getTransactionToken() {
      return transactionToken;
    }
  }

  /**
   * Starts a transaction to transfer funds between accounts. If the transaction is initialized
   * successfully, it returns a transaction token that can be used to wait for the transaction to
   * complete. If the transaction fails during initialization, it throws an {@link
   * io.temporal.failure.ApplicationFailure} with the transaction token as a detail, which can be
   * used to wait for the transaction to be cancelled.
   *
   * @param request The request containing the transaction details.
   * @return A response containing the transaction token and initial result.
   */
  @Operation
  StartTransactionResponse startTransaction(StartTransactionRequest request);

  /**
   * Wait for a transaction to complete and return the result using the transaction token.
   *
   * @param request The request containing the transaction token.
   * @return The result of the transaction.
   */
  @Operation
  TxResult getTransactionResult(GetTransactionResultRequest request);
}
