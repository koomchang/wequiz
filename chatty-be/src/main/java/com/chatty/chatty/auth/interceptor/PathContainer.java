package com.chatty.chatty.auth.interceptor;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

@Slf4j
public class PathContainer {

    private final PathMatcher pathMatcher;
    private final List<PathRequest> includePatterns;
    private final List<PathRequest> excludePatterns;

    public PathContainer() {
        this.pathMatcher = new AntPathMatcher();
        this.includePatterns = new ArrayList<>();
        this.excludePatterns = new ArrayList<>();
    }

    public boolean isNotIncludedPath(String targetPath, String pathMethod) {
        boolean isExcludePattern = excludePatterns.stream()
                .anyMatch(pathPattern -> pathPattern.matches(pathMatcher, targetPath, pathMethod));
        boolean isNotIncludePattern = includePatterns.stream()
                .noneMatch(pathPattern -> pathPattern.matches(pathMatcher, targetPath, pathMethod));
        return isExcludePattern || isNotIncludePattern;
    }

    public void addIncludePatterns(String path, HttpMethod... methods) {
        for (HttpMethod httpMethod : methods) {
            includePatterns.add(new PathRequest(path, httpMethod));
        }
    }

    public void addExcludePatterns(String path, HttpMethod... methods) {
        for (HttpMethod httpMethod : methods) {
            excludePatterns.add(new PathRequest(path, httpMethod));
        }
    }
}
