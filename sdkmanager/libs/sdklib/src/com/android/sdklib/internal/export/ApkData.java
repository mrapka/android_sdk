/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.sdklib.internal.export;

import com.android.sdklib.xml.ManifestData.SupportsScreens;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Class representing one apk that needs to be generated. This contains
 * which project it must be created from, and which filters should be used.
 *
 * This class is meant to be sortable in a way that allows generation of the buildInfo
 * value that goes in the composite versionCode.
 */
public class ApkData implements Comparable<ApkData> {

    private static final String PROP_SCREENS = "screens";
    private static final String PROP_ABI = "abi";
    private static final String PROP_GL = "gl";
    private static final String PROP_API = "api";
    private static final String PROP_PROJECT = "project";
    private static final String PROP_MINOR = "minor";
    private static final String PROP_BUILDINFO = "buildinfo";
    private static final String PROP_OUTPUTNAME = "outputname";

    private String mOutputName;
    private String mRelativePath;
    private File mProject;
    private int mBuildInfo;
    private int mMinor;

    // the following are used to sort the export data and generate buildInfo
    private int mMinSdkVersion;
    private String mAbi;
    private int mGlVersion = -1;
    private SupportsScreens mSupportsScreens;

    ApkData() {
        // do nothing.
    }

    public ApkData(int minSdkVersion, SupportsScreens supportsScreens, int glEsVersion) {
        mMinSdkVersion = minSdkVersion;
        mSupportsScreens = supportsScreens;
        mGlVersion = glEsVersion;
    }

    public ApkData(ApkData data) {
        mRelativePath = data.mRelativePath;
        mProject = data.mProject;
        mBuildInfo = data.mBuildInfo;
        mMinor = data.mBuildInfo;
        mMinSdkVersion = data.mMinSdkVersion;
        mAbi = data.mAbi;
        mGlVersion = data.mGlVersion;
        mSupportsScreens = data.mSupportsScreens;
    }

    public String getOutputName() {
        return mOutputName;
    }

    public void setOutputName(String outputName) {
        mOutputName = outputName;
    }

    public String getRelativePath() {
        return mRelativePath;
    }

    public void setRelativePath(String relativePath) {
        mRelativePath = relativePath;
    }

    public File getProject() {
        return mProject;
    }

    public void setProject(File project) {
        mProject = project;
    }

    public int getBuildInfo() {
        return mBuildInfo;
    }

    public void setBuildInfo(int buildInfo) {
        mBuildInfo = buildInfo;
    }

    public int getMinor() {
        return mMinor;
    }

    public void setMinor(int minor) {
        mMinor = minor;
    }

    public int getMinSdkVersion() {
        return mMinSdkVersion;
    }

    public String getAbi() {
        return mAbi;
    }

    public void setAbi(String abi) {
        mAbi = abi;
    }

    public int getGlVersion() {
        return mGlVersion;
    }

    public SupportsScreens getSupportsScreens() {
        return mSupportsScreens;
    }

    /**
     * Computes and returns the composite version code
     * @param versionCode the major version code.
     * @return the composite versionCode to be used in the manifest.
     */
    public int getCompositeVersionCode(int versionCode) {
        int trueVersionCode = versionCode * MultiApkExportHelper.OFFSET_VERSION_CODE;
        trueVersionCode += getBuildInfo() * MultiApkExportHelper.OFFSET_BUILD_INFO;
        trueVersionCode += getMinor();

        return trueVersionCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        write(sb, PROP_OUTPUTNAME, mOutputName);
        write(sb, PROP_BUILDINFO, mBuildInfo);
        sb.append(getLogLine());

        return sb.toString();
    }

    public String getLogLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(mBuildInfo).append(':');
        write(sb, PROP_MINOR, mMinor);
        write(sb, PROP_PROJECT, mRelativePath);
        write(sb, PROP_API, mMinSdkVersion);

        if (mGlVersion != -1) {
            write(sb, PROP_GL, mGlVersion);
        }

        if (mAbi != null) {
            write(sb, PROP_ABI, mAbi);
        }

        write(sb, PROP_SCREENS, mSupportsScreens);

        return sb.toString();
    }

    public int compareTo(ApkData o) {
        int minSdkDiff = mMinSdkVersion - o.mMinSdkVersion;
        if (minSdkDiff != 0) {
            return minSdkDiff;
        }

        int comp;
        if (mAbi != null) {
            if (o.mAbi != null) {
                comp = mAbi.compareTo(o.mAbi);
                if (comp != 0) return comp;
            } else {
                return -1;
            }
        } else if (o.mAbi != null) {
            return 1;
        }

        comp = mSupportsScreens.compareTo(o.mSupportsScreens);
        if (comp != 0) return comp;

        if (mGlVersion != -1) {
            if (o.mGlVersion != -1) {
                comp = mGlVersion - o.mGlVersion;
                if (comp != 0) return comp;
            } else {
                return -1;
            }
        } else if (o.mGlVersion != -1) {
            return 1;
        }

        return 0;
    }

    /**
     * reads the apk description from a log line.
     * @param line The fields to read, comma-separated.
     *
     * @see #getLogLine()
     */
    public void initFromLogLine(String line) {
        int colon = line.indexOf(':');
        mBuildInfo = Integer.parseInt(line.substring(0, colon));
        String[] properties = line.substring(colon+1).split(";");
        HashMap<String, String> map = new HashMap<String, String>();
        for (String prop : properties) {
            colon = prop.indexOf('=');
            map.put(prop.substring(0, colon), prop.substring(colon+1));
        }
        setValues(map);
    }

    private void setValues(Map<String, String> values) {
        mMinor = Integer.parseInt(values.get(PROP_MINOR));
        mRelativePath = values.get(PROP_PROJECT);
        mMinSdkVersion = Integer.parseInt(values.get(PROP_API));

        String tmp = values.get(PROP_GL);
        if (tmp != null) {
            mGlVersion = Integer.parseInt(tmp);
        }

        tmp = values.get(PROP_ABI);
        if (tmp != null) {
            mAbi = tmp;
        }

        tmp = values.get(PROP_SCREENS);
        if (tmp != null) {
            mSupportsScreens = new SupportsScreens(tmp);
        }
    }

    private void write(StringBuilder sb, String name, Object value) {
        sb.append(name + "=").append(value).append(';');
    }

    private void write(StringBuilder sb, String name, int value) {
        sb.append(name + "=").append(value).append(';');
    }
}
