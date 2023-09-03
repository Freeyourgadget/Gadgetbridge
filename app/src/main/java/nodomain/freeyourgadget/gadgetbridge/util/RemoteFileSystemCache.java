/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, João
    Paulo Barraca, JohnnySun

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package nodomain.freeyourgadget.gadgetbridge.util;

import java.util.ArrayList;
import java.util.List;

public class RemoteFileSystemCache {

    private Directory root;

    public RemoteFileSystemCache() {
        root = new Directory("/");
    }

    public void addFile(String full_path) {
        root.addFile(full_path.substring(1)); // skip first slash
    }

    public void addDirectory(String full_path) {
        root.addDirectory(full_path.substring(1)); // skip first slash
    }
    public boolean hasFile(String full_path) {
        return root.hasFile(full_path);
    }
    private class Directory {
        String name;
        List<String> files;
        List<Directory> directories;

        Directory(String name) {
            files = new ArrayList<>();
            directories = new ArrayList<>();
            this.name = name;
        }

        void addFile(String filename) {
            int indexOfSlash = filename.indexOf('/');
            if (indexOfSlash > 0) {
                Directory nextDirectory = getDirectory(filename.substring(0, indexOfSlash)).get();
                nextDirectory.addFile(filename.substring(indexOfSlash + 1));
            } else {
                files.add(filename);
            }
        }

        void addDirectory(String name) {
            int indexOfSlash = name.indexOf('/');
            if (indexOfSlash > 0) {
                Directory nextDirectory = getDirectory(name.substring(0, indexOfSlash)).get();
                nextDirectory.addFile(name.substring(indexOfSlash + 1));
            } else {
                directories.add(new Directory(name));
            }
        }

        boolean hasFile(String full_path) {
            // Remove leading slash, it may be left if we are root
            String path;
            if (full_path.charAt(0) == '/') {
                path = full_path.substring(1);
            } else {
                path = full_path;
            }
            int indexOfSlash = path.indexOf('/');
            if (indexOfSlash >= 0) {
                Optional<Directory> nextDirectory = getDirectory(path.substring(0, indexOfSlash));
                if (nextDirectory.isPresent()) {
                    String remains = path.substring(indexOfSlash  + 1); // Skip past slash
                    return nextDirectory.get().hasFile(remains);
                }
                return false;
            } else {
                return files.contains(path);
            }
        }

        private Optional<Directory> getDirectory(String name) {
            for(Directory directory: directories) {
                if (directory.name == name) {
                    return Optional.of(directory);
                }
            }
            return null;
        }
    }
}
