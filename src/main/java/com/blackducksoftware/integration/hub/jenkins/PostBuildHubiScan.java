package com.blackducksoftware.integration.hub.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.JDK;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import hudson.tools.ToolDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.IScanToolMissingException;

public class PostBuildHubiScan extends Recorder {

    private IScanJobs[] scans;

    private String iScanName;

    private String workingDirectory;

    private JDK java;

    @DataBoundConstructor
    public PostBuildHubiScan(IScanJobs[] scans, String iScanName) {
        this.scans = scans;
        this.iScanName = iScanName;
    }

    public IScanJobs[] getScans() {
        return scans;
    }

    public String getiScanName() {
        return iScanName;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    private void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    // http://javadoc.jenkins-ci.org/hudson/tasks/Recorder.html
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public PostBuildScanDescriptor getDescriptor() {
        return (PostBuildScanDescriptor) super.getDescriptor();
    }

    /**
     * Overrides the Recorder perform method. This is the method that gets called by Jenkins to run as a Post Build
     * Action
     * 
     * @param build
     *            AbstractBuild
     * @param launcher
     *            Launcher
     * @param listener
     *            BuildListener
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        Result result = build.getResult();
        if (result.equals(Result.SUCCESS)) {
            try {
                listener.getLogger().println("Starting Black Duck iScans...");

                IScanInstallation[] iScanTools = null;
                ToolDescriptor<IScanInstallation> iScanDescriptor = (ToolDescriptor<IScanInstallation>) build.getDescriptorByName(IScanInstallation.class
                        .getSimpleName());
                iScanTools = iScanDescriptor.getInstallations();
                if (validateConfiguration(iScanTools, getScans())) {
                    // This set the base of the scan Target, DO NOT remove this or the user will be able to specify any
                    // file even outside of the Jenkins directories
                    setWorkingDirectory(build.getWorkspace().getRemote()); // This should work on master and
                                                                           // slaves
                    setJava(build, listener);
                    FilePath iScanScript = getIScanScript(iScanTools, listener, build);
                    List<String> scanTargets = new ArrayList<String>();
                    for (IScanJobs scanJob : getScans()) {
                        scanTargets.add(getWorkingDirectory() + "/" + scanJob.getScanTarget()); // Prefixes the targets
                                                                                                // with the workspace
                                                                                                // directory
                    }
                    runScan(build, launcher, listener, iScanScript, scanTargets);
                }

            } catch (Exception e) {
                e.printStackTrace(listener.getLogger());
                listener.error(e.getMessage());
                result = Result.UNSTABLE;
            }
        } else {
            listener.getLogger().println("Build was not successful. Will not run Black Duck iScans.");
        }
        build.setResult(result);
        return true;
    }

    /**
     * Validates that the target of the scanJob exists, creates a ProcessBuilder to run the shellscript and passes in
     * the necessarry arguments, sets the JAVA_HOME of the Process Builder to the one that the User chose, starts the
     * process and prints out all stderr and stdout to the Console Output.
     * 
     * @param build
     *            AbstractBuild
     * @param launcher
     *            Launcher
     * @param listener
     *            BuildListener
     * @param iScanScript
     *            FilePath
     * @param scanTargets
     *            List<String>
     * 
     * @throws IOException
     * @throws HubConfigurationException
     * @throws InterruptedException
     */
    private void runScan(AbstractBuild build, Launcher launcher, BuildListener listener, FilePath iScanScript, List<String> scanTargets) throws IOException,
            HubConfigurationException, InterruptedException {

        validateScanTargets(listener, build.getBuiltOn().getChannel(), scanTargets);
        // Use a substring of the host url because the http:// is not currently needed in the definition.
        // String[] cmdCreds = { iScanScript.getRemote(),
        // "--host", getDescriptor().getServerUrl().substring(7)
        // , "--username", getDescriptor().getHubServerInfo().getUsername()
        // , "--password", getDescriptor().getHubServerInfo().getPassword() };
        List<String> cmd = new ArrayList<String>();
        cmd.add(iScanScript.getRemote());
        cmd.add("--host");
        cmd.add(getDescriptor().getServerUrl().substring(7));
        cmd.add("--username");
        cmd.add(getDescriptor().getHubServerInfo().getUsername());
        cmd.add("--password");
        cmd.add(getDescriptor().getHubServerInfo().getPassword());
        for (String target : scanTargets) {
            cmd.add(target);
        }
        listener.getLogger().println("[DEBUG] : Using this java installation : " + getJava().getName() + " : " +
                getJava().getHome());
        // String[] cmd = ObjectArrays.concat(cmdCreds, scanTargets, String.class);

        ProcStarter ps = launcher.launch();

        ps.envs(build.getEnvironment(listener));
        ps.cmds(cmd);
        ps.stdout(listener);
        ps.join();

        OutputStream outputStream = ps.stdout();
        PrintStream printOutStream = new PrintStream(outputStream);
        printOutStream.println();
        printOutStream.flush();
        // DO NOT close this PrintStream or Jenkins will not be able to log any more messages. Jenkins will handle
        // closing it.

    }

    public JDK getJava() {
        return java;
    }

    /**
     * Sets the Java Home that is to be used for running the Shell script
     * 
     * @param build
     *            AbstractBuild
     * @param listener
     *            BuildListener
     * @throws IOException
     * @throws InterruptedException
     */
    private void setJava(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
        EnvVars envVars = new EnvVars();
        envVars = build.getEnvironment(listener);
        JDK javaHomeTemp = null;
        javaHomeTemp = build.getProject().getJDK();
        if (StringUtils.isEmpty(build.getBuiltOn().getNodeName())) {
            // Empty node name indicates master
            javaHomeTemp = build.getProject().getJDK();
        } else {
            javaHomeTemp = javaHomeTemp.forNode(build.getBuiltOn(), listener);
        }
        if (javaHomeTemp == null || StringUtils.isEmpty(javaHomeTemp.getHome())) {
            // In case the user did not select a java installation, set to the environment variable JAVA_HOME
            javaHomeTemp = new JDK("Default Java", envVars.get("JAVA_HOME"));
        }
        java = javaHomeTemp;
    }

    /**
     * Looks through the iScanInstallations to find the one that the User chose, then looks for the scan.cli.sh in the
     * bin folder of the directory defined by the Installation.
     * It then checks that the File exists.
     * 
     * @param iScanTools
     *            IScanInstallation[] User defined iScan installations
     * @param listener
     *            BuildListener
     * @param build
     *            AbstractBuild
     * 
     * @return File the scan.cli.sh
     * @throws IScanToolMissingException
     * @throws IOException
     * @throws InterruptedException
     * @throws HubConfigurationException
     */
    public FilePath getIScanScript(IScanInstallation[] iScanTools, BuildListener listener, AbstractBuild build) throws IScanToolMissingException, IOException,
            InterruptedException, HubConfigurationException {
        File locationFile = null;
        FilePath iScanScript = null;
        for (IScanInstallation iScan : iScanTools) {
            if (StringUtils.isEmpty(build.getBuiltOn().getNodeName())) {
                // Empty node name indicates master
                listener.getLogger().println("[DEBUG] : master");
            } else {
                listener.getLogger().println("[DEBUG] : " + build.getBuiltOn().getNodeName());
                iScan = iScan.forNode(build.getBuiltOn(), listener);
            }
            if (iScan.getName().equals(getiScanName())) {
                locationFile = new File(iScan.getHome() + "/bin/scan.cli.sh");
                iScanScript = new FilePath(build.getBuiltOn().getChannel(), locationFile.getCanonicalPath());
            }
        }
        if (iScanScript == null) {
            // Should not get here unless there are no iScan Installations defined
            // But we check this just in case
            throw new HubConfigurationException("You need to select which iScan installation to use.");
        }
        if (!iScanScript.exists()) {
            listener.getLogger().println("[ERROR] : Script doesn't exist : " + iScanScript.getRemote());
            throw new IScanToolMissingException("Could not find the script file to execute.");
        } else {
            listener.getLogger().println(
                    "[DEBUG] : Using this iScan script at : " + iScanScript.getRemote());
        }
        return iScanScript;
    }

    /**
     * Validates that the Plugin is configured correctly. Checks that the User has defined an iScan tool, a Hub server
     * URL, a Credential, and that there are at least one scan Target/Job defined in the Build
     * 
     * @param iScanTools
     *            IScanInstallation[] User defined iScan installations
     * @param scans
     *            IScanJobs[] the iScan jobs defined in the Job config
     * 
     * @return True if everything is configured correctly
     * 
     * @throws IScanToolMissingException
     * @throws HubConfigurationException
     */
    public boolean validateConfiguration(IScanInstallation[] iScanTools, IScanJobs[] scans) throws IScanToolMissingException, HubConfigurationException {
        if (iScanTools[0] == null) {
            throw new IScanToolMissingException("Could not find an iScan Installation to use.");
        }
        if (scans == null || scans.length == 0) {
            throw new HubConfigurationException("Could not find any targets to scan.");
        }
        if (!getDescriptor().getHubServerInfo().isPluginConfigured()) {
            // If plugin is not Configured, we try to find out what is missing.
            if (StringUtils.isEmpty(getDescriptor().getHubServerInfo().getServerUrl())) {
                throw new HubConfigurationException("Could not find any targets to scan.");
            }
            if (StringUtils.isEmpty(getDescriptor().getHubServerInfo().getCredentialsId())) {
                throw new HubConfigurationException("Could not find any targets to scan.");
            }
        }
        // No exceptions were thrown so return true
        return true;
    }

    /**
     * Validates that all scan targets exist
     * 
     * @param listener
     *            BuildListener
     * @param channel
     *            VirtualChannel
     * @param scanTargets
     *            List<String>
     * 
     * @return
     * @throws IOException
     * @throws HubConfigurationException
     * @throws InterruptedException
     */
    public boolean validateScanTargets(BuildListener listener, VirtualChannel channel, List<String> scanTargets) throws IOException, HubConfigurationException,
            InterruptedException {
        for (String currTarget : scanTargets) {
            File locationFile = new File(currTarget);
            FilePath target = null;
            if (channel != null) {
                target = new FilePath(channel, locationFile.getCanonicalPath());
            } else {
                target = new FilePath(locationFile);
            }
            if (!target.getRemote().equals(getWorkingDirectory()) && !target.getRemote().contains(getWorkingDirectory())) {
                throw new HubConfigurationException("Can not scan targets outside of the workspace.");
            }

            if (!target.exists()) {
                throw new IOException("Scan target could not be found : " + target.getRemote());
            } else {
                listener.getLogger().println(
                        "[DEBUG] : Scan target exists at : " + target.getRemote());
            }
        }
        return true;
    }
}