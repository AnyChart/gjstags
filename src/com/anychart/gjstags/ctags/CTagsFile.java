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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Aleksandr Batsuev (alex@batsuev.com)
 */
public class CTagsFile {

    private static final String HEADER =
            "!_TAG_FILE_FORMAT\t2\t/extended format; --format=1 will not append ;\" to lines/\n" +
                    "!_TAG_FILE_SORTED\t0\t/0=unsorted, 1=sorted, 2=foldcase/\n" +
                    "!_TAG_PROGRAM_AUTHOR\tAnyChart.Com Team\n" +
                    "!_TAG_PROGRAM_NAME\tgjstags\t//\n" +
                    "!_TAG_PROGRAM_URL\thttps://github.com/AnyChart/gjstags\t/official site/\n" +
                    "!_TAG_PROGRAM_VERSION\t1.0\t//\n";

    private List<CTag> entries;

    public CTagsFile() {
        this.entries = new ArrayList<CTag>();
    }

    public void update(CTag[] newEntries) {
        //remove all old entries with specific file names
        for (CTag tag : newEntries) {
            String fileName = tag.getFile();
            for (CTag currentTag : this.entries) {
                if (currentTag.getFile().equals(fileName)) {
                    this.entries.remove(currentTag);
                }
            }
        }

        Collections.addAll(this.entries, newEntries);
    }

    public void write(String fileName) throws IOException {
        FileWriter writer = new FileWriter(fileName);
        String tagsString = HEADER;
        for (CTag tag : this.entries) {
            tagsString += tag.getCTagString();
        }
        writer.write(tagsString);
        writer.close();
    }

    public static CTagsFile fromFile(String fileName) throws IOException {
        CTagsFile file = new CTagsFile();
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        while (line != null) {
            CTag tag = CTag.fromString(line);
            if (tag != null)
                file.entries.add(tag);
            line = reader.readLine();
        }
        return file;
    }
}
