/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.util;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

/**
 * @version $Revision$
 */
public class ObjectHelperTest extends TestCase {
    public void testRemoveInitialCharacters() throws Exception {
        assertEquals(ObjectHelper.removeStartingCharacters("foo", '/'), "foo");
        assertEquals(ObjectHelper.removeStartingCharacters("/foo", '/'), "foo");
        assertEquals(ObjectHelper.removeStartingCharacters("//foo", '/'), "foo");
    }

    public void testGetPropertyName() throws Exception {
        Method method = getClass().getMethod("setCheese", String.class);
        assertNotNull("should have found a method!", method);

        String name = ObjectHelper.getPropertyName(method);
        assertEquals("Property name", "cheese", name);
    }

    public void testContains() throws Exception {
        String[] array = {"foo", "bar"};
        Collection<String> collection = Arrays.asList(array);
        
        assertTrue(ObjectHelper.contains(array, "foo"));
        assertTrue(ObjectHelper.contains(collection, "foo"));
        assertTrue(ObjectHelper.contains("foo", "foo"));
        
        assertFalse(ObjectHelper.contains(array, "xyz"));
        assertFalse(ObjectHelper.contains(collection, "xyz"));
        assertFalse(ObjectHelper.contains("foo", "xyz"));
    }
    
    public void setCheese(String cheese) {
    }
}
