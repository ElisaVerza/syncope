/*
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

package org.apache.syncope.core.provisioning.api.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(value= Parameterized.class)
public class RealmUtilsTest{

    private String input;
    private List<String> tokenStr = new ArrayList<String>();
    private Pair<String, String> expectedPair;
    private Optional<Pair<String, String>> groupOwner;
    private String realmNames;
    private String newRealm;
    private boolean normalizingAddToRet;
    private Set<String> realmSet;
    private Set<String> spiedRealmSet;
    private boolean expectedBool;
    private Pair<Set<String>, Set<String>> normalizeRet;
    private Pair<Set<String>, Set<String>> normalizeExpected;

    @Parameters
    public static Collection<Object[]> getTestParameters() {
        return Arrays.asList(new Object[][]{
                {"", "RealmString1,StringForRealm", "Realm", true},
                {"inputTest", "RealmString1@,StringForRealm", "RealmString1@New", false},
                {"input@Test", ",", "", true},
                {"input@", null, "test", true},
        });
    }

    public RealmUtilsTest(String input, String realmNames, String newRealm, boolean expectedBool){
        this.input = input;
        this.realmNames = realmNames;
        this.newRealm = newRealm;
        this.expectedBool = expectedBool;
    }

    @Before
    public void setupNormalizeAddTo(){
        Set<String> firstSet = new HashSet<>();
        Set<String> secondSet = new HashSet<>();
        if(realmNames!=null){
            realmSet = Collections.list(new StringTokenizer(realmNames, ",")).stream().map(token -> (String) token).collect(Collectors.toSet());
            spiedRealmSet = spy(realmSet);
            normalizingAddToRet = RealmUtils.normalizingAddTo(spiedRealmSet, newRealm);

            for(String s:realmSet){
                if(s.contains("@")){
                    secondSet.add(s);
                } else{
                    firstSet.add(s);
                }
            }
        }
        normalizeRet = RealmUtils.normalize(realmSet);
        normalizeExpected = Pair.of(firstSet, secondSet);
    }

    @Before
    public void setupParseGroupOwnerRealm(){
        groupOwner = RealmUtils.parseGroupOwnerRealm(input);
        if(input!=null & input.contains("@")){
            tokenStr = Collections.list(new StringTokenizer(input, "@")).stream()
                    .map(token -> (String) token)
                    .collect(Collectors.toList());
        }
        if(tokenStr.size()>=2){
            expectedPair = Pair.of(tokenStr.get(0), tokenStr.get(1));
        }
        else{
            expectedPair = null;
        }
    }

    @Test
    public void normalizingTest(){
        if(realmNames!=null){
            verify(spiedRealmSet, times(1)).removeAll(anySet());
            assertEquals(expectedBool, normalizingAddToRet);
        }
        assertEquals(normalizeExpected, normalizeRet);
    }
    @Test
    public void parseGroupOwnerRealmTest(){
        if(groupOwner.isPresent()){
            assertEquals(expectedPair, groupOwner.get());
        }
        else{
            assertEquals(Optional.empty(), groupOwner);
        }
    }
}