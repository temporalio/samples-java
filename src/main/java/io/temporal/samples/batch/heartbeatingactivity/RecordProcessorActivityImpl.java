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

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RecordProcessorActivity implementation.
 *
 * <p>It relies on RecordLoader to iterate over set of records and process them one by one. The
 * heartbeat is used to remember offset. On activity retry the data from the last recorded heartbeat
 * is used to minimize the number of records that are reprocessed. Note that not every heartbeat
 * call is sent to the service. The frequency of the heartbeat service calls depends on the
 * heartbeat timeout the activity was scheduled with. If no heartbeat timeout is not set then no
 * heartbeat is ever sent to the service.
 *
 * <p>The biggest advantage of this approach is efficiency. It uses very few Temporal resources.
 *
 * <p>The biggest limitation of this approach is that it cannot deal with record processing
 * failures. The only options are either failing the whole batch or skip the record. While it is
 * possible to build additional logic to record failed records somewhere the experience is not
 * seamless.
 */
public class RecordProcessorActivityImpl implements RecordProcessorActivity {

  private static final Logger log = LoggerFactory.getLogger(RecordProcessorActivityImpl.class);

  private final RecordLoader recordLoader;

  private final RecordProcessor recordProcessor;

  public RecordProcessorActivityImpl(RecordLoader recordLoader, RecordProcessor recordProcessor) {
    this.recordLoader = recordLoader;
    this.recordProcessor = recordProcessor;
  }

  @Override
  public int processRecords() {
    // On activity retry load the last reported offset from the heartbeat details.
    ActivityExecutionContext context = Activity.getExecutionContext();
    Optional<Integer> heartbeatDetails = context.getHeartbeatDetails(Integer.class);
    int offset = heartbeatDetails.orElse(0);
    log.info("Activity processRecords started with offset=" + offset);
    // This sample implementation processes records one by one.
    // If needed it can be changed to use a pool of threads or asynchronous code to process multiple
    // such records in parallel.
    while (true) {
      Optional<SingleRecord> record = recordLoader.getRecord(offset);
      if (!record.isPresent()) {
        return offset;
      }
      recordProcessor.processRecord(record.get());
      // Report that activity is still alive. The assumption is that each record takes less time
      // to process than the heartbeat timeout.
      // Leverage heartbeat details to record offset.
      context.heartbeat(offset);
      offset++;
    }
  }
}
