/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Log-capture fallback (task 2.d): rather than fighting pax-logging's classloader isolation to
 *  attach a programmatic log4j2 appender to the LogMediator logger, the sequence instrumenter
 *  inserts one of these right after every real <log> mediator, carrying a serialized copy of the
 *  original <log>'s properties in the single string-settable "spec" property (the Synapse class
 *  mediator only supports simple, single-arg string setters via reflection - see
 *  ClassMediator#updateInstancePropertiesAndMediate - so an arbitrary list of child properties
 *  cannot be injected directly). At mediate() time this mediator re-parses spec, re-evaluates any
 *  XPath/get-property() expressions against the live MessageContext, and records the resulting
 *  line into {@link SandboxRunRegistry} for the current run. This works regardless of how (or
 *  whether) the underlying commons-logging -> pax-logging -> log4j2 chain is wired in this
 *  runtime, at the cost of duplicating (a subset of) LogMediator's message-formatting logic.
 *
 *  spec format: one property per line ("\n"-separated), each line "name\tKIND\tvalueOrExpr" where
 *  KIND is "L" for a literal value or "X" for an expression string to be compiled as a
 *  {@link SynapseXPath}.
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.xpath.SynapseXPath;

import java.util.ArrayList;
import java.util.List;

public class LogReplicaMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(LogReplicaMediator.class);

    private String runId;
    private String nodeId;
    private String level = "INFO";
    private String spec = "";

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    @Override
    public boolean mediate(MessageContext synCtx) {

        SandboxRunRegistry.RunRecord run = SandboxRunRegistry.get(runId);
        if (run == null) {
            return true;
        }
        List<String> parts = new ArrayList<>();
        if (spec != null && !spec.isEmpty()) {
            for (String line : spec.split("\n")) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] tokens = line.split("\t", 3);
                if (tokens.length != 3) {
                    continue;
                }
                String name = tokens[0];
                String kind = tokens[1];
                String raw = tokens[2];
                String value = raw;
                if ("X".equals(kind)) {
                    try {
                        value = new SynapseXPath(raw).stringValueOf(synCtx);
                    } catch (Exception e) {
                        value = "<error evaluating '" + raw + "': " + e.getMessage() + ">";
                        log.debug("Sandbox log-replica failed to evaluate expression: " + raw, e);
                    }
                }
                parts.add(name + " = " + value);
            }
        }
        run.addLog(nodeId, level, String.join(", ", parts));
        return true;
    }

    @Override
    public String getType() {
        return "APIMPolicySandboxLogReplica";
    }
}
