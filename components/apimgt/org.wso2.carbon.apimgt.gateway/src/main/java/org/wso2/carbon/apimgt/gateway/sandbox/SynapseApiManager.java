/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Deploys/undeploys temporary, in-memory Synapse APIs used by the policy sandbox. See the feature
 *  plan ("Fase 1", task 2.a) for why the direct APIFactory route was chosen over
 *  RestApiAdmin/SynapseArtifactDeploymentStore (which persists artifacts to disk). Formerly
 *  {@code SpikeSynapseApiManager} (Phase 1 spike); promoted unchanged into Phase 2 production.
 */
package org.wso2.carbon.apimgt.gateway.sandbox;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.api.API;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.rest.APIFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.wso2.carbon.apimgt.gateway.internal.ServiceReferenceHolder;
import org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService;

public final class SynapseApiManager {

    private static final Log log = LogFactory.getLog(SynapseApiManager.class);

    private SynapseApiManager() {
    }

    public static SynapseConfiguration getSynapseConfiguration() {
        return ServiceReferenceHolder.getInstance().getSynapseConfigurationService().getSynapseConfiguration();
    }

    /**
     * Looks up the super-tenant SynapseEnvironment straight from the OSGi service registry (there
     * is no {@code SynapseEnvironmentService} exposed through carbon-apimgt's own
     * {@code ServiceReferenceHolder}).
     */
    public static SynapseEnvironment getSynapseEnvironment() {

        BundleContext bundleContext = FrameworkUtil.getBundle(SynapseApiManager.class).getBundleContext();
        if (bundleContext == null) {
            throw new IllegalStateException("No OSGi BundleContext available (are we running outside OSGi?)");
        }
        ServiceReference<SynapseEnvironmentService> ref =
                bundleContext.getServiceReference(SynapseEnvironmentService.class);
        if (ref == null) {
            throw new IllegalStateException("SynapseEnvironmentService is not registered yet");
        }
        SynapseEnvironmentService service = bundleContext.getService(ref);
        try {
            if (service == null) {
                throw new IllegalStateException("SynapseEnvironmentService lookup returned null");
            }
            return service.getSynapseEnvironment();
        } finally {
            bundleContext.ungetService(ref);
        }
    }

    /**
     * Parses {@code apiXml}, initializes it against the live SynapseEnvironment and adds it to the
     * running SynapseConfiguration in memory (no disk I/O).
     *
     * @return the deployed API's name (as given in the XML), for later undeploy.
     */
    public static String deploy(OMElement apiXml) {

        API api = APIFactory.createAPI(apiXml);
        api.init(getSynapseEnvironment());
        SynapseConfiguration synCfg = getSynapseConfiguration();
        synCfg.addAPI(api.getName(), api);
        // Rebuild the internal (sorted-by-specificity) dispatch table. addAPI() alone is NOT
        // enough for correct REST dispatch of a freshly-added API (see Phase 1 spike notes).
        synCfg.reconstructAPITable();
        if (log.isDebugEnabled()) {
            log.debug("Sandbox deployed temporary API " + api.getName() + " at context " + api.getContext());
        }
        return api.getName();
    }

    /**
     * Removes the API from the running configuration and calls destroy() on it. Safe to call even
     * if the API is already gone.
     */
    public static void undeploy(String apiName) {

        SynapseConfiguration synCfg = getSynapseConfiguration();
        API api = synCfg.getAPI(apiName);
        if (api == null) {
            return;
        }
        try {
            synCfg.removeAPI(apiName);
        } finally {
            try {
                api.destroy();
            } catch (Exception e) {
                log.warn("Error destroying sandbox API " + apiName, e);
            }
        }
    }

    public static boolean exists(String apiName) {
        return getSynapseConfiguration().getAPI(apiName) != null;
    }

    /** Names of all currently deployed sandbox APIs (main or mock), for the janitor to sweep. */
    public static java.util.List<String> listSandboxApiNames(String prefix) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Object apiObj : getSynapseConfiguration().getAPIs()) {
            API api = (API) apiObj;
            if (api.getName() != null && api.getName().startsWith(prefix)) {
                names.add(api.getName());
            }
        }
        return names;
    }
}
