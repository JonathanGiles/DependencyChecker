package net.jonathangiles.tool.maven.dependencies.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class WebProject implements Project {
    private String projectName;

    private List<String> pomUrls;

    private transient Project parent;
    private transient List<WebProject> modules;

    private boolean isBom;

    private WebProject() {
        this.pomUrls = new ArrayList<>();
        this.modules = new ArrayList<>();
    }

    public WebProject(String projectName, boolean isBom) {
        this();
        this.isBom = isBom;
        this.projectName = projectName;
    }

    public WebProject(String projectName, Project parent) {
        this();
        this.projectName = projectName;
        this.parent = parent;
        this.isBom = false;
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    public Project getParent() {
        return parent;
    }

    public String getFullProjectName() {
        StringBuilder sb = new StringBuilder();
        Stack<Project> stack = new Stack<>();
        stack.push(this);

        Project p = this;
        while (p.getParent() != null) {
            stack.push(p.getParent());
            p = p.getParent();
        }

        // unwind stack
        while (!stack.empty()) {
            p = stack.pop();
            sb.append(p.getProjectName());

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
    public boolean isBom() {
        return isBom;
    }

    @Override
    public String toString() {
        return getFullProjectName();
    }
}
