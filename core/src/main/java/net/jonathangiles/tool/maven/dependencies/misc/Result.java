package net.jonathangiles.tool.maven.dependencies.misc;

public enum Result {
    CLEAN,
    NOT_ON_LATEST,
    DEPENDENCY_VERSION_CONFLICTS;

    public static Result compare(Result r1, Result r2) {
        return r1.ordinal() > r2.ordinal() ? r1 : r2;
    }
}
