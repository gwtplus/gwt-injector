/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gwtproject.injector;

import org.gwtproject.injector.client.ScriptInjectorTest;
import org.gwtproject.injector.client.StyleInjectorTest;

import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * All injector tests that use GWTTestCase.
 */
public class InjectorGwtSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("All injector tests");

    suite.addTestSuite(ScriptInjectorTest.class);
    suite.addTestSuite(StyleInjectorTest.class);

    return suite;
  }
}
