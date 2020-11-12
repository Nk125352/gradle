/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.internal.snapshot;

import org.gradle.internal.file.FileMetadata.AccessType;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.hash.Hasher;
import org.gradle.internal.hash.Hashing;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.gradle.internal.snapshot.MerkleDirectorySnapshotBuilder.EmptyDirectoryHandlingStrategy.EXCLUDE_EMPTY_DIRS;

public class MerkleDirectorySnapshotBuilder {
    private static final HashCode DIR_SIGNATURE = Hashing.signature("DIR");

    private final Deque<Level> stack = new ArrayDeque<>();
    private final boolean sortingRequired;

    public static MerkleDirectorySnapshotBuilder sortingRequired() {
        return new MerkleDirectorySnapshotBuilder(true);
    }

    public static MerkleDirectorySnapshotBuilder noSortingRequired() {
        return new MerkleDirectorySnapshotBuilder(false);
    }

    private MerkleDirectorySnapshotBuilder(boolean sortingRequired) {
        this.sortingRequired = sortingRequired;
        stack.addLast(new Result());
    }

    public void preVisitDirectory(CompleteDirectorySnapshot directorySnapshot, EmptyDirectoryHandlingStrategy emptyDirectoryHandlingStrategy) {
        preVisitDirectory(directorySnapshot.getAccessType(), directorySnapshot.getAbsolutePath(), directorySnapshot.getName(), emptyDirectoryHandlingStrategy);
    }

    public void preVisitDirectory(AccessType accessType, String absolutePath, String name, EmptyDirectoryHandlingStrategy emptyDirectoryHandlingStrategy) {
        stack.addLast(new Directory(accessType, absolutePath, name, emptyDirectoryHandlingStrategy));
    }

    public void visitLeafElement(FileSystemLeafSnapshot snapshot) {
        collectEntry(snapshot);
    }

    public boolean postVisitDirectory() {
        CompleteFileSystemLocationSnapshot snapshot = stack.removeLast().fold();
        if (snapshot == null) {
            return false;
        }
        collectEntry(snapshot);
        return true;
    }

    private void collectEntry(CompleteFileSystemLocationSnapshot snapshot) {
        Level level = stack.peekLast();
        if (level == null) {
            throw new IllegalStateException("Outside of root");
        }
        level.collectEntry(snapshot);
    }

    @Nullable
    public CompleteFileSystemLocationSnapshot getResult() {
        assert stack.size() == 1;
        return stack.getLast().fold();
    }

    public enum EmptyDirectoryHandlingStrategy {
        INCLUDE_EMPTY_DIRS,
        EXCLUDE_EMPTY_DIRS
    }

    private interface Level {
        void collectEntry(CompleteFileSystemLocationSnapshot snapshot);

        @Nullable
        CompleteFileSystemLocationSnapshot fold();
    }

    private static class Result implements Level {
        private CompleteFileSystemLocationSnapshot result;

        @Override
        public void collectEntry(CompleteFileSystemLocationSnapshot snapshot) {
            assert result == null;
            result = snapshot;
        }

        @Override
        public CompleteFileSystemLocationSnapshot fold() {
            return result;
        }
    }

    private class Directory implements Level {
        private final AccessType accessType;
        private final String absolutePath;
        private final String name;
        private final List<CompleteFileSystemLocationSnapshot> children;
        private final EmptyDirectoryHandlingStrategy emptyDirectoryHandlingStrategy;

        public Directory(AccessType accessType, String absolutePath, String name, EmptyDirectoryHandlingStrategy emptyDirectoryHandlingStrategy) {
            this.accessType = accessType;
            this.absolutePath = absolutePath;
            this.name = name;
            this.children = new ArrayList<>();
            this.emptyDirectoryHandlingStrategy = emptyDirectoryHandlingStrategy;
        }

        @Override
        public void collectEntry(CompleteFileSystemLocationSnapshot snapshot) {
            children.add(snapshot);
        }

        @Override
        public CompleteDirectorySnapshot fold() {
            if (emptyDirectoryHandlingStrategy == EXCLUDE_EMPTY_DIRS && children.isEmpty()) {
                return null;
            }
            if (sortingRequired) {
                children.sort(CompleteFileSystemLocationSnapshot.BY_NAME);
            }
            Hasher hasher = Hashing.newHasher();
            hasher.putHash(DIR_SIGNATURE);
            for (CompleteFileSystemLocationSnapshot child : children) {
                hasher.putString(child.getName());
                hasher.putHash(child.getHash());
            }
            return new CompleteDirectorySnapshot(absolutePath, name, accessType, hasher.hash(), children);
        }
    }
}
