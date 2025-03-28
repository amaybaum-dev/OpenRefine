/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.operations.cell;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.DecoratedValue;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.facets.ListFacet.ListFacetConfig;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.MetaParser;
import com.google.refine.grel.Parser;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.cell.MassEditOperation.Edit;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class MassOperationTests extends RefineTest {

    private List<Edit> editList;
    private String editsString;

    @BeforeMethod
    public void registerGRELParser() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @AfterMethod
    public void unregisterGRELParser() {
        MetaParser.unregisterLanguageParser("grel");
    }

    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "mass-edit", MassEditOperation.class);
    }

    @Test
    public void serializeMassEditOperation() throws Exception {
        String json = "{\"op\":\"core/mass-edit\","
                + "\"description\":" + new TextNode(OperationDescription.cell_mass_edit_brief("my column")).toString() + ","
                + "\"engineConfig\":{\"mode\":\"record-based\",\"facets\":[]},"
                + "\"columnName\":\"my column\",\"expression\":\"value\","
                + "\"edits\":[{\"fromBlank\":false,\"fromError\":false,\"from\":[\"String\"],\"to\":\"newString\"}]}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, MassEditOperation.class), json);
    }

    @Test
    public void testReconstructEditString() throws Exception {
        editsString = "[{\"from\":[\"String\"],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.size(), 1);
        Assert.assertEquals(editList.get(0).from.get(0), "String");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditMultiString() throws Exception {
        editsString = "[{\"from\":[\"String1\",\"String2\"],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.size(), 2);
        Assert.assertEquals(editList.get(0).from.get(0), "String1");
        Assert.assertEquals(editList.get(0).from.get(1), "String2");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditBoolean() throws Exception {
        editsString = "[{\"from\":[true],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.size(), 1);
        Assert.assertEquals(editList.get(0).from.get(0), "true");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditNumber() throws Exception {
        editsString = "[{\"from\":[1],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.size(), 1);
        Assert.assertEquals(editList.get(0).from.get(0), "1");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditDate() throws Exception {
        editsString = "[{\"from\":[\"2018-10-04T00:00:00Z\"],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.get(0), "2018-10-04T00:00:00Z");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditEmpty() throws Exception {
        editsString = "[{\"from\":[\"\"],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.size(), 1);
        Assert.assertEquals(editList.get(0).from.get(0), "");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertTrue(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);

    }

    @Test
    public void testValidate() {
        assertThrows(IllegalArgumentException.class,
                () -> new MassEditOperation(invalidEngineConfig, "foo", "grel:value", editsWithFromBlank).validate());
        assertThrows(IllegalArgumentException.class,
                () -> new MassEditOperation(defaultEngineConfig, null, "grel:value", editsWithFromBlank).validate());
        assertThrows(IllegalArgumentException.class,
                () -> new MassEditOperation(defaultEngineConfig, "foo", "grel:invalid(", editsWithFromBlank).validate());
        assertThrows(IllegalArgumentException.class,
                () -> new MassEditOperation(defaultEngineConfig, "foo", "grel:value", null).validate());
    }

    @Test
    public void testColumnsDiff() {
        assertEquals(new MassEditOperation(defaultEngineConfig, "foo", "grel:value", editsWithFromBlank).getColumnsDiff().get(),
                ColumnsDiff.modifySingleColumn("foo"));
    }

    @Test
    public void testColumnsDependencies() {
        assertEquals(new MassEditOperation(engineConfigWithColumnDeps, "foo", "grel:cells['bar'].value", editsWithFromBlank)
                .getColumnDependencies().get(), Set.of("foo", "bar", "facet_1"));
    }

    @Test
    public void testRename() {
        var SUT = new MassEditOperation(defaultEngineConfig, "foo", "grel:cells['bar'].value", editsWithFromBlank);
        AbstractOperation renamed = SUT.renameColumns(Map.of("bar", "bar2", "unrelated", "unrelated2"));

        String expectedJSON = "{\n"
                + "  \"columnName\" : \"foo\",\n"
                + "  \"description\" : " + new TextNode(OperationDescription.cell_mass_edit_brief("foo")).toString() + ",\n"
                + "  \"edits\" : [ {\n"
                + "    \"from\" : [ \"v1\" ],\n"
                + "    \"fromBlank\" : false,\n"
                + "    \"fromError\" : false,\n"
                + "    \"to\" : \"v2\"\n"
                + "  }, {\n"
                + "    \"from\" : [ ],\n"
                + "    \"fromBlank\" : true,\n"
                + "    \"fromError\" : false,\n"
                + "    \"to\" : \"hey\"\n"
                + "  } ],\n"
                + "  \"engineConfig\" : {\n"
                + "    \"facets\" : [ ],\n"
                + "    \"mode\" : \"row-based\"\n"
                + "  },\n"
                + "  \"expression\" : \"grel:cells.get(\\\"bar2\\\").value\",\n"
                + "  \"op\": \"core/mass-edit\""
                + "}";
        TestUtils.isSerializedTo(renamed, expectedJSON);
    }

    // Not yet testing for mass edit from OR Error

    private Project project;
    private static EngineConfig engineConfig;
    private ListFacetConfig facet;
    private List<Edit> edits;
    private List<Edit> editsWithFromBlank;

    @BeforeMethod
    public void setUpInitialState() {
        project = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "v1", "a" },
                        { "v3", "a" },
                        { "", "a" },
                        { "", "b" },
                        { new EvalError("error"), "a" },
                        { "v1", "b" }
                });
        facet = new ListFacetConfig();
        facet.columnName = "bar";
        facet.name = "bar";
        facet.expression = "grel:value";
        facet.selection = Collections.singletonList(new DecoratedValue("a", "a"));
        engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
        edits = Collections.singletonList(new Edit(Collections.singletonList("v1"), false, false, "v2"));
        editsWithFromBlank = Arrays.asList(edits.get(0), new Edit(Collections.emptyList(), true, false, "hey"));
    }

    @Test
    public void testSimpleReplace() throws Exception {
        MassEditOperation operation = new MassEditOperation(engineConfig, "foo", "grel:value", editsWithFromBlank);

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "v2", "a" },
                        { "v3", "a" },
                        { "hey", "a" },
                        { "", "b" },
                        { new EvalError("error"), "a" },
                        { "v1", "b" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testRecordsMode() throws Exception {
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RecordBased);
        MassEditOperation operation = new MassEditOperation(engineConfig, "foo", "grel:value", editsWithFromBlank);

        runOperation(operation, project);

        Project expected = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "v2", "a" },
                        { "v3", "a" },
                        { "hey", "a" },
                        { "hey", "b" },
                        { new EvalError("error"), "a" },
                        { "v1", "b" }
                });
        assertProjectEquals(project, expected);
    }
}
