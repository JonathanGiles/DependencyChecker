package net.jonathangiles.tool.maven.dependencies.misc;

import net.jonathangiles.tool.maven.dependencies.model.Version;
import org.jboss.shrinkwrap.resolver.api.maven.*;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Util {

    private static final Map<String, Version> resolvedVersionsWithNoQualifiers = new HashMap<>();
    private static final Map<String, Version> resolvedVersions = new HashMap<>(); // this map contains values with or without qualifiers

    private Util() {  }

    public static MavenResolverSystemBase<PomEquippedResolveStage, PomlessResolveStage, MavenStrategyStage, MavenFormatStage> getMavenResolver() {
        // TODO externalise configuration of which repos to use
        return Maven.configureResolver()
//                .withRemoteRepo(MavenRemoteRepositories
//                        .createRemoteRepository("spring-plugins", "http://repo.spring.io/plugins-release/", "default")
//                        .setChecksumPolicy(MavenChecksumPolicy.CHECKSUM_POLICY_IGNORE)
//                        .setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_NEVER))
//                .withRemoteRepo(MavenRemoteRepositories
//                        .createRemoteRepository("snapshots", "https://oss.sonatype.org/content/repositories/snapshots/", "default")
//                        .setChecksumPolicy(MavenChecksumPolicy.CHECKSUM_POLICY_IGNORE)
//                        .setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_NEVER))
                .withMavenCentralRepo(true);
    }

    public static Version getLatestVersionInMavenCentral(String groupId, String artifactId, boolean acceptQualifiers) {
        return getLatestVersionInMavenCentral(groupId + ":" + artifactId, acceptQualifiers);
    }

    public static Version getLatestVersionInMavenCentral(String ga, boolean acceptQualifiers) {
        Map<String, Version> mapToLookup = acceptQualifiers ? resolvedVersions : resolvedVersionsWithNoQualifiers;

        Version result = mapToLookup.computeIfAbsent(ga, key -> versionLookup(key, acceptQualifiers));

        if (!acceptQualifiers && result == Version.UNKNOWN) {
            // try again with qualifiers
            result = resolvedVersions.computeIfAbsent(ga, key -> versionLookup(key, true));
        }

        return result;
    }

    public static Version getVersionFromGAV(String gav) {
        String version = gav.substring(gav.lastIndexOf(":") + 1);
        return Version.build(version);
    }

    private static Version versionLookup(String ga, boolean acceptQualifiers) {
        // special cases for some broken maven central artifacts
        switch (ga) {
            // oddly, a few artifacts do not have maven-metadata.xml files, e.g.
            // http://repo1.maven.org/maven2/net/jcip/jcip-annotations/ does not have a maven-metadata.xml file
            case "net.jcip:jcip-annotations": return Version.build("1.0");
            case "com.vaadin.external.google:android-json": return Version.build("0.0.20131108");
        }

        // otherwise do the usual lookup
        try {
            MavenVersionRangeResult rangeResult = getMavenResolver().resolveVersionRange(ga + ":[0.1,)");
            if (rangeResult.getVersions().isEmpty()) {
                System.err.println("Failed to get any versions for " + ga + " - exiting");
                System.exit(-1);
            }

            Optional<MavenCoordinate> result = rangeResult
                    .getVersions()
                    .stream()
                    .filter(coor -> acceptQualifiers || !coor.getVersion().contains("-")) // we don't want -SNAPSHOT, etc
                    .reduce((first, second) -> second); // the highest version is the last version

            return result.map(c -> Version.build(c.getVersion())).orElse(Version.UNKNOWN);
        } catch (Exception e) {
            return Version.UNKNOWN;
        }
    }
}
