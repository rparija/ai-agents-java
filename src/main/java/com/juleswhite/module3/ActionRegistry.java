package com.juleswhite.module3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionRegistry {
    private final Map<String, Tool> tools;
    private final Map<String, Object> toolBindings;

    public ActionRegistry() {
        this.tools = new HashMap<>();
        this.toolBindings = new HashMap<>();
    }

    public void register(Tool tool, Object binding) {
        tools.put(tool.getToolName(), tool);
        toolBindings.put(tool.getToolName(), binding);
    }

    public Action getAction(String toolName) {
        Tool tool = tools.get(toolName);
        if (tool != null) {
            return new Action(tool);
        }
        return null;
    }

    public Action getAction(String toolName, Map<String, Object> args) {
        Action action = getAction(toolName);
        if (action != null) {
            action.setArgs(args);
        }
        return action;
    }

    public Object getBinding(String toolName) {
        return toolBindings.get(toolName);
    }

    public List<Tool> getTools() {
        return new ArrayList<>(tools.values());
    }
}