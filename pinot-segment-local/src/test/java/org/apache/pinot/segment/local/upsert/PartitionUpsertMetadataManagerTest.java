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
package org.apache.pinot.segment.local.upsert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pinot.common.metrics.ServerMetrics;
import org.apache.pinot.common.utils.LLCSegmentName;
import org.apache.pinot.segment.local.indexsegment.immutable.ImmutableSegmentImpl;
import org.apache.pinot.segment.local.utils.HashUtils;
import org.apache.pinot.segment.local.utils.RecordInfo;
import org.apache.pinot.segment.spi.IndexSegment;
import org.apache.pinot.segment.spi.index.mutable.ThreadSafeMutableRoaringBitmap;
import org.apache.pinot.spi.config.table.HashFunction;
import org.apache.pinot.spi.data.readers.PrimaryKey;
import org.apache.pinot.spi.utils.ByteArray;
import org.apache.pinot.spi.utils.BytesUtils;
import org.apache.pinot.spi.utils.builder.TableNameBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;


public class PartitionUpsertMetadataManagerTest {
  private static final String RAW_TABLE_NAME = "testTable";
  private static final String REALTIME_TABLE_NAME = TableNameBuilder.REALTIME.tableNameWithType(RAW_TABLE_NAME);

  @Test
  public void testAddSegment() {
    verifyAddSegment(HashFunction.NONE);
    verifyAddSegment(HashFunction.MD5);
    verifyAddSegment(HashFunction.MURMUR3);
  }

  private void verifyAddSegment(HashFunction hashFunction) {
    PartitionUpsertMetadataManager upsertMetadataManager =
        new PartitionUpsertMetadataManager(REALTIME_TABLE_NAME, 0, mock(ServerMetrics.class), null, hashFunction);
    Map<Object, RecordLocation> recordLocationMap = upsertMetadataManager._primaryKeyToRecordLocationMap;

    // Add the first segment
    List<RecordInfo> recordInfoList1 = new ArrayList<>();
    recordInfoList1.add(new RecordInfo(getPrimaryKey(0), 0, new IntWrapper(100)));
    recordInfoList1.add(new RecordInfo(getPrimaryKey(1), 1, new IntWrapper(100)));
    recordInfoList1.add(new RecordInfo(getPrimaryKey(2), 2, new IntWrapper(100)));
    recordInfoList1.add(new RecordInfo(getPrimaryKey(0), 3, new IntWrapper(80)));
    recordInfoList1.add(new RecordInfo(getPrimaryKey(1), 4, new IntWrapper(120)));
    recordInfoList1.add(new RecordInfo(getPrimaryKey(0), 5, new IntWrapper(100)));
    ThreadSafeMutableRoaringBitmap validDocIds1 = new ThreadSafeMutableRoaringBitmap();
    ImmutableSegmentImpl segment1 = mockSegment(1, validDocIds1);
    upsertMetadataManager.addSegment(segment1, recordInfoList1.iterator());
    // segment1: 0 -> {5, 100}, 1 -> {4, 120}, 2 -> {2, 100}
    checkRecordLocation(recordLocationMap, 0, segment1, 5, 100, hashFunction);
    checkRecordLocation(recordLocationMap, 1, segment1, 4, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 2, segment1, 2, 100, hashFunction);
    assertEquals(validDocIds1.getMutableRoaringBitmap().toArray(), new int[]{2, 4, 5});

    // Add the second segment
    ArrayList<RecordInfo> recordInfoList2 = new ArrayList<>();
    recordInfoList2.add(new RecordInfo(getPrimaryKey(0), 0, new IntWrapper(100)));
    recordInfoList2.add(new RecordInfo(getPrimaryKey(1), 1, new IntWrapper(100)));
    recordInfoList2.add(new RecordInfo(getPrimaryKey(2), 2, new IntWrapper(120)));
    recordInfoList2.add(new RecordInfo(getPrimaryKey(3), 3, new IntWrapper(80)));
    recordInfoList2.add(new RecordInfo(getPrimaryKey(0), 4, new IntWrapper(80)));
    ThreadSafeMutableRoaringBitmap validDocIds2 = new ThreadSafeMutableRoaringBitmap();
    ImmutableSegmentImpl segment2 = mockSegment(2, validDocIds2);
    upsertMetadataManager.addSegment(segment2, recordInfoList2.iterator());
    // segment1: 1 -> {4, 120}
    // segment2: 0 -> {0, 100}, 2 -> {2, 120}, 3 -> {3, 80}
    checkRecordLocation(recordLocationMap, 0, segment2, 0, 100, hashFunction);
    checkRecordLocation(recordLocationMap, 1, segment1, 4, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 2, segment2, 2, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 3, segment2, 3, 80, hashFunction);
    assertEquals(validDocIds1.getMutableRoaringBitmap().toArray(), new int[]{4});
    assertEquals(validDocIds2.getMutableRoaringBitmap().toArray(), new int[]{0, 2, 3});

    // Replace (reload) the first segment
    ThreadSafeMutableRoaringBitmap newValidDocIds1 = new ThreadSafeMutableRoaringBitmap();
    ImmutableSegmentImpl newSegment1 = mockSegment(1, newValidDocIds1);
    upsertMetadataManager.addSegment(newSegment1, recordInfoList1.iterator());
    // original segment1: 1 -> {4, 120}
    // segment2: 0 -> {0, 100}, 2 -> {2, 120}, 3 -> {3, 80}
    // new segment1: 1 -> {4, 120}
    checkRecordLocation(recordLocationMap, 0, segment2, 0, 100, hashFunction);
    checkRecordLocation(recordLocationMap, 1, newSegment1, 4, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 2, segment2, 2, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 3, segment2, 3, 80, hashFunction);
    assertEquals(validDocIds1.getMutableRoaringBitmap().toArray(), new int[]{4});
    assertEquals(validDocIds2.getMutableRoaringBitmap().toArray(), new int[]{0, 2, 3});
    assertEquals(newValidDocIds1.getMutableRoaringBitmap().toArray(), new int[]{4});
    assertSame(recordLocationMap.get(HashUtils.hashPrimaryKey(getPrimaryKey(1), hashFunction))
        .getSegment(), newSegment1);

    // Remove the original segment1
    upsertMetadataManager.removeSegment(segment1);
    // segment2: 0 -> {0, 100}, 2 -> {2, 120}, 3 -> {3, 80}
    // new segment1: 1 -> {4, 120}
    checkRecordLocation(recordLocationMap, 0, segment2, 0, 100, hashFunction);
    checkRecordLocation(recordLocationMap, 1, newSegment1, 4, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 2, segment2, 2, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 3, segment2, 3, 80, hashFunction);
    assertEquals(validDocIds2.getMutableRoaringBitmap().toArray(), new int[]{0, 2, 3});
    assertEquals(newValidDocIds1.getMutableRoaringBitmap().toArray(), new int[]{4});
    assertSame(recordLocationMap.get(HashUtils.hashPrimaryKey(getPrimaryKey(1), hashFunction))
        .getSegment(), newSegment1);
  }

