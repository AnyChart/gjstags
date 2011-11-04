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
package com.anychart.gjstags;

import com.anychart.gjstags.builder.CTagsBuilder;
import com.anychart.gjstags.ctags.CTagsFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aleksandr Batsuev (alex@batsuev.com)
 */
public class CommandLineRunner {

    public static final String VERSION = "1.2";

    private static final String ERROR_NO_OUTPUT = "Error: No output file specified";
    private static final String ERROR_NO_INPUT = "Error: No valid inputs specified";

    private static final String LICENSE = "Copyright 2011 AnyChart.Com Team\n" +
            "\n" +
            "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
            "you may not use this file except in compliance with the License.\n" +
            "You may obtain a copy of the License at\n" +
            "\n" +
            "     http://www.apache.org/licenses/LICENSE-2.0\n" +
            "\n" +
            "Unless required by applicable law or agreed to in writing, software\n" +
            "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
            "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
            "See the License for the specific language governing permissions and\n" +
            "limitations under the License.\n";

    private static final String HELP = "gjstags " + VERSION + ", Copyright (C) 2011 AnyChart.Com Team\n" +
            "Author: Aleksandr Batsuev (alex@batsuev.com)\n" +
            "\n" +
            "Usage: gjstags [options] [file(s)]\n" +
            "\n" +
            "Options:\n" +
            "  -a Append the tags to an existing tag file. Disabled by default. \n" +
            "  -f <file>\n" +
            "     Write tags to specified file. 'tags' by default.\n" +
            "  -o Alias for -f\n" +
            "  -r Find source files recursively.\n" +
            "  -R alias for -r\n" +
            "  -b base dir\n" +
            "  --recursive Alias for -r\n" +
            "  --basedir Alias for -b\n" +
            "  --version Show version info.\n" +
            "  --license Show license.\n" +
            "  --help Show this help.\n";

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.out.print(HELP);
            return;
        }

        boolean isRecursive = false;
        String outputFile = "tags";
        boolean appendTags = false;
        List<String> inputs = new ArrayList<String>();
        String baseDir = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--help")) {
                System.out.print(HELP);
                return;
            } else if (arg.equals("--version")) {
                System.out.println("gjstags " + VERSION + ", Copyright (C) 2011 AnyChart.Com Team\n");
                return;
            } else if (arg.equals("--license")) {
                System.out.print(LICENSE);
            } else if (arg.equals("-R") || arg.equals("-r") || arg.equals("--recursive")) {
                isRecursive = true;
            } else if (arg.equals("-f") || arg.equals("-o")) {
                if (i == (args.length - 1)) {
                    System.out.println(ERROR_NO_OUTPUT);
                    return;
                }
                outputFile = args[++i];
            } else if (arg.equals("-a")) {
                appendTags = true;
            } else if (arg.equals("-b") || arg.equals("--basedir")) {
                if (i < (args.length - 1)) {
                    baseDir = args[++i];
                }
            } else {
                inputs.add(arg);
            }
        }

        ArrayList<String> realInputFiles = new ArrayList<String>();

        if (isRecursive) {
            for (String input : inputs) {
                File f = new File(input);
                if (f.isDirectory()) {
                    realInputFiles.addAll(getJSFilesFromDir(input));
                } else if (f.isFile()) {
                    realInputFiles.add(input);
                }
            }
        }else {
            for (String input : inputs) {
                File f = new File(input);
                if (f.isFile() && f.exists()) {
                    realInputFiles.add(input);
                }
            }
        }

        if (realInputFiles.size() == 0) {
            System.out.println(ERROR_NO_INPUT);
            return;
        }


        CTagsBuilder cTagsBuilder = new CTagsBuilder();
        cTagsBuilder.initAST(realInputFiles.toArray(new String[realInputFiles.size()]));
        cTagsBuilder.parseCTags(baseDir);

        CTagsFile file;
        if (appendTags && new File(outputFile).exists()) {
            file = CTagsFile.fromFile(outputFile);
        } else {
            file = new CTagsFile();
        }

        file.update(cTagsBuilder.getTags());
        file.write(outputFile);
    }

    private static List<String> getJSFilesFromDir(String path) {
        List<String> res = new ArrayList<String>();
        File dir = new File(path);
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                res.addAll(getJSFilesFromDir(child.getPath()));
            } else if (child.isFile()) {
                String childPath = child.getPath();
                if (!childPath.contains(".")) continue;
                int index = childPath.lastIndexOf(".");
                String extension = childPath.substring(index);
                if (extension.toLowerCase().equals(".js")) {
                    res.add(childPath);
                }
            }
        }
        return res;
    }
}
