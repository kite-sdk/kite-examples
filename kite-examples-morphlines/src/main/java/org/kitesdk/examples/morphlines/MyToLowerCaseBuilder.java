/*
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitesdk.examples.morphlines;

import java.util.Collection;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Locale;

import org.kitesdk.morphline.api.Command;
import org.kitesdk.morphline.api.CommandBuilder;
import org.kitesdk.morphline.api.MorphlineContext;
import org.kitesdk.morphline.api.Record;
import org.kitesdk.morphline.base.AbstractCommand;

import com.typesafe.config.Config;


/**
 * Example custom morphline command that lowercases a string, and optionally reverses the order of
 * the characters in the string.
 */
public final class MyToLowerCaseBuilder implements CommandBuilder {
  
  @Override
  public Collection<String> getNames() {
    return Collections.singletonList("myToLowerCase");
  }
  
  @Override
  public Command build(Config config, Command parent, Command child, MorphlineContext context) {
    return new MyToLowerCase(this, config, parent, child, context);
  }
  
  
  ///////////////////////////////////////////////////////////////////////////////
  // Nested classes:
  ///////////////////////////////////////////////////////////////////////////////
  private static final class MyToLowerCase extends AbstractCommand {
    
    private final String fieldName;
    private final boolean reverse;
    private final Locale locale;

    public MyToLowerCase(CommandBuilder builder, Config config, Command parent, Command child, MorphlineContext context) {
      super(builder, config, parent, child, context);
      this.fieldName = getConfigs().getString(config, "field");
      this.locale = getConfigs().getLocale(config, "locale", Locale.ROOT);
      this.reverse = getConfigs().getBoolean(config, "reverse", false);
      LOG.debug("fieldName: {}", fieldName);
      validateArguments();
    }
    
    @Override
    protected boolean doProcess(Record record) {
      ListIterator iter = record.get(fieldName).listIterator();
      while (iter.hasNext()) {
        iter.set(transformFieldValue(iter.next()));
      }
      
      // pass record to next command in chain:
      return super.doProcess(record);
    }
    
    /** Transforms the given input value to some output value */
    private Object transformFieldValue(Object value) {
      String str = value.toString().toLowerCase(locale);
      if (reverse) {
        str = new StringBuilder(str).reverse().toString();
      }
      return str;
    }

    @Override
    protected void doNotify(Record notification) {
      LOG.debug("myNotification: {}", notification);
      super.doNotify(notification);
    }
    
  }
  
}
