/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.ant;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;
import org.apache.tools.ant.types.Resource;

/**
 * Creates an ant fileset consisting in all artifacts found during a resolve. Note that this task is
 * not compatible with the useOrigin mode.
 */
public class IvyCacheFileset extends IvyCacheTask {
    private String setid;

    public String getSetid() {
        return setid;
    }

    public void setSetid(String id) {
        setid = id;
    }

    public void setUseOrigin(boolean useOrigin) {
        if (useOrigin) {
            throw new UnsupportedOperationException(
                    "the cachefileset task does not support the useOrigin mode, since filesets require to have only one root directory. Please use the the cachepath task instead");
        }
    }

    public void doExecute() throws BuildException {
        prepareAndCheck();
        if (setid == null) {
            throw new BuildException("setid is required in ivy cachefileset");
        }
        try {
            List<ArtifactDownloadReport> paths = getArtifactReports();
            File base = null;
            for (ArtifactDownloadReport artifactDownloadReport : paths) {
                if (artifactDownloadReport.getLocalFile() != null) {
                    base = getBaseDir(base, artifactDownloadReport.getLocalFile());
                }
            }

            FileSet fileset;
            if (base == null) {
                fileset = new EmptyFileSet();
            } else {
                fileset = new FileSet();
                fileset.setDir(base);
                for (ArtifactDownloadReport artifactDownloadReport : paths) {
                    if (artifactDownloadReport.getLocalFile() != null) {
                        NameEntry ne = fileset.createInclude();
                        ne.setName(getPath(base, artifactDownloadReport.getLocalFile()));
                    }
                }
            }

            fileset.setProject(getProject());
            getProject().addReference(setid, fileset);
        } catch (Exception ex) {
            throw new BuildException("impossible to build ivy cache fileset: " + ex, ex);
        }
    }

    /**
     * Returns the path of the file relative to the given base directory.
     * 
     * @param base
     *            the parent directory to which the file must be evaluated.
     * @param file
     *            the file for which the path should be returned
     * @return the path of the file relative to the given base directory.
     */
    private String getPath(File base, File file) {
        String absoluteBasePath = base.getAbsolutePath();

        int beginIndex = absoluteBasePath.length();

        // checks if the basePath ends with the file separator (which can for instance
        // happen if the basePath is the root on unix)
        if (!absoluteBasePath.endsWith(File.separator)) {
            beginIndex++; // skip the separator char as well
        }

        return file.getAbsolutePath().substring(beginIndex);
    }

    /**
     * Returns the common base directory between a current base directory and a given file.
     * <p>
     * The returned base directory must be a parent of both the current base and the given file.
     * </p>
     * 
     * @param base
     *            the current base directory, may be null.
     * @param file
     *            the file for which the new base directory should be returned.
     * @return the common base directory between a current base directory and a given file.
     */
    File getBaseDir(File base, File file) {
        if (base == null) {
            return file.getParentFile().getAbsoluteFile();
        }
        Iterator<File> bases = getParents(base).iterator();
        Iterator<File> fileParents = getParents(file.getAbsoluteFile()).iterator();
        File result = null;
        while (bases.hasNext() && fileParents.hasNext()) {
            File next = bases.next();
            if (next.equals(fileParents.next())) {
                result = next;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * @return a list of files, starting with the root and ending with the file itself
     */
    private List<File> getParents(File file) {
        LinkedList<File> r = new LinkedList<File>();
        while (file != null) {
            r.addFirst(file);
            file = file.getParentFile();
        }
        return r;
    }

    private static class EmptyFileSet extends FileSet {

        private DirectoryScanner ds = new EmptyDirectoryScanner();

        public Iterator<Resource> iterator() {
            return new EmptyIterator<Resource>();
        }

        public Object clone() {
            return new EmptyFileSet();
        }

        public int size() {
            return 0;
        }

        public DirectoryScanner getDirectoryScanner(Project project) {
            return ds;
        }
    }

    private static class EmptyIterator<T> implements Iterator<T> {

        public boolean hasNext() {
            return false;
        }

        public T next() {
            throw new NoSuchElementException("EmptyFileSet Iterator");
        }

        public void remove() {
            throw new IllegalStateException("EmptyFileSet Iterator");
        }

    }

    private static class EmptyDirectoryScanner extends DirectoryScanner {

        public String[] getIncludedFiles() {
            return new String[0];
        }

    }
}
