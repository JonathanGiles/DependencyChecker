package net.jonathangiles.tool.maven.dependencies.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class WebProject implements Project {
    private String projectName;

    private List<String> pomUrls;

    private transient WebProject parent;
    private transient List<WebProject> modules;

    private WebProject() {
        this.pomUrls = new ArrayList<>();
        this.modules = new ArrayList<>();
    }

    public WebProject(String projectName, String... poms) {
        this();
        this.projectName = projectName;
        this.parent = null;
        this.pomUrls.addAll(Arrays.asList(poms));
    }

//    public WebProject(String projectName, WebProject parent) {
//        this();
//        this.projectName = projectName;
//        this.parent = parent;
//    }

    public String getProjectName() {
        return projectName;
    }

    public String getFullProjectName() {
        StringBuilder sb = new StringBuilder();
        Stack<WebProject> stack = new Stack<>();
        stack.push(this);

        WebProject p = this;
        while (p.parent != null) {
            stack.push(p.parent);
            p = p.parent;
        }

        // unwind stack
        while (!stack.empty()) {
            p = stack.pop();
            sb.append(p.projectName);

            if (!stack.isEmpty()) {
                sb.append("/");
            }
        }

        return sb.toString();
    }

    public List<String> getPomUrls() {
        return pomUrls;
    }

    public List<WebProject> getModules() {
        return modules;
    }

    @Override
    public String toString() {
        return getFullProjectName();
    }
}
