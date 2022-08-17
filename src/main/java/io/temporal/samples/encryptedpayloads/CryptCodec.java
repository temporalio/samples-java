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

package io.temporal.samples.encryptedpayloads;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.DataConverterException;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.codec.PayloadCodecException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.jetbrains.annotations.NotNull;

class CryptCodec implements PayloadCodec {
  static final ByteString METADATA_ENCODING =
      ByteString.copyFrom("binary/encrypted", StandardCharsets.UTF_8);

  private static final String CIPHER = "AES/GCM/NoPadding";

  static final String METADATA_ENCRYPTION_CIPHER_KEY = "encryption-cipher";
  static final ByteString METADATA_ENCRYPTION_CIPHER =
      ByteString.copyFrom(CIPHER, StandardCharsets.UTF_8);

  static final String METADATA_ENCRYPTION_KEY_ID_KEY = "encryption-key-id";

  private static final int GCM_NONCE_LENGTH_BYTE = 12;
  private static final int GCM_TAG_LENGTH_BIT = 128;
  private static final Charset UTF_8 = StandardCharsets.UTF_8;

  @NotNull
  @Override
  public List<Payload> encode(@NotNull List<Payload> payloads) {
    return payloads.stream().map(this::encodePayload).collect(Collectors.toList());
  }

  @NotNull
  @Override
  public List<Payload> decode(@NotNull List<Payload> payloads) {
    return payloads.stream().map(this::decodePayload).collect(Collectors.toList());
  }

  private Payload encodePayload(Payload payload) {
    String keyId = getKeyId();
    SecretKey key = getKey(keyId);

    byte[] encryptedData;
    try {
      encryptedData = encrypt(payload.toByteArray(), key);
    } catch (Throwable e) {
      throw new DataConverterException(e);
    }

    return Payload.newBuilder()
        .putMetadata(EncodingKeys.METADATA_ENCODING_KEY, METADATA_ENCODING)
        .putMetadata(METADATA_ENCRYPTION_CIPHER_KEY, METADATA_ENCRYPTION_CIPHER)
        .putMetadata(METADATA_ENCRYPTION_KEY_ID_KEY, ByteString.copyFromUtf8(keyId))
        .setData(ByteString.copyFrom(encryptedData))
        .build();
  }

  private Payload decodePayload(Payload payload) {
    if (METADATA_ENCODING.equals(
        payload.getMetadataOrDefault(EncodingKeys.METADATA_ENCODING_KEY, null))) {
      String keyId;
      try {
        keyId = payload.getMetadataOrThrow(METADATA_ENCRYPTION_KEY_ID_KEY).toString(UTF_8);
      } catch (Exception e) {
        throw new PayloadCodecException(e);
      }
      SecretKey key = getKey(keyId);

      byte[] plainData;
      Payload decryptedPayload;

      try {
        plainData = decrypt(payload.getData().toByteArray(), key);
        decryptedPayload = Payload.parseFrom(plainData);
        return decryptedPayload;
      } catch (Throwable e) {
        throw new PayloadCodecException(e);
      }
    } else {
      return payload;
    }
  }

  private String getKeyId() {
    // Currently there is no context available to vary which key is used.
    // Use a fixed key for all payloads.
    // This still supports key rotation as the key ID is recorded on payloads allowing
    // decryption to use a previous key.

    return "test-key-test-key-test-key-test!";
  }

  private SecretKey getKey(String keyId) {
    // Key must be fetched from KMS or other secure storage.
    // Hard coded here only for example purposes.
    return new SecretKeySpec(keyId.getBytes(UTF_8), "AES");
  }

  private static byte[] getNonce(int size) {
    byte[] nonce = new byte[size];
    new SecureRandom().nextBytes(nonce);
    return nonce;
  }

  private byte[] encrypt(byte[] plainData, SecretKey key) throws Exception {
    byte[] nonce = getNonce(GCM_NONCE_LENGTH_BYTE);

    Cipher cipher = Cipher.getInstance(CIPHER);
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BIT, nonce));

    byte[] encryptedData = cipher.doFinal(plainData);
    return ByteBuffer.allocate(nonce.length + encryptedData.length)
        .put(nonce)
        .put(encryptedData)
        .array();
  }

  private byte[] decrypt(byte[] encryptedDataWithNonce, SecretKey key) throws Exception {
    ByteBuffer buffer = ByteBuffer.wrap(encryptedDataWithNonce);

    byte[] nonce = new byte[GCM_NONCE_LENGTH_BYTE];
    buffer.get(nonce);
    byte[] encryptedData = new byte[buffer.remaining()];
    buffer.get(encryptedData);

    Cipher cipher = Cipher.getInstance(CIPHER);
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BIT, nonce));

    return cipher.doFinal(encryptedData);
  }
}
