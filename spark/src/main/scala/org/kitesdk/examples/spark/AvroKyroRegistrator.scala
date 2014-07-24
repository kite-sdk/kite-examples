/*
 * Copyright 2014 Cloudera, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitesdk.examples.spark

import org.apache.spark.serializer.KryoRegistrator
import com.esotericsoftware.kryo.Kryo
import com.twitter.chill.avro.AvroSerializer
import org.kitesdk.data.event.StandardEvent
import org.kitesdk.data.event.CorrelatedEvents
 
class AvroKyroRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    kryo.register(classOf[StandardEvent], AvroSerializer.SpecificRecordBinarySerializer[StandardEvent])
    kryo.register(classOf[CorrelatedEvents], AvroSerializer.SpecificRecordBinarySerializer[CorrelatedEvents])
  }
}
