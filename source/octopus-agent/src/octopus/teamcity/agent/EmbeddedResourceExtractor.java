/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package octopus.teamcity.agent;

import java.io.*;
import java.lang.*;

public class EmbeddedResourceExtractor {
    public void extractTo(String destinationPath) throws Exception {
        ensureDirectory(destinationPath, "1.0");
        extractFile("/resources/1/0/octo.exe", destinationPath + "\\1.0\\Octo.exe");
        extractFile("/resources/1/0/Octo.exe.config", destinationPath + "\\1.0\\Octo.exe.config");

        ensureDirectory(destinationPath, "2.0");
        extractFile("/resources/2/0/Octo.exe", destinationPath + "\\2.0\\Octo.exe");
        extractFile("/resources/2/0/Octo.exe.config", destinationPath + "\\2.0\\Octo.exe.config");

        ensureDirectory(destinationPath, "3.0");
        extractFile("/resources/3/0/Octo.exe", destinationPath + "\\3.0\\Octo.exe");
        extractFile("/resources/3/0/Octo.exe.config", destinationPath + "\\3.0\\Octo.exe.config");
	}

    public void extractNugetTo(String destinationPath) throws Exception {
        extractFile("/resources/nuget.exe", destinationPath + "\\nuget.exe");
    }

    private void extractFile(String resourceName, String destinationName) throws Exception {
        int attempts = 0;
        while (true) {
            attempts++;

            try {
                File file = new File(destinationName);
                if (file.exists())
                    return;

                InputStream is = getClass().getResourceAsStream(resourceName);
                OutputStream os = new FileOutputStream(destinationName, false);

                byte[] buffer = new byte[4096];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }

                os.close();
                is.close();
            }
            catch (Exception ex) {
                Thread.sleep(4000);
                if (attempts > 3) {
                    throw ex;
                }
            }
        }
    }

    private void ensureDirectory(String destinationPath, String version) {
        File extractedTo = new File(destinationPath, version);
        if (extractedTo.exists())
            return;

        if (!extractedTo.mkdirs())
            throw new RuntimeException("Unable to create temp output directory " + extractedTo);
    }
}
