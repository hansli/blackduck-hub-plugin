// CHECKSTYLE:OFF

package com.blackducksoftware.integration.hub.jenkins;

import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;

@SuppressWarnings({
    "",
    "PMD"
})
public class Messages {

    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

    /**
     * Please fill in the Hub credentials in the global settings!
     * 
     */
    public static String HubBuildScan_getPleaseFillInGlobalCCCredentials() {
        return holder.format("HubBuildScan_getPleaseFillInGlobalCCCredentials");
    }

    /**
     * Please fill in the Hub credentials in the global settings!
     * 
     */
    public static Localizable _HubBuildScan_getPleaseFillInGlobalCCCredentials() {
        return new Localizable(holder, "HubBuildScan_getPleaseFillInGlobalCCCredentials");
    }

    /**
     * Can not reach this server
     * 
     */
    public static String HubBuildScan_getCanNotReachThisServer() {
        return holder.format("HubBuildScan_getCanNotReachThisServer");
    }

    /**
     * Can not reach this server
     * 
     */
    public static Localizable _HubBuildScan_getCanNotReachThisServer() {
        return new Localizable(holder, "HubBuildScan_getCanNotReachThisServer");
    }

    /**
     * Please set an application name!
     * 
     */
    public static String HubBuildScan_getPleaseSetApplicationName() {
        return holder.format("HubBuildScan_getPleaseSetApplicationName");
    }

    /**
     * Please set an application name!
     * 
     */
    public static Localizable _HubBuildScan_getPleaseSetApplicationName() {
        return new Localizable(holder, "HubBuildScan_getPleaseSetApplicationName");
    }

    /**
     * This application has been created: {0}
     * 
     */
    public static String HubBuildScan_getApplicationCreated_0_(Object arg1) {
        return holder.format("HubBuildScan_getApplicationCreated_0_", arg1);
    }

    /**
     * This application has been created: {0}
     * 
     */
    public static Localizable _HubBuildScan_getApplicationCreated_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getApplicationCreated_0_", arg1);
    }

    /**
     * Please set a Server URL
     * 
     */
    public static String HubBuildScan_getPleaseSetServerUrl() {
        return holder.format("HubBuildScan_getPleaseSetServerUrl");
    }

    /**
     * Please set a Server URL
     * 
     */
    public static Localizable _HubBuildScan_getPleaseSetServerUrl() {
        return new Localizable(holder, "HubBuildScan_getPleaseSetServerUrl");
    }

    /**
     * Credentials valid for: {0}
     * 
     */
    public static String HubBuildScan_getCredentialsValidFor_0_(Object arg1) {
        return holder.format("HubBuildScan_getCredentialsValidFor_0_", arg1);
    }

    /**
     * Credentials valid for: {0}
     * 
     */
    public static Localizable _HubBuildScan_getCredentialsValidFor_0_(Object arg1) {
        return new Localizable(holder, "HubBuildScan_getCredentialsValidFor_0_", arg1);
    }

    /**
     * Please set an application version!
     * 
     */
    public static String HubBuildScan_getPleaseSetApplicationVersion() {
        return holder.format("HubBuildScan_getPleaseSetApplicationVersion");
    }

    /**
     * Please set an application version!
     * 
     */
    public static Localizable _HubBuildScan_getPleaseSetApplicationVersion() {
        return new Localizable(holder, "HubBuildScan_getPleaseSetApplicationVersion");
    }

    /**
     * Directed to the Hub.
     * 
     */
    public static String HubBuildScan_getDirectedToHub() {
        return holder.format("HubBuildScan_getDirectedToHub");
    }

    /**
     * Directed to the Hub.
     * 
     */
    public static Localizable _HubBuildScan_getDirectedToHub() {
        return new Localizable(holder, "HubBuildScan_getDirectedToHub");
    }

    /**
     * Not a valid URL
     * 
     */
    public static String HubBuildScan_getNotAValidUrl() {
        return holder.format("HubBuildScan_getNotAValidUrl");
    }

    /**
     * Not a valid URL
     * 
     */
    public static Localizable _HubBuildScan_getNotAValidUrl() {
        return new Localizable(holder, "HubBuildScan_getNotAValidUrl");
    }

    /**
     * Black Duck Hub Integration
     * 
     */
    public static String HubBuildScan_getDisplayName() {
        return holder.format("HubBuildScan_getDisplayName");
    }

    /**
     * Black Duck Hub Integration
     * 
     */
    public static Localizable _HubBuildScan_getDisplayName() {
        return new Localizable(holder, "HubBuildScan_getDisplayName");
    }

}