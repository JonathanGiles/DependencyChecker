package net.jonathangiles.tool.maven.dependencies.report;

import java.util.*;
import java.util.stream.Stream;

public final class Reporters {

    private static final Map<String, Reporter> reporters = new HashMap<>();

    static {
        ServiceLoader<Reporter> services = ServiceLoader.load(Reporter.class);
        Iterator<Reporter> it = services.iterator();

        while (it.hasNext()) {
            Reporter r = it.next();
            reporters.put(r.getName(), r);
        }
    }

    public static Stream<Reporter> getReporters(String... names) {
        if (names == null || names.length == 0) {
            return reporters.values().stream();
        }
        return Stream.of(names)
                .filter(reporters::containsKey)
                .map(reporters::get);
    }

    public static Stream<String> getReporterNames() {
        return reporters.keySet().stream();
    }
}