  private static ImmutableSegmentImpl mockSegment(int sequenceNumber, ThreadSafeMutableRoaringBitmap validDocIds) {
    ImmutableSegmentImpl segment = mock(ImmutableSegmentImpl.class);
    String segmentName = getSegmentName(sequenceNumber);
    when(segment.getSegmentName()).thenReturn(segmentName);
    when(segment.getValidDocIds()).thenReturn(validDocIds);
    return segment;
  }

  private static String getSegmentName(int sequenceNumber) {
    return new LLCSegmentName(RAW_TABLE_NAME, 0, sequenceNumber, System.currentTimeMillis()).toString();
  }

  private static PrimaryKey getPrimaryKey(int value) {
    return new PrimaryKey(new Object[]{value});
  }

  private static void checkRecordLocation(Map<Object, RecordLocation> recordLocationMap, int keyValue,
      IndexSegment segment, int docId, int comparisonValue, HashFunction hashFunction) {
    RecordLocation recordLocation =
        recordLocationMap.get(HashUtils.hashPrimaryKey(getPrimaryKey(keyValue), hashFunction));
    assertNotNull(recordLocation);
    assertSame(recordLocation.getSegment(), segment);
    assertEquals(recordLocation.getDocId(), docId);
    assertEquals(((IntWrapper) recordLocation.getComparisonValue())._value, comparisonValue);
  }

  @Test
  public void testAddRecord() {
    verifyAddRecord(HashFunction.NONE);
    verifyAddRecord(HashFunction.MD5);
    verifyAddRecord(HashFunction.MURMUR3);
  }

