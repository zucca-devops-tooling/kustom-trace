/*
 * Copyright 2025 GuidoZuccarelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

import java.io.File;

public class KustomTraceCLIFactory implements IFactory {

    private final File appsDir;

    public KustomTraceCLIFactory(File appsDir) {
        this.appsDir = appsDir;
    }

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        if (cls == AffectedAppsCommand.class) {
            AffectedAppsCommand command = new AffectedAppsCommand();
            command.setAppsDir(appsDir);
            return (K) command;
        } else if (cls == AppFilesCommand.class) {
            AppFilesCommand command = new AppFilesCommand();
            command.setAppsDir(appsDir);
            return (K) command;
        }
        // For other commands, use the default factory
        return CommandLine.defaultFactory().create(cls);
    }
}