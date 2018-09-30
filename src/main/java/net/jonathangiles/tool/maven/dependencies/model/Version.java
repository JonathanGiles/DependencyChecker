package net.jonathangiles.tool.maven.dependencies.model;

public final class Version implements Comparable<Version> {
    private int major;
    private int minor;
    private int patch;
    private String versionString;

    private Version(String version) {
        this.versionString = version;
        try {
            String[] vals = version.split("\\.");
            major = parse(vals, 0);
            minor = parse(vals, 1);
            patch = parse(vals, 2);
        } catch (Exception e) {
            System.out.println("Failed to parse version string '" + version + "'");
            System.exit(-1);
        }
    }

    public static Version build(String version) {
        return new Version(version);
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

    private int parse(String[] vals, int i) {
        return vals.length >= (i+1) ? Integer.parseInt(vals[i]) : 0;
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
}