  private void verifyAddRecord(HashFunction hashFunction) {
    PartitionUpsertMetadataManager upsertMetadataManager =
        new PartitionUpsertMetadataManager(REALTIME_TABLE_NAME, 0, mock(ServerMetrics.class), null, hashFunction);
    Map<Object, RecordLocation> recordLocationMap = upsertMetadataManager._primaryKeyToRecordLocationMap;

    // Add the first segment
    // segment1: 0 -> {0, 100}, 1 -> {1, 120}, 2 -> {2, 100}
    List<RecordInfo> recordInfoList1 = new ArrayList<>();
    recordInfoList1.add(new RecordInfo(getPrimaryKey(0), 0, new IntWrapper(100)));
    recordInfoList1.add(new RecordInfo(getPrimaryKey(1), 1, new IntWrapper(120)));
    recordInfoList1.add(new RecordInfo(getPrimaryKey(2), 2, new IntWrapper(100)));
    ThreadSafeMutableRoaringBitmap validDocIds1 = new ThreadSafeMutableRoaringBitmap();
    ImmutableSegmentImpl segment1 = mockSegment(1, validDocIds1);
    upsertMetadataManager.addSegment(segment1, recordInfoList1.iterator());

    // Update records from the second segment
    ThreadSafeMutableRoaringBitmap validDocIds2 = new ThreadSafeMutableRoaringBitmap();
    IndexSegment segment2 = mockSegment(1, validDocIds2);

    upsertMetadataManager.addRecord(segment2,
        new RecordInfo(getPrimaryKey(3), 0, new IntWrapper(100)));
    // segment1: 0 -> {0, 100}, 1 -> {1, 120}, 2 -> {2, 100}
    // segment2: 3 -> {0, 100}
    checkRecordLocation(recordLocationMap, 0, segment1, 0, 100, hashFunction);
    checkRecordLocation(recordLocationMap, 1, segment1, 1, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 2, segment1, 2, 100, hashFunction);
    checkRecordLocation(recordLocationMap, 3, segment2, 0, 100, hashFunction);
    assertEquals(validDocIds1.getMutableRoaringBitmap().toArray(), new int[]{0, 1, 2});
    assertEquals(validDocIds2.getMutableRoaringBitmap().toArray(), new int[]{0});

    upsertMetadataManager.addRecord(segment2,
        new RecordInfo(getPrimaryKey(2), 1, new IntWrapper(120)));
    // segment1: 0 -> {0, 100}, 1 -> {1, 120}
    // segment2: 2 -> {1, 120}, 3 -> {0, 100}
    checkRecordLocation(recordLocationMap, 0, segment1, 0, 100, hashFunction);
    checkRecordLocation(recordLocationMap, 1, segment1, 1, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 2, segment2, 1, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 3, segment2, 0, 100, hashFunction);
    assertEquals(validDocIds1.getMutableRoaringBitmap().toArray(), new int[]{0, 1});
    assertEquals(validDocIds2.getMutableRoaringBitmap().toArray(), new int[]{0, 1});

    upsertMetadataManager.addRecord(segment2,
        new RecordInfo(getPrimaryKey(1), 2, new IntWrapper(100)));
    // segment1: 0 -> {0, 100}, 1 -> {1, 120}
    // segment2: 2 -> {1, 120}, 3 -> {0, 100}
    checkRecordLocation(recordLocationMap, 0, segment1, 0, 100, hashFunction);
    checkRecordLocation(recordLocationMap, 1, segment1, 1, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 2, segment2, 1, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 3, segment2, 0, 100, hashFunction);
    assertEquals(validDocIds1.getMutableRoaringBitmap().toArray(), new int[]{0, 1});
    assertEquals(validDocIds2.getMutableRoaringBitmap().toArray(), new int[]{0, 1});

    upsertMetadataManager.addRecord(segment2,
        new RecordInfo(getPrimaryKey(0), 3, new IntWrapper(100)));
    // segment1: 1 -> {1, 120}
    // segment2: 0 -> {3, 100}, 2 -> {1, 120}, 3 -> {0, 100}
    checkRecordLocation(recordLocationMap, 0, segment2, 3, 100, hashFunction);
    checkRecordLocation(recordLocationMap, 1, segment1, 1, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 2, segment2, 1, 120, hashFunction);
    checkRecordLocation(recordLocationMap, 3, segment2, 0, 100, hashFunction);
    assertEquals(validDocIds1.getMutableRoaringBitmap().toArray(), new int[]{1});
    assertEquals(validDocIds2.getMutableRoaringBitmap().toArray(), new int[]{0, 1, 3});
  }

