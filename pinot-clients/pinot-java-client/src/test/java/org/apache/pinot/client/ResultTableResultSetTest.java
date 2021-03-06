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
package org.apache.pinot.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.apache.pinot.spi.utils.JsonUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;


public class ResultTableResultSetTest {

  private JsonNode _jsonNode;

  private ResultTableResultSet _resultTableResultSetUnderTest;

  @BeforeTest
  public void setUp()
      throws Exception {
    String json =
        "{ \"rows\" : [[\"r1c1\", \"r1c2\"], [\"r2c1\", \"r2c2\"]], \"dataSchema\" : {\"columnNames\":[\"column1\", "
            + "\"column2\"], " + "\"columnDataTypes\":[\"column1DataType\", \"column2DataType\"]} } ";
    _jsonNode = JsonUtils.stringToJsonNode(json);
    _resultTableResultSetUnderTest = new ResultTableResultSet(_jsonNode);
  }

  @Test
  public void testGetRowCount() {
    // Run the test
    final int result = _resultTableResultSetUnderTest.getRowCount();

    // Verify the results
    assertEquals(2, result);
  }

  @Test
  public void testGetColumnCount() {
    // Run the test
    final int result = _resultTableResultSetUnderTest.getColumnCount();

    // Verify the results
    assertEquals(2, result);
  }

  @Test
  public void testGetColumnName() {
    // Run the test
    final String result = _resultTableResultSetUnderTest.getColumnName(0);

    // Verify the results
    assertEquals("column1", result);
  }

  @Test
  public void testGetColumnDataType() {
    // Run the test
    final String result = _resultTableResultSetUnderTest.getColumnDataType(0);

    // Verify the results
    assertEquals("column1DataType", result);
  }

  @Test
  public void testGetString() {
    // Run the test
    final String result = _resultTableResultSetUnderTest.getString(0, 0);

    // Verify the results
    assertEquals("r1c1", result);
  }

  @Test
  public void testGetAllColumns() {
    // Run the test
    final List<String> result = _resultTableResultSetUnderTest.getAllColumns();

    // Verify the results
    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  public void testGetAllColumnsDataTypes() {
    // Run the test
    final List<String> result = _resultTableResultSetUnderTest.getAllColumnsDataTypes();

    // Verify the results
    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  public void testGetGroupKeyLength() {
    // Run the test
    final int result = _resultTableResultSetUnderTest.getGroupKeyLength();

    // Verify the results
    assertEquals(0, result);
  }

  @Test(expectedExceptions = AssertionError.class)
  public void testGetGroupKeyString() {
    // Run the test
    _resultTableResultSetUnderTest.getGroupKeyString(0, 0);
  }

  @Test(expectedExceptions = AssertionError.class)
  public void testGetGroupKeyColumnName() {
    // Run the test
    _resultTableResultSetUnderTest.getGroupKeyColumnName(0);
  }

  @Test
  public void testToString() {
    // Run the test
    final String result = _resultTableResultSetUnderTest.toString();

    // Verify the results
    assertNotEquals("", result);
  }
}
