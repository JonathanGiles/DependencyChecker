package net.jonathangiles.tool.maven.dependencies.model;

import java.util.ArrayList;

public class DependencyVersionList extends ArrayList<DependencyVersion> {

    public boolean hasAnyDependencyChains() {
        return stream().anyMatch(DependencyVersion::hasDependencyChain);
    }
}
