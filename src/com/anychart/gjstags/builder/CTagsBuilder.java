/**
 * Copyright 2011 AnyChart.Com Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anychart.gjstags.builder;

import com.anychart.gjstags.utils.SourceCodeTraversal;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.*;
import com.google.javascript.rhino.JSDocInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Aleksandr Batsuev (alex@batsuev.com)
 */
public class CTagsBuilder implements ITopLevelInfoProvider {

    private static Compiler compiler;
    private static CompilerOptions compilerOptions;

    private static void initCompiler() {
        compiler = new Compiler();
        compilerOptions = new CompilerOptions();
        compilerOptions.ideMode = true;
        compilerOptions.variableRenaming = VariableRenamingPolicy.OFF;
    }

    private SourceCodeTraversal sourceCodeTraversal;

    public CTagsBuilder() {
        if (CTagsBuilder.compiler == null)
            CTagsBuilder.initCompiler();
    }

    public void initAST(String[] paths) {
        List<JSSourceFile> sourceFiles = new ArrayList<JSSourceFile>();
        for (String path : paths)
            sourceFiles.add(JSSourceFile.fromFile(path));

        compiler.compile(Lists.<JSSourceFile>newArrayList(), sourceFiles, compilerOptions);

        sourceCodeTraversal = new SourceCodeTraversal();
        NodeTraversal.traverse(compiler, compiler.getRoot(), sourceCodeTraversal);
    }

    private List<CTagsEntry> tags;

    public CTagsEntry[] getTags() {
        return this.tags.toArray(new CTagsEntry[this.tags.size()]);
    }

    public void parseCTags() {
        this.classes = new ArrayList<String>();
        this.interfaces = new ArrayList<String>();
        this.enums = new ArrayList<String>();

        Map<String, SourceCodeTraversal.SourceCodeEntry> entries = sourceCodeTraversal.getEntries();

        this.tags = new ArrayList<CTagsEntry>();

        //first of all - extract classes, enums, interfaces and generate ctags for them
        for (Map.Entry<String, SourceCodeTraversal.SourceCodeEntry> entry : entries.entrySet()) {
            this.checkTopLevelEntry(this.tags, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, SourceCodeTraversal.SourceCodeEntry> entry : entries.entrySet()) {
            if (entry.getValue().getValue() == null) continue;
            CTagsEntry.generateFromEntry(this.tags, this, entry.getKey(), entry.getValue());
        }
    }

    private List<String> classes;
    private List<String> interfaces;
    private List<String> enums;

    public boolean isClass(String name) {
        return classes != null && classes.contains(name);
    }

    public boolean isInterface(String name) {
        return interfaces != null && interfaces.contains(name);
    }

    public boolean isEnum(String name) {
        return enums != null && enums.contains(name);
    }

    private void checkTopLevelEntry(List<CTagsEntry> tags, String key, SourceCodeTraversal.SourceCodeEntry entry) {
        if (entry.getValue() == null || entry.getJsDocInfo() == null) return;

        JSDocInfo info = entry.getJsDocInfo();

        if (info.isConstructor()) {
            classes.add(key);
        } else if (info.isInterface()) {
            interfaces.add(key);
        } else if (info.hasEnumParameterType()) {
            enums.add(key);
        }

        CTagsEntry.generateFromTopLevelEntry(tags, key, entry);
    }
}
