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
package com.anychart.gjstags.utils;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.jscomp.PublicNodeUtil;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aleksandr Batsuev (alex@batsuev.com)
 */
public class SourceCodeTraversal extends NodeTraversal.AbstractShallowCallback {

    private Compiler compiler;

    public Compiler getCompiler() {
        return this.compiler;
    }

    private Map<String, SourceCodeEntry> entries;

    public Map<String, SourceCodeEntry> getEntries() {
        return this.entries;
    }

    public List<SourceCodeEntry> getEntries(String prefix) {
        List<SourceCodeEntry> entries = new ArrayList<SourceCodeTraversal.SourceCodeEntry>();

        for (SourceCodeEntry entry : this.entries.values()) {
            if (entry.getName().startsWith(prefix) && !entry.getName().equals(prefix))
                entries.add(entry);
        }

        return entries;
    }

    private List<InheritanceInfo> classInheritanceInfos;

    public List<InheritanceInfo> getClassInheritanceInfos() {
        return this.classInheritanceInfos;
    }

    public SourceCodeTraversal() {
        this.entries = new HashMap<String, SourceCodeEntry>();
        this.classInheritanceInfos = new ArrayList<InheritanceInfo>();
    }

    public void visit(NodeTraversal nodeTraversal, Node node, Node parent) {
        if (node.getType() != Token.SCRIPT) return;
        this.compiler = (Compiler) nodeTraversal.getCompiler();

        for (Node child : node.children()) {
            if (child.getBooleanProp(Node.IS_NAMESPACE)) continue;

            switch (child.getType()) {
                case Token.VAR:
                    this.visitVar(nodeTraversal, child, node);
                    break;
                case Token.EXPR_RESULT:
                    if (child.getFirstChild() == null) continue;
                    if (child.getFirstChild().getType() == Token.ASSIGN) {
                        this.visitAssign(nodeTraversal, child.getFirstChild(), child);
                    } else if (child.getFirstChild().getType() == Token.CALL) {
                        if (child.getFirstChild().getFirstChild().getType() == Token.GETPROP &&
                                child.getFirstChild().getFirstChild().isQualifiedName() &&
                                child.getFirstChild().getFirstChild().getQualifiedName().equals("goog.inherits")) {

                            String className = child.getFirstChild().getChildAtIndex(1).getQualifiedName();
                            String baseClassName = child.getFirstChild().getLastChild().getQualifiedName();

                            classInheritanceInfos.add(new InheritanceInfo(baseClassName, className));
                        }
                    }
                    break;
            }
        }
    }

    private void visitAssign(NodeTraversal nodeTraversal, Node node, Node parent) {
        String name = node.getFirstChild().getQualifiedName();
        if (name == null) return;
        Node assignValue = node.getLastChild();
        this.visitValue(name, assignValue, node.getJSDocInfo());
    }

    private void visitVar(NodeTraversal nodeTraversal, Node node, Node parent) {
        String name = node.getFirstChild().getString();
        if (name == null) return;

        Node varValue = node.getFirstChild().getFirstChild();
        this.visitValue(name, varValue, node.getJSDocInfo());
    }

    private void visitValue(String name, Node valueNode, JSDocInfo info) {
        SourceCodeEntry entry = new SourceCodeEntry(name, valueNode, info);
        if (this.entries.containsKey(name))
            this.entries.remove(this.entries.get(name));

        this.entries.put(name, entry);

        if (valueNode != null) {
            if (valueNode.getType() == Token.OBJECTLIT) {
                if (name.contains(".DEFAULT_TEMPLATE")) return; //anychart-specific gag - ignore templates json

                for (Node child : valueNode.children()) {
                    String childName = PublicNodeUtil.getObjectLitKeyName(child);
                    this.visitValue(name + "." + childName, child.getFirstChild(), child.getJSDocInfo());
                }
            } else if (valueNode.getType() == Token.FUNCTION) {
                Node body = PublicNodeUtil.getFunctionBody(valueNode);
                this.visitFunctionBody(name, body);
            }
        }
    }

    private void visitValueFromFunction(String functionName, String propName, Node valueNode, JSDocInfo info) {
        if (functionName.contains(".prototype.")) return; //ignore fields from prototype functions bodies
        propName = functionName + ".__OBJDEF__." + propName.substring("this.".length());
        this.visitValue(propName, valueNode, info);
    }

    private void visitFunctionBody(String name, Node body) {
        if (name.contains(".__OBJDEF__.")) return;
        if (name.contains(".prototype.")) return;

        for (Node child : body.children()) {
            if (child.getType() != Token.EXPR_RESULT) continue;
            if (child.getFirstChild() == null) continue;
            if (child.getFirstChild().getType() != Token.ASSIGN) continue;
            if (!child.getFirstChild().getFirstChild().isQualifiedName()) continue;
            String targetName = child.getFirstChild().getFirstChild().getQualifiedName();
            if (targetName.startsWith("this.")) {
                this.visitValueFromFunction(name, targetName, child.getFirstChild().getLastChild(), child.getJSDocInfo());
            }
        }
    }

    public class SourceCodeEntry implements Comparable<SourceCodeEntry> {
        private String name;
        private Node value;
        private JSDocInfo jsDocInfo;

        public String getName() {
            return this.name;
        }

        public Node getValue() {
            return this.value;
        }

        public JSDocInfo getJsDocInfo() {
            return this.jsDocInfo;
        }

        public SourceCodeEntry(String name, Node value, JSDocInfo jsDocInfo) {
            this.name = name;
            this.value = value;
            this.jsDocInfo = jsDocInfo;
        }

        public int compareTo(SourceCodeEntry sourceCodeEntry) {
            return sourceCodeEntry.name.compareTo(this.name);
        }
    }

    private class InheritanceInfo {
        private String baseName;

        public String getBaseName() {
            return this.baseName;
        }

        private String name;

        public String getName() {
            return this.name;
        }

        public InheritanceInfo(String baseName, String name) {
            this.baseName = baseName;
            this.name = name;
        }
    }
}