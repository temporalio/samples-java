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

package io.temporal.samples.batch.heartbeatingactivity;

import java.util.Optional;

/**
 * Helper class that is used to iterate over a list of records.
 *
 * <p>A specific implementation depends on a use case. For example, it can execute an SQL DB query
 * or read a comma delimited file. More complex use cases would need passing a different type of
 * offset parameter.
 */
public interface RecordLoader {

  /**
   * Returns the next record.
   *
   * @param offset offset of the next record.
   * @return Record at the offset. Empty optional if offset exceeds the dataset size.
   */
  Optional<SingleRecord> getRecord(int offset);
}