  @Test
  public void testRemoveSegment() {
    verifyRemoveSegment(HashFunction.NONE);
    verifyRemoveSegment(HashFunction.MD5);
    verifyRemoveSegment(HashFunction.MURMUR3);
  }

  private void verifyRemoveSegment(HashFunction hashFunction) {
    PartitionUpsertMetadataManager upsertMetadataManager =
        new PartitionUpsertMetadataManager(REALTIME_TABLE_NAME, 0, mock(ServerMetrics.class), null, hashFunction);
    Map<Object, RecordLocation> recordLocationMap = upsertMetadataManager._primaryKeyToRecordLocationMap;

    // Add 2 segments
    // segment1: 0 -> {0, 100}, 1 -> {1, 100}
    // segment2: 2 -> {0, 100}, 3 -> {0, 100}
    List<RecordInfo> recordInfoList1 = new ArrayList<>();
    recordInfoList1.add(new RecordInfo(getPrimaryKey(0), 0, new IntWrapper(100)));
    recordInfoList1.add(new RecordInfo(getPrimaryKey(1), 1, new IntWrapper(100)));
    ThreadSafeMutableRoaringBitmap validDocIds1 = new ThreadSafeMutableRoaringBitmap();
    ImmutableSegmentImpl segment1 = mockSegment(1, validDocIds1);
    upsertMetadataManager.addSegment(segment1, recordInfoList1.iterator());
    List<RecordInfo> recordInfoList2 = new ArrayList<>();
    recordInfoList2.add(new RecordInfo(getPrimaryKey(2), 0, new IntWrapper(100)));
    recordInfoList2.add(new RecordInfo(getPrimaryKey(3), 1, new IntWrapper(100)));
    ThreadSafeMutableRoaringBitmap validDocIds2 = new ThreadSafeMutableRoaringBitmap();
    ImmutableSegmentImpl segment2 = mockSegment(2, validDocIds2);
    upsertMetadataManager.addSegment(segment2, recordInfoList2.iterator());

    // Remove the first segment
    upsertMetadataManager.removeSegment(segment1);
    // segment2: 2 -> {0, 100}, 3 -> {0, 100}
    assertNull(recordLocationMap.get(getPrimaryKey(0)));
    assertNull(recordLocationMap.get(getPrimaryKey(1)));
    checkRecordLocation(recordLocationMap, 2, segment2, 0, 100, hashFunction);
    checkRecordLocation(recordLocationMap, 3, segment2, 1, 100, hashFunction);
    assertEquals(validDocIds2.getMutableRoaringBitmap().toArray(), new int[]{0, 1});
  }

  @Test
  public void testHashPrimaryKey() {
    PrimaryKey pk = new PrimaryKey(new Object[]{"uuid-1", "uuid-2", "uuid-3"});
    Assert.assertEquals(BytesUtils.toHexString(
        ((ByteArray) HashUtils.hashPrimaryKey(pk, HashFunction.MD5)).getBytes()),
        "58de44997505014e02982846a4d1cbbd");
    Assert.assertEquals(BytesUtils.toHexString(
        ((ByteArray) HashUtils.hashPrimaryKey(pk, HashFunction.MURMUR3)).getBytes()),
        "7e6b4a98296292a4012225fff037fa8c");
    // reorder
    pk = new PrimaryKey(new Object[]{"uuid-3", "uuid-2", "uuid-1"});
    Assert.assertEquals(BytesUtils.toHexString(
        ((ByteArray) HashUtils.hashPrimaryKey(pk, HashFunction.MD5)).getBytes()),
        "d2df12c6dea7b83f965613614eee58e2");
    Assert.assertEquals(BytesUtils.toHexString(
        ((ByteArray) HashUtils.hashPrimaryKey(pk, HashFunction.MURMUR3)).getBytes()),
        "8d68b314cc0c8de4dbd55f4dad3c3e66");
  }

  /**
   * Use a wrapper class to ensure different value has different reference.
   */
  private static class IntWrapper implements Comparable<IntWrapper> {
    final int _value;

    IntWrapper(int value) {
      _value = value;
    }

    @Override
    public int compareTo(IntWrapper o) {
      return Integer.compare(_value, o._value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof IntWrapper)) {
        return false;
      }
      IntWrapper that = (IntWrapper) o;
      return _value == that._value;
    }

    @Override
    public int hashCode() {
      return _value;
    }
  }
}
