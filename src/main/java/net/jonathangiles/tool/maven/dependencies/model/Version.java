package net.jonathangiles.tool.maven.dependencies.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Version implements Comparable<Version> {
    private static Map<String, Version> parseMap = new HashMap<>();

    private int major;
    private int minor;
    private int patch;
    private String extension;

    private String versionString;

    private Version(String version) {
        this.versionString = version;
        String[] vals = version.split("\\.");

        // we need at least major!
        try {
            String val = vals.length >= 1 ? vals[0] : "";
            if (!val.isEmpty()) {
                major = Integer.parseInt(val);
            }
        } catch (Exception e) {
            System.out.println("Failed to parse version string '" + version + "' - exiting!");
            System.exit(-1);
        }

        try {
            String val = vals.length >= 2 ? vals[1] : "";
            if (!val.isEmpty()) {
                minor = Integer.parseInt(val);
            }
        } catch (Exception e) {
            System.out.println("WARNING: Failed to completely parse version string '" + version + "'");
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
            System.out.println("WARNING: Failed to completely parse version string '" + version + "'");
        }
    }

    public static Version build(String version) {
        return parseMap.computeIfAbsent(version, Version::new);
    }

    @Override
    public int compareTo(Version v) {
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
        return major == version.major &&
                minor == version.minor &&
                patch == version.patch &&
                Objects.equals(versionString, version.versionString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, versionString);
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
        return major + "." + minor + "." + patch + (extension != null ? "-" + extension : "");
    }
}