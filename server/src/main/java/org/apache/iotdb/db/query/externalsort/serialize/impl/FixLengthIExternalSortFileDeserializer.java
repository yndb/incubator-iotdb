/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.db.query.externalsort.serialize.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.iotdb.db.query.externalsort.serialize.IExternalSortFileDeserializer;
import org.apache.iotdb.db.utils.TimeValuePair;
import org.apache.iotdb.tsfile.utils.TsPrimitiveType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.utils.Binary;
import org.apache.iotdb.tsfile.utils.BytesUtils;
import org.apache.iotdb.tsfile.utils.ReadWriteIOUtils;

/**
 * FileFormat: [Header][Body]
 * <p>
 * [Header] = [DataTypeLength] + [DataTypeInStringBytes]
 * <p>
 * [DataTypeLength] = 4 bytes.
 */
public class FixLengthIExternalSortFileDeserializer implements IExternalSortFileDeserializer {

  private TimeValuePairReader reader;
  private InputStream inputStream;
  private String tmpFilePath;

  public FixLengthIExternalSortFileDeserializer(String tmpFilePath) throws IOException {
    this.tmpFilePath = tmpFilePath;
    inputStream = new BufferedInputStream(new FileInputStream(tmpFilePath));
    TSDataType dataType = readHeader();
    setReader(dataType);
  }

  @Override
  public boolean hasNext() throws IOException {
    return inputStream.available() > 0;
  }

  @Override
  public TimeValuePair next() throws IOException {
    return reader.read(inputStream);
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
    File file = new File(tmpFilePath);
    if (!file.exists()) {
      return;
    }
    if (!file.delete()) {
      throw new IOException("Delete external sort tmp file error. FilePath:" + tmpFilePath);
    }
  }

  public String getTmpFilePath() {
    return tmpFilePath;
  }

  private TSDataType readHeader() throws IOException {
    return TSDataType.deserialize(ReadWriteIOUtils.readShort(inputStream));
  }

  private void setReader(TSDataType type) {
    switch (type) {
      case BOOLEAN:
        this.reader = new TimeValuePairReader.BooleanReader();
        break;
      case INT32:
        this.reader = new TimeValuePairReader.IntReader();
        break;
      case INT64:
        this.reader = new TimeValuePairReader.LongReader();
        break;
      case FLOAT:
        this.reader = new TimeValuePairReader.FloatReader();
        break;
      case DOUBLE:
        this.reader = new TimeValuePairReader.DoubleReader();
        break;
      case TEXT:
        this.reader = new TimeValuePairReader.BinaryReader();
        break;
      default:
        throw new RuntimeException("Unknown TSDataType in FixLengthTimeValuePairSerializer:"
            + type);
    }
  }

  private abstract static class TimeValuePairReader {

    public abstract TimeValuePair read(InputStream inputStream) throws IOException;

    private static class BooleanReader
        extends FixLengthIExternalSortFileDeserializer.TimeValuePairReader {

      byte[] timestampBytes = new byte[8];
      byte[] valueBytes = new byte[1];

      @Override
      public TimeValuePair read(InputStream inputStream) throws IOException {
        inputStream.read(timestampBytes);
        inputStream.read(valueBytes);
        return new TimeValuePair(BytesUtils.bytesToLong(timestampBytes),
            new TsPrimitiveType.TsBoolean(BytesUtils.bytesToBool(valueBytes)));
      }
    }

    private static class IntReader extends
        FixLengthIExternalSortFileDeserializer.TimeValuePairReader {

      byte[] timestampBytes = new byte[8];
      byte[] valueBytes = new byte[4];

      @Override
      public TimeValuePair read(InputStream inputStream) throws IOException {
        inputStream.read(timestampBytes);
        inputStream.read(valueBytes);
        return new TimeValuePair(BytesUtils.bytesToLong(timestampBytes),
            new TsPrimitiveType.TsInt(BytesUtils.bytesToInt(valueBytes)));
      }
    }

    private static class LongReader extends
        FixLengthIExternalSortFileDeserializer.TimeValuePairReader {

      byte[] timestampBytes = new byte[8];
      byte[] valueBytes = new byte[8];

      @Override
      public TimeValuePair read(InputStream inputStream) throws IOException {
        inputStream.read(timestampBytes);
        inputStream.read(valueBytes);
        return new TimeValuePair(BytesUtils.bytesToLong(timestampBytes),
            new TsPrimitiveType.TsLong(BytesUtils.bytesToLong(valueBytes)));
      }
    }

    private static class FloatReader
        extends FixLengthIExternalSortFileDeserializer.TimeValuePairReader {

      byte[] timestampBytes = new byte[8];
      byte[] valueBytes = new byte[4];

      @Override
      public TimeValuePair read(InputStream inputStream) throws IOException {
        inputStream.read(timestampBytes);
        inputStream.read(valueBytes);
        return new TimeValuePair(BytesUtils.bytesToLong(timestampBytes),
            new TsPrimitiveType.TsFloat(BytesUtils.bytesToFloat(valueBytes)));
      }
    }

    private static class DoubleReader
        extends FixLengthIExternalSortFileDeserializer.TimeValuePairReader {

      byte[] timestampBytes = new byte[8];
      byte[] valueBytes = new byte[8];

      @Override
      public TimeValuePair read(InputStream inputStream) throws IOException {
        inputStream.read(timestampBytes);
        inputStream.read(valueBytes);
        return new TimeValuePair(BytesUtils.bytesToLong(timestampBytes),
            new TsPrimitiveType.TsDouble(BytesUtils.bytesToDouble(valueBytes)));
      }
    }

    private static class BinaryReader
        extends FixLengthIExternalSortFileDeserializer.TimeValuePairReader {

      byte[] timestampBytes = new byte[8];
      byte[] valueLength = new byte[4];
      byte[] valueBytes;

      @Override
      public TimeValuePair read(InputStream inputStream) throws IOException {
        inputStream.read(timestampBytes);
        inputStream.read(valueLength);
        int length = BytesUtils.bytesToInt(valueLength);
        valueBytes = new byte[length];
        inputStream.read(valueBytes);
        return new TimeValuePair(BytesUtils.bytesToLong(timestampBytes),
            new TsPrimitiveType.TsBinary(new Binary(BytesUtils.bytesToString(valueBytes))));
      }
    }
  }
}
