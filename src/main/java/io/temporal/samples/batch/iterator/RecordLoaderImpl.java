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

package io.temporal.samples.batch.iterator;

import java.util.ArrayList;
import java.util.List;

/** Fake implementation of RecordLoader. */
public final class RecordLoaderImpl implements RecordLoader {

  // The sample always returns 5 pages.
  // The real application would iterate over an existing dataset or file.
  public static final int PAGE_COUNT = 5;

  @Override
  public List<SingleRecord> getRecords(int pageSize, int offset) {
    List<SingleRecord> records = new ArrayList<>(pageSize);
    if (offset < pageSize * PAGE_COUNT) {
      for (int i = 0; i < pageSize; i++) {
        records.add(new SingleRecord(offset + i));
      }
    }
    return records;
  }
}
