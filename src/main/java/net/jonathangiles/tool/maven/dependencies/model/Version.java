package net.jonathangiles.tool.maven.dependencies.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Version implements Comparable<Version> {
    private static final String UNKNOWN_VERSION_STRING = "<unknown version>";
    public static final Version UNKNOWN = new Version("");

    private static Map<String, Version> parseMap = new HashMap<>();

    private int major = -1;
    private int minor = -1;
    private int patch = -1;
    private String extension;
    private boolean isSnapshot;

    private String versionString;

    private Version(String version) {
        this.versionString = version;
        this.isSnapshot = version.contains("SNAPSHOT");

        String[] vals = version.split("\\.");

        // we need at least major!
        try {
            String val = vals.length >= 1 ? vals[0] : "";
            if (!val.isEmpty()) {
                major = Integer.parseInt(val);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse version string '" + version + "'!");
        }

        try {
            String val = vals.length >= 2 ? vals[1] : "";
            if (!val.isEmpty()) {
                minor = Integer.parseInt(val);
            }
        } catch (Exception e) {
            System.err.println("WARNING: Failed to completely parse version string '" + version + "'");
        }

        try {
            String val = vals.length >= 3 ? vals[2] : "";

            if (!val.isEmpty()) {
                if (val.contains("-")) {
                    int endIndex = val.indexOf("-");
                    patch = Integer.parseInt(val.substring(0, endIndex));
                    extension = val.substring(endIndex + 1);
                } else {
                    patch = Integer.parseInt(val);
                }
            }
        } catch (Exception e) {
            System.err.println("WARNING: Failed to completely parse version string '" + version + "'");
        }
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }

    public static Version build(String version) {
        return parseMap.computeIfAbsent(version, Version::new);
    }

    @Override
    public int compareTo(Version v) {
        if (major == -1) {
            return versionString.compareTo(v.versionString);
        }

        int t = Integer.compare(major, v.major);
        if (t != 0) return t;

        t = Integer.compare(minor, v.minor);
        if (t != 0) return t;

        t = Integer.compare(patch, v.patch);
        return t;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;

        if (major == -1) {
            return Objects.equals(versionString, version.versionString);
        }

        return major == version.major &&
                minor == version.minor &&
                patch == version.patch &&
                Objects.equals(versionString, version.versionString);
    }

    @Override
    public int hashCode() {
        return major == -1? Objects.hash(versionString) : Objects.hash(major, minor, patch, versionString);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getVersionString() {
        return versionString;
    }

    @Override
    public String toString() {
        return versionString;
    }
}