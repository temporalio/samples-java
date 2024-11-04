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

package io.temporal.samples.keymanagementencryption.awsencryptionsdk;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.context.ActivitySerializationContext;
import io.temporal.payload.context.HasWorkflowSerializationContext;
import io.temporal.payload.context.SerializationContext;
import io.temporal.workflow.unsafe.WorkflowUnsafe;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import software.amazon.cryptography.materialproviders.IKeyring;

/**
 * KeyringCodec is a {@link PayloadCodec} that encrypts and decrypts payloads using the AWS
 * Encryption SDK. It uses the provided {@link IKeyring} to encrypt and decrypt payloads. It can
 * optionally support using a {@link SerializationContext}.
 */
class KeyringCodec implements PayloadCodec {
  // Metadata encoding key for the AWS Encryption SDK
  static final ByteString METADATA_ENCODING =
      ByteString.copyFrom("awsencriptionsdk/binary/encrypted", StandardCharsets.UTF_8);

  private final AwsCrypto crypto;
  private final IKeyring kmsKeyring;
  private final boolean useSerializationContext;
  @Nullable private final SerializationContext serializationContext;

  /**
   * Constructs a new KeyringCodec with the provided {@link IKeyring}. The codec will not use a
   * {@link SerializationContext}.
   *
   * @param kmsKeyring the keyring to use for encryption and decryption.
   */
  public KeyringCodec(IKeyring kmsKeyring) {
    this.crypto = AwsCrypto.standard();
    this.kmsKeyring = kmsKeyring;
    this.useSerializationContext = false;
    this.serializationContext = null;
  }

  /**
   * Constructs a new KeyringCodec with the provided {@link IKeyring}.
   *
   * @param crypto the AWS Crypto object to use for encryption and decryption.
   * @param kmsKeyring the keyring to use for encryption and decryption.
   * @param useSerializationContext whether to use a {@link SerializationContext} for encoding and
   *     decoding payloads.
   */
  public KeyringCodec(AwsCrypto crypto, IKeyring kmsKeyring, boolean useSerializationContext) {
    this.crypto = crypto;
    this.kmsKeyring = kmsKeyring;
    this.useSerializationContext = useSerializationContext;
    this.serializationContext = null;
  }

  private KeyringCodec(
      AwsCrypto crypto, IKeyring kmsKeyring, SerializationContext serializationContext) {
    this.crypto = crypto;
    this.kmsKeyring = kmsKeyring;
    this.useSerializationContext = true;
    this.serializationContext = serializationContext;
  }

  @NotNull
  @Override
  public List<Payload> encode(@NotNull List<Payload> payloads) {
    // Disable deadlock detection for encoding payloads because this may make a network call
    // to encrypt the data.
    return WorkflowUnsafe.deadlockDetectorOff(
        () -> payloads.stream().map(this::encodePayload).collect(Collectors.toList()));
  }

  @NotNull
  @Override
  public List<Payload> decode(@NotNull List<Payload> payloads) {
    // Disable deadlock detection for decoding payloads because this may make a network call
    // to decrypt the data.
    return WorkflowUnsafe.deadlockDetectorOff(
        () -> payloads.stream().map(this::decodePayload).collect(Collectors.toList()));
  }

  @NotNull
  @Override
  public PayloadCodec withContext(@Nonnull SerializationContext context) {
    if (!useSerializationContext) {
      return this;
    }
    return new KeyringCodec(crypto, kmsKeyring, context);
  }

  private Map<String, String> getEncryptionContext() {
    // If we are not using a serialization context, return an empty map
    // There may not be a serialization context if certain cases, such as when the codec is used
    // for encoding/decoding payloads for a Nexus operation.
    if (!useSerializationContext
        || serializationContext == null
        || !(serializationContext instanceof HasWorkflowSerializationContext)) {
      return Collections.emptyMap();
    }
    String workflowId = ((HasWorkflowSerializationContext) serializationContext).getWorkflowId();
    String activityType = null;
    if (serializationContext instanceof ActivitySerializationContext) {
      activityType = ((ActivitySerializationContext) serializationContext).getActivityType();
    }
    String signature = activityType != null ? workflowId + activityType : workflowId;
    return Collections.singletonMap("signature", signature);
  }

  private Payload encodePayload(Payload payload) {
    byte[] plaintext = payload.toByteArray();
    byte[] ciphertext =
        crypto.encryptData(kmsKeyring, plaintext, getEncryptionContext()).getResult();
    return Payload.newBuilder()
        .setData(ByteString.copyFrom(ciphertext))
        .putMetadata(EncodingKeys.METADATA_ENCODING_KEY, METADATA_ENCODING)
        .build();
  }

  private Payload decodePayload(Payload payload) {
    if (METADATA_ENCODING.equals(
        payload.getMetadataOrDefault(EncodingKeys.METADATA_ENCODING_KEY, null))) {
      byte[] ciphertext = payload.getData().toByteArray();
      byte[] plaintext =
          crypto.decryptData(kmsKeyring, ciphertext, getEncryptionContext()).getResult();
      try {
        return Payload.parseFrom(plaintext);
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }
    }
    return payload;
  }
}
