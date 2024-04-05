/**
 * Copyright (c) 2003-2021 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.elfinder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.sakaiproject.exception.PermissionException;

public interface SakaiFsVolume {
    void createFile(SakaiFsItem fsi) throws IOException;

    void createFolder(SakaiFsItem fsi) throws IOException;

    void deleteFile(SakaiFsItem fsi) throws IOException;

    void deleteFolder(SakaiFsItem fsi) throws IOException;

    default boolean exists(SakaiFsItem newFile) {
        return false;
    }

    SakaiFsItem fromPath(String relativePath);

    default String getDimensions(SakaiFsItem fsi) {
        return null;
    }

    default long getLastModified(SakaiFsItem fsi) {
        return 0L;
    }

    String getMimeType(SakaiFsItem fsi);

    String getName();

    String getName(SakaiFsItem fsi);

    SakaiFsItem getParent(SakaiFsItem fsi);

    String getPath(SakaiFsItem fsi) throws IOException;

    SakaiFsItem getRoot();

    default long getSize(SakaiFsItem fsi) throws IOException {
        return 0;
    }

    default String getThumbnailFileName(SakaiFsItem fsi) {
        return null;
    }

    default boolean hasChildFolder(SakaiFsItem fsi) {
        return false;
    }

    boolean isFolder(SakaiFsItem fsi);

    default boolean isRoot(SakaiFsItem fsi) {
        return false;
    }

    SakaiFsItem[] listChildren(SakaiFsItem fsi) throws PermissionException;

    default InputStream openInputStream(SakaiFsItem fsi) throws IOException {
        return null;
    }

    void writeStream(SakaiFsItem f, InputStream is) throws IOException;

    void rename(SakaiFsItem src, SakaiFsItem dst) throws IOException;

    String getURL(SakaiFsItem f);

    void filterOptions(SakaiFsItem f, Map<String, Object> map);
}
