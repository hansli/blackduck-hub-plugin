/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.jenkins;

import java.io.File;
import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.hub.jenkins.failure.HubFailureConditionStep;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCanonicalPath;
import com.blackducksoftware.integration.hub.jenkins.scan.BDCommonScanStep;
import com.blackducksoftware.integration.hub.jenkins.scan.ScanExclusion;
import com.blackducksoftware.integration.log.IntLogger;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.DescribableList;

public class PostBuildHubScan extends Recorder {
    private final ScanJobs[] scans;

    private final String hubProjectName;

    private final String hubProjectVersion;

    private final String hubVersionPhase;

    private final String hubVersionDist;

    private final String scanMemory;

    private final boolean shouldGenerateHubReport;

    private final String bomUpdateMaximumWaitTime;

    private final boolean projectLevelAdjustments;

    private final boolean dryRun;

    private final boolean cleanupOnSuccessfulScan;

    private Boolean verbose;

    private final ScanExclusion[] excludePatterns;

    private final String codeLocationName;

    private final boolean unmapPreviousCodeLocations;

    private final boolean deletePreviousCodeLocations;

    @DataBoundConstructor
    public PostBuildHubScan(final ScanJobs[] scans, final String hubProjectName, final String hubProjectVersion, final String hubVersionPhase, final String hubVersionDist, final String scanMemory, final boolean shouldGenerateHubReport,
            final boolean projectLevelAdjustments, final String bomUpdateMaximumWaitTime, final boolean dryRun, final boolean cleanupOnSuccessfulScan, final ScanExclusion[] excludePatterns, final String codeLocationName,
            final boolean unmapPreviousCodeLocations, final boolean deletePreviousCodeLocations) {
        this.scans = scans;
        this.hubProjectName = hubProjectName;
        this.hubProjectVersion = hubProjectVersion;
        this.hubVersionPhase = hubVersionPhase;
        this.hubVersionDist = hubVersionDist;
        this.scanMemory = scanMemory;
        this.shouldGenerateHubReport = shouldGenerateHubReport;
        this.projectLevelAdjustments = projectLevelAdjustments;
        this.bomUpdateMaximumWaitTime = bomUpdateMaximumWaitTime;
        this.dryRun = dryRun;
        this.cleanupOnSuccessfulScan = cleanupOnSuccessfulScan;
        this.excludePatterns = excludePatterns;
        this.codeLocationName = codeLocationName;
        this.unmapPreviousCodeLocations = unmapPreviousCodeLocations;
        this.deletePreviousCodeLocations = deletePreviousCodeLocations;
    }

    public void setverbose(final boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        if (verbose == null) {
            verbose = true;
        }
        return verbose;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public boolean isCleanupOnSuccessfulScan() {
        return cleanupOnSuccessfulScan;
    }

    public boolean getShouldGenerateHubReport() {
        return shouldGenerateHubReport;
    }

    public boolean isProjectLevelAdjustments() {
        return projectLevelAdjustments;
    }

    public String getScanMemory() {
        return scanMemory;
    }

    public String getBomUpdateMaximumWaitTime() {
        return bomUpdateMaximumWaitTime;
    }

    public String getHubProjectVersion() {
        return hubProjectVersion;
    }

    public String getHubProjectName() {
        return hubProjectName;
    }

    public String getHubVersionPhase() {
        return hubVersionPhase;
    }

    public String getHubVersionDist() {
        return hubVersionDist;
    }

    public ScanJobs[] getScans() {
        return scans;
    }

    public ScanExclusion[] getExcludePatterns() {
        return excludePatterns;
    }

    public String getCodeLocationName() {
        return codeLocationName;
    }

    public boolean isUnmapPreviousCodeLocations() {
        return unmapPreviousCodeLocations;
    }

    public boolean isDeletePreviousCodeLocations() {
        return deletePreviousCodeLocations;
    }

    // http://javadoc.jenkins-ci.org/hudson/tasks/Recorder.html
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public PostBuildScanDescriptor getDescriptor() {
        return (PostBuildScanDescriptor) super.getDescriptor();
    }

    public HubServerInfo getHubServerInfo() {
        return HubServerInfoSingleton.getInstance().getServerInfo();
    }

    /**
     * Overrides the Recorder perform method. This is the method that gets called by Jenkins to run as a Post Build Action
     *
     */
    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        final HubJenkinsLogger logger = new HubJenkinsLogger(listener);

        try {
            final BDCommonScanStep scanStep = new BDCommonScanStep(getScans(), getHubProjectName(), getHubProjectVersion(), getHubVersionPhase(), getHubVersionDist(), getScanMemory(), isProjectLevelAdjustments(),
                    getShouldGenerateHubReport(), getBomUpdateMaximumWaitTime(), isDryRun(), isCleanupOnSuccessfulScan(), isVerbose(), getExclusionPatterns(), getCodeLocationName(), isUnmapPreviousCodeLocations(),
                    isDeletePreviousCodeLocations(), isFailureConditionsConfigured(build));
            final EnvVars envVars = build.getEnvironment(listener);

            scanStep.runScan(build, build.getBuiltOn(), envVars, getWorkingDirectory(logger, build), logger, launcher, listener, build.getFullDisplayName(), String.valueOf(build.getNumber()));
        } catch (final Exception e) {
            logger.error(e);
        }
        return true;
    }

    private boolean isFailureConditionsConfigured(final AbstractBuild<?, ?> build) {
        if (build.getParent() instanceof AbstractProject) {
            final AbstractProject project = (build.getParent());
            final DescribableList<Publisher, Descriptor<Publisher>> list = project.getPublishersList();
            final HubFailureConditionStep failureCondition = list.get(HubFailureConditionStep.class);
            if (failureCondition != null) {
                return true;
            }
        }
        return false;
    }

    public String[] getExclusionPatterns() {
        String[] exclusionPatterns = null;
        if (getExcludePatterns() != null) {
            exclusionPatterns = new String[getExcludePatterns().length];
            int i = 0;
            for (final ScanExclusion exclusion : getExcludePatterns()) {
                exclusionPatterns[i] = exclusion.getExclusionPattern();
                i++;
            }
        }
        return exclusionPatterns;
    }

    public FilePath getWorkingDirectory(final IntLogger logger, final AbstractBuild<?, ?> build) throws InterruptedException {
        String workingDirectory = "";
        try {
            if (build.getWorkspace() == null) {
                // might be using custom workspace
                workingDirectory = build.getProject().getCustomWorkspace();
            } else {
                workingDirectory = build.getWorkspace().getRemote();
            }

            workingDirectory = build.getBuiltOn().getChannel().call(new GetCanonicalPath(new File(workingDirectory)));
        } catch (final IOException e) {
            logger.error("Problem getting the working directory on this node. Error : " + e.getMessage(), e);
        }
        logger.info("Node workspace " + workingDirectory);
        return new FilePath(build.getBuiltOn().getChannel(), workingDirectory);
    }

}
