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

import com.anychart.gjstags.ctags.CTag;
import com.anychart.gjstags.utils.SourceCodeTraversal;
import com.google.javascript.jscomp.NodeUtil;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.Token;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aleksandr Batsuev (alex@batsuev.com)
 */
public class CTagsEntry extends CTag {

    public enum CTagsEntryKind {
        CLASS,
        INTERFACE,
        ENUM,
        METHOD,
        PROPERTY,
        STATIC_METHOD,
        STATIC_PROPERTY,
        ENUM_ENTRY,
        DEFINE;

        public String toString() {
            switch (this) {
                case CLASS:
                    return "c";
                case INTERFACE:
                    return "i";
                case ENUM:
                    return "g";
                case METHOD:
                    return "m";
                case PROPERTY:
                    return "f";
                case STATIC_PROPERTY:
                    return "f";
                case STATIC_METHOD:
                    return "p";
                case ENUM_ENTRY:
                    return "e";
                case DEFINE:
                    return "d";
            }
            return null;
        }
    }

    private String packageName;
    private CTagsEntryKind kind;
    private String className;
    private String enumName;

    public String getMeta() {
        String meta = ";\"";
        if (this.kind != null) meta += "\tkind:" + this.kind.toString();
        if (this.packageName != null) meta += "\tpackage:" + this.packageName;
        if (this.className != null) meta += "\tclass:" + this.className;
        if (this.enumName != null) meta += "\tenum:" + this.enumName;
        return meta;
    }

    public static void generateFromEntry(List<CTagsEntry> tags, ITopLevelInfoProvider infoProvider, String name, SourceCodeTraversal.SourceCodeEntry entry, String baseDir) {

        CTagsEntryKind kind;
        String className = null;
        String enumName = null;
        String packageName = null;

        String fileName = NodeUtil.getSourceName(entry.getValue());
        if (baseDir != null) {
            String absPath = new File(fileName).getAbsolutePath();
            String absBaseDir = new File(baseDir).getAbsolutePath();
            if (absPath.contains(absBaseDir)) {
                fileName = absPath.substring(absPath.indexOf(absBaseDir) + absBaseDir.length()+1);
            }
        }

        int lineNumber = entry.getValue().getLineno();

        //class or interface method/prop
        if (name.contains(".prototype.") || name.contains(".__OBJLIT__.")) {
            if (name.contains(".prototype.")) {
                className = name.substring(0, name.indexOf(".prototype."));
            } else {
                className = name.substring(0, name.indexOf(".__OBJLIT__."));
            }
            packageName = getPackage(className);

            if (entry.getValue().getType() == Token.FUNCTION)
                kind = CTagsEntryKind.METHOD;
            else
                kind = CTagsEntryKind.PROPERTY;

        } else {
            JSDocInfo info = entry.getJsDocInfo();
            if (info != null &&
                    (info.isConstructor() ||
                            info.isInterface() ||
                            info.hasEnumParameterType()))
                return;

            String containerName = getPackage(name);

            //enum entry
            if (infoProvider.isEnum(containerName)) {
                kind = CTagsEntryKind.ENUM_ENTRY;
                enumName = containerName;
                //class entry
            } else if (infoProvider.isClass(containerName)) {
                className = containerName;
                if (entry.getValue().getType() == Token.FUNCTION)
                    kind = CTagsEntryKind.STATIC_METHOD;
                else
                    kind = CTagsEntryKind.STATIC_PROPERTY;
            } else if (info != null && info.isDefine()) {
                kind = CTagsEntryKind.DEFINE;
            } else {
                //package static method or prop
                if (entry.getValue().getType() == Token.FUNCTION)
                    kind = CTagsEntryKind.STATIC_METHOD;
                else
                    kind = CTagsEntryKind.STATIC_PROPERTY;
                packageName = containerName;
            }
        }

        for (String tagName : getNameVariations(name)) {
            CTagsEntry tag = new CTagsEntry();
            tag.file = fileName;
            tag.address = String.valueOf(lineNumber);
            tag.packageName = packageName;
            tag.className = className;
            tag.enumName = enumName;
            tag.kind = kind;
            tag.name = tagName;
            tags.add(tag);
        }
    }

    public static void generateFromTopLevelEntry(List<CTagsEntry> tags, String name, SourceCodeTraversal.SourceCodeEntry entry) {
        JSDocInfo info = entry.getJsDocInfo();
        if (info == null || entry.getValue() == null) return;

        CTagsEntryKind kind = null;
        if (info.isConstructor())
            kind = CTagsEntryKind.CLASS;
        else if (info.isInterface())
            kind = CTagsEntryKind.INTERFACE;
        else if (info.hasEnumParameterType())
            kind = CTagsEntryKind.ENUM;

        if (kind == null) return;

        String fileName = NodeUtil.getSourceName(entry.getValue());
        int lineNumber = entry.getValue().getLineno();
        String packageName = getPackage(name);

        for (String tagName : getNameVariations(name)) {
            CTagsEntry tag = new CTagsEntry();
            tag.file = fileName;
            tag.address = String.valueOf(lineNumber);
            tag.packageName = packageName;
            tag.kind = kind;
            tag.name = tagName;
            tags.add(tag);
        }
    }

    private static String getPackage(String name) {
        if (name.contains("."))
            return name.substring(0, name.lastIndexOf("."));
        return "";
    }

    private static List<String> getNameVariations(String name) {
        List<String> res = new ArrayList<String>();

        name = name.replace("prototype.", "");
        name = name.replace("__OBJDEF__.", "");

        res.add(name);

        if (!name.contains(".")) return res;

        String packageName = name;
        int index = packageName.indexOf(".");
        while (packageName.contains(".")) {
            packageName = packageName.substring(index + 1);
            index = packageName.indexOf(".");
            if (!packageName.startsWith("prototype.") &&
                    !packageName.startsWith("__OBJDEF__.")) res.add(packageName);
        }

        return res;
    }
}
