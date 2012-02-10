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
package com.anychart.gjstags.ctags;

import java.io.Writer;
import java.io.IOException;

/**
 * @author Aleksandr Batsuev (alex@batsuev.com)
 */
public class CTag {
    protected String name;
    protected String file;
    protected String address;
    protected String meta;

    public String getName() {
        return name;
    }

    public String getFile() {
        return file;
    }

    public String getAddress() {
        return address;
    }

    public String getMeta() {
        return meta;
    }

    public String getCTagString() {
        return this.getName() + "\t" + this.getFile() + "\t" + this.getAddress() + this.getMeta() + "\n";

    public void writeCTagString(Writer writer) throws IOException {
        writer.write(this.getName());
        writer.write('\t');
        writer.write(this.getFile());
        writer.write('\t');
        writer.write(this.getAddress());
        writer.write(this.getMeta());
        writer.write('\n');
    }

    public static CTag fromString(String line) {

        String tag = line;
        String meta = "";
        if (tag.contains(";\"\t")) {
            tag = line.substring(0, tag.indexOf(";\"\t"));
            meta = line.substring(tag.indexOf(";\"\t"));
        }

        String[] entries = tag.split("\t");
        if (entries.length == 0) return null;
        String name = entries[0];
        if (name.startsWith("!")) return null;
        String fileName = entries[1];
        String address = entries[2];

        CTag res = new CTag();
        res.name = name;
        res.file = fileName;
        res.address = address;
        res.meta = meta;
        return res;
    }
}
