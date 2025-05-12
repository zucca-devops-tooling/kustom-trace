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
package model;

import java.util.Optional;

public class KustomResource {

    private String name;
    private String kind;

    private KustomFile file;

    public KustomResource() {
    }

    public KustomResource(String name, String kind, KustomFile file) {
        this.name = name;
        this.kind = kind;
        this.file = file;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFile(KustomFile file) {
        this.file = file;
    }

    public String getDisplayName() {
        return "%s %s %s".formatted(getKind(), getName(), file.getPath().toString());
    }

    public String getName() {
        return Optional.ofNullable(name).orElse("undefined");
    }

    public KustomFile getFile() {
        return file;
    }
}
