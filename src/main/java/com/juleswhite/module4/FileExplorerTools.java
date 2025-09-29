package com.juleswhite.module4;

import java.io.File;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FileExplorerTools provides a set of tools for navigating and exploring a file system.
 * All operations are relative to a specified root directory, and access is restricted to that directory.
 */
public class FileExplorerTools {

    // Instance variables to maintain state
    private final File rootDirectory;
    private final Set<String> visitedPaths;

    /**
     * Constructor initializes with a specified root directory.
     *
     * @param rootPath The path to use as the root directory
     */
    public FileExplorerTools(String rootPath) {
        this.rootDirectory = new File(rootPath);
        if (!this.rootDirectory.exists() || !this.rootDirectory.isDirectory()) {
            throw new IllegalArgumentException("Root path must be an existing directory: " + rootPath);
        }
       this.visitedPaths = new HashSet<>();
        visitedPaths.add("/");
    }

    /**
     * Constructor initializes with the current working directory as root.
     */
    public FileExplorerTools() {
        this(System.getProperty("user.dir"));
    }

    /**
     * Gets the relative path from the root directory to the specified file.
     *
     * @param file The file to get the relative path for
     * @return The relative path as a string, starting with "/"
     */
    private String getRelativePath(File file) {
        Path relativePath = rootDirectory.toPath().relativize(file.toPath());
        return "/" + relativePath.toString().replace('\\', '/');
    }

    /**
     * Resolves a path relative to the root directory, validating it doesn't escape the root.
     *
     * @param path The path to resolve (can be absolute or relative to current directory)
     * @return The resolved file, or null if invalid or outside root
     */
    private File resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        File targetFile;
        if (path.equals("/")) {
            return rootDirectory;
        } else if (path.startsWith("/")) {
            // Path relative to root
            targetFile = new File(rootDirectory, path.substring(1));
        } else {
            // Path relative to current directory
            targetFile = new File(rootDirectory, path);
        }

        // Verify the path is within the root directory
        if (!targetFile.getAbsolutePath().startsWith(rootDirectory.getAbsolutePath())) {
            return null; // Path is outside root directory
        }

