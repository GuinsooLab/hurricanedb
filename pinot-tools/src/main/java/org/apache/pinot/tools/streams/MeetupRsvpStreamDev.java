/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.tools.streams;

import org.apache.pinot.spi.stream.StreamDataProducer;
import org.apache.pinot.spi.stream.StreamDataProvider;
import org.apache.pinot.tools.utils.KafkaStarterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;


public class MeetupRsvpStreamDev {
  protected static final Logger LOGGER = LoggerFactory.getLogger(MeetupRsvpStreamDev.class);
  private static final String DEFAULT_TOPIC_NAME = "meetupRSVPEvents";
  private static final String DEFAULT_KAFKA_BROKER_DEV = "192.168.120.17:9092";
  protected String _topicName = DEFAULT_TOPIC_NAME;

  protected PinotRealtimeSource _pinotRealtimeSource;

  public MeetupRsvpStreamDev()
      throws Exception {
    this(false);
  }

  public MeetupRsvpStreamDev(boolean partitionByKey)
      throws Exception {
    // calling this constructor means that we wish to use EVENT_ID as key. RsvpId is used by MeetupRsvpJsonStream
    this(partitionByKey ? RsvpSourceGenerator.KeyColumn.EVENT_ID : RsvpSourceGenerator.KeyColumn.NONE);
  }

  public MeetupRsvpStreamDev(RsvpSourceGenerator.KeyColumn keyColumn)
      throws Exception {
    Properties properties = new Properties();
    properties.put("metadata.broker.list", DEFAULT_KAFKA_BROKER_DEV);
    properties.put("serializer.class", "kafka.serializer.DefaultEncoder");
    properties.put("request.required.acks", "1");
    StreamDataProducer producer =
        StreamDataProvider.getStreamDataProducer(KafkaStarterUtils.KAFKA_PRODUCER_CLASS_NAME, properties);
    _pinotRealtimeSource =
        PinotRealtimeSource.builder().setGenerator(new RsvpSourceGenerator(keyColumn)).setProducer(producer)
            .setRateLimiter(permits -> {
              int delay = (int) (Math.log(ThreadLocalRandom.current().nextDouble()) / Math.log(0.999)) + 1;
              try {
                Thread.sleep(delay);
              } catch (InterruptedException ex) {
                LOGGER.warn("Interrupted from sleep but will continue", ex);
              }
            })
            .setTopic(_topicName)
            .build();
  }

  public void run()
      throws Exception {
    _pinotRealtimeSource.run();
  }

  public void stopPublishing() {
    try {
      _pinotRealtimeSource.close();
    } catch (Exception ex) {
      LOGGER.error("Failed to close real time source. ignored and continue", ex);
    }
  }
}