        return targetFile;
    }


    @RegisterTool(tags = {"navigation"})
    public Map<String, Object> listDirectory(String path) {
        /**
         * Lists all files and directories in the specified directory.
         *
         * @param path The path to list (relative to root or current directory)
         * @return A map containing details of the directory contents
         */
        File targetDir = path == null || path.isEmpty() ? rootDirectory : resolvePath(path);

        if (targetDir == null || !targetDir.exists() || !targetDir.isDirectory()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid directory path: " + path);
            return error;
        }

        String relativePath = getRelativePath(targetDir);
        visitedPaths.add(relativePath);

        File[] contents = targetDir.listFiles();
        List<String> files = new ArrayList<>();
        List<String> directories = new ArrayList<>();

        if (contents != null) {
            for (File item : contents) {
                String itemRelativePath = getRelativePath(item);
                if (item.isFile()) {
                    files.add(itemRelativePath);
                } else if (item.isDirectory()) {
                    directories.add(itemRelativePath);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("path", relativePath);
        result.put("files", files);
        result.put("directories", directories);
        result.put("isRoot", targetDir.equals(rootDirectory));
        return result;
    }


    @RegisterTool(tags = {"file_operations"})
    public Map<String, Object> readFile(String path) {
        /**
         * Reads the content of a specified file.
         *
         * @param path The path of the file to read (relative to root or current directory)
         * @return A map containing the file content and metadata
         */
        if (path == null || path.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "File path cannot be empty");
            return error;
        }

        File file = resolvePath(path);

        if (file == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Path is outside the root directory: " + path);
            return error;
        }

        if (!file.exists()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "File does not exist: " + path);
            return error;
        }

        if (!file.isFile()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Path is not a file: " + path);
            return error;
        }

        if (file.length() > 1024 * 1024) {  // 1MB limit
            Map<String, Object> error = new HashMap<>();
            error.put("error", "File is too large to read: " + path);
            return error;
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()));

            Map<String, Object> result = new HashMap<>();
            result.put("content", content);
            result.put("path", getRelativePath(file));
            result.put("size", file.length());
            result.put("lastModified", new Date(file.lastModified()).toString());

            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error reading file: " + e.getMessage());
            return error;
        }
    }

    @RegisterTool(tags = {"search"})
    public Map<String, Object> findFiles(String directory, String pattern) {
        /**
         * Finds files matching the given pattern in the specified directory.
         *
         * @param directory The directory to search in (relative to root or current directory)
         * @param pattern The file name pattern to match
         * @return A map containing the matching file paths
         */
        if (directory == null || directory.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Directory path cannot be empty");
            return error;
        }

        File dir = resolvePath(directory);

        if (dir == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Path is outside the root directory: " + directory);
            return error;
        }

        if (!dir.exists() || !dir.isDirectory()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Directory does not exist: " + directory);
            return error;
        }

        if (pattern == null || pattern.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Search pattern cannot be empty");
            return error;
        }

        File[] allFiles = dir.listFiles();
        List<String> matchingFiles = new ArrayList<>();

        if (allFiles != null) {
            for (File file : allFiles) {
                if (file.isFile() && file.getName().contains(pattern)) {
                    matchingFiles.add(getRelativePath(file));
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pattern", pattern);
        result.put("directory", getRelativePath(dir));
        result.put("matchingFiles", matchingFiles);
        return result;
    }

    @RegisterTool(tags = {"search"})
    public Map<String, Object> searchInFiles(String directory, String keyword) {
        /**
         * Searches for a keyword in all text files in the specified directory.
         *
         * @param directory The directory to search in (relative to root or current directory)
         * @param keyword The keyword to search for
         * @return A map containing the matching file paths and lines
         */
        if (directory == null || directory.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Directory path cannot be empty");
            return error;
        }

        File dir = resolvePath(directory);

        if (dir == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Path is outside the root directory: " + directory);
            return error;
        }

        if (!dir.exists() || !dir.isDirectory()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Directory does not exist: " + directory);
            return error;
        }

        if (keyword == null || keyword.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Search keyword cannot be empty");
            return error;
        }

        File[] files = dir.listFiles();
        List<Map<String, Object>> results = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isTextFile(file)) {
                    try {
                        List<String> lines = Files.readAllLines(file.toPath());
                        List<Map<String, Object>> matchingLines = new ArrayList<>();

                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            if (line.contains(keyword)) {
                                Map<String, Object> match = new HashMap<>();
                                match.put("lineNumber", i + 1);
                                match.put("content", line);
                                matchingLines.add(match);
                            }
                        }

                        if (!matchingLines.isEmpty()) {
                            Map<String, Object> fileResult = new HashMap<>();
                            fileResult.put("file", getRelativePath(file));
                            fileResult.put("matches", matchingLines);
                            results.add(fileResult);
                        }
                    } catch (Exception e) {
                        // Skip files that can't be read
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("directory", getRelativePath(dir));
        result.put("results", results);
        return result;
    }

    @RegisterTool(tags = {"navigation"})
    public List<String> getVisitedPaths() {
        /**
         * Returns a list of all directory paths that have been visited.
         *
         * @return List of visited directory paths relative to root
         */
        return new ArrayList<>(visitedPaths);
    }

    @RegisterTool(tags = {"system"}, terminal = true)
    public Map<String, Object> terminate(String summary) {
        /**
         * Terminates the agent's execution with a summary of findings.
         *
         * @param summary The summary of exploration and findings
         * @return A map containing the termination status and summary
         */
        Map<String, Object> result = new HashMap<>();
        result.put("status", "terminated");
        result.put("summary", summary);
        result.put("rootPath", rootDirectory.getAbsolutePath());
        result.put("exploredPaths", new ArrayList<>(visitedPaths));
        return result;
    }

    // Helper method to determine if a file is likely a text file
    private boolean isTextFile(File file) {
        String name = file.getName().toLowerCase();
        String[] textExtensions = {".txt", ".java", ".py", ".md", ".json", ".xml", ".html", ".css", ".js", ".csv"};

        for (String ext : textExtensions) {
            if (name.endsWith(ext)) {
                return true;
            }
        }

        // Additional check for files without extensions
        if (!name.contains(".")) {
            try {
                // Try to read a few bytes to check if it's text
                byte[] bytes = Files.readAllBytes(file.toPath());
                int checkLength = Math.min(bytes.length, 1000);
                for (int i = 0; i < checkLength; i++) {
                    // Non-printable characters often indicate binary file
                    if (bytes[i] < 32 && bytes[i] != 9 && bytes[i] != 10 && bytes[i] != 13) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }
}