package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.servlet.ServletException;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDRestException;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class PostBuildScanDescriptorTest {
    private static final String PASSWORD_WRONG = "Assert.failurePassword";

    private static final String USERNAME_NON_EXISTING = "Assert.failureUser";

    private static final String PROJECT_NAME_EXISTING = "Jenkins Hub Descriptor Test Project";

    private static final String PROJECT_NAME_NOT_EXISTING = "Assert Project Does Not Exist";

    private static final String PROJECT_RELEASE_EXISTING = "First Release";

    private static final String PROJECT_RELEASE_NOT_EXISTING = "Assert Release Does Not Exist";

    private static Properties testProperties;

    private static JenkinsHubIntTestHelper restHelper;

    private static PostBuildScanDescriptor descriptor;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public static JenkinsRule j = new JenkinsRule();

    @BeforeClass
    public static void init() throws Exception {
        testProperties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream("test.properties");
        try {
            testProperties.load(is);
        } catch (IOException e) {
            System.err.println("reading test.properties failed!");
        }
        String user = testProperties.getProperty("TEST_USERNAME");
        String pass = testProperties.getProperty("TEST_PASSWORD");
        String url = testProperties.getProperty("TEST_HUB_SERVER_URL");
        restHelper = new JenkinsHubIntTestHelper();
        restHelper.setBaseUrl(url);
        restHelper.setCookies(user, pass);
        projectCleanup();
    }

    public static void projectCleanup() {
        try {
            // This cleans up all the Projects that were created for the tests that may still be hanging around
            ArrayList<LinkedHashMap<String, Object>> responseList = restHelper.getProjectMatches(PROJECT_NAME_EXISTING);
            ArrayList<String> projectIds = restHelper.getProjectIdsFromProjectMatches(responseList, PROJECT_NAME_EXISTING);
            if (projectIds.size() > 0) {
                for (String projectId : projectIds) {
                    restHelper.deleteHubProject(projectId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConnectionNonExistentUrl() {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FormValidation form = descriptor.doTestConnection("http://ASSERTNONEXISTENTURL", credential.getId());
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertTrue(form.getMessage().contains("UnknownHostException"));

    }

    @Test
    public void testConnectionNonExistentUser() {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        FormValidation form = descriptor.doTestConnection(testProperties.getProperty("TEST_HUB_SERVER_URL"), "NONEXITENTCREDENTIAL");
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertTrue(form.getMessage().contains("User needs to specify which credentials to use."));

    }

    @Test
    public void testConnectionUnAuthorized() {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                USERNAME_NON_EXISTING, PASSWORD_WRONG);
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FormValidation form = descriptor.doTestConnection(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId());
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertTrue(form.getMessage().contains("Unauthorized (401) - Unauthorized"));

    }

    @Test
    public void testConnection() {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FormValidation form = descriptor.doTestConnection(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId());
        Assert.assertEquals(FormValidation.Kind.OK, form.kind);
        Assert.assertTrue(form.getMessage().contains("Credentials valid for: " + testProperties.getProperty("TEST_HUB_SERVER_URL")));

    }

    @Test
    public void testCreateProjectAndCheckForNameAndRelease() throws IOException, ServletException, BDRestException, InterruptedException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        try {
            FormValidation form = descriptor.doCreateHubProject(PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING);

            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());

            // wait 1.5 seconds before checking for the project and release
            Thread.sleep(3000);
            // Need to wait a second before checking if the project exists or it will not be recognized
            FormValidation form2 = descriptor.doCheckHubProjectName(PROJECT_NAME_EXISTING);
            Assert.assertEquals(FormValidation.Kind.OK, form2.kind);
            Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectExistsIn_0_(testProperties.getProperty("TEST_HUB_SERVER_URL")));

            FormValidation form3 = descriptor.doCheckHubProjectVersion(PROJECT_RELEASE_EXISTING, PROJECT_NAME_EXISTING);
            assertEquals(FormValidation.Kind.OK, form3.kind);
            assertTrue(form3.getMessage().contains(Messages.HubBuildScan_getVersionExistsIn_0_(null).substring(0, 42)));
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectId(PROJECT_NAME_EXISTING));
        }

    }

    @Test
    public void testCreateProjectDuplicateNameDifferentRelease() throws IOException, ServletException, BDRestException, InterruptedException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);
        try {
            Thread.sleep(3000);
            FormValidation form = descriptor.doCreateHubProject(PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING);
            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());
            Thread.sleep(3000);
            FormValidation form2 = descriptor.doCreateHubProject(PROJECT_NAME_EXISTING, "New Release");
            Assert.assertEquals(FormValidation.Kind.OK, form2.kind);
            Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectId(PROJECT_NAME_EXISTING));
        }
    }

    @Test
    public void testCreateProjectDuplicateRelease() throws IOException, ServletException, BDRestException, InterruptedException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        try {
            Thread.sleep(3000);
            FormValidation form = descriptor.doCreateHubProject(PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING);
            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
            Thread.sleep(3000);
            FormValidation form2 = descriptor.doCreateHubProject(PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING);
            Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
            Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionExist(), form2.getMessage());
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectId(PROJECT_NAME_EXISTING));
        }
    }

    @Test
    public void testCheckForProjectNameNotExistent() throws IOException, ServletException, BDRestException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        FormValidation form = descriptor.doCheckHubProjectName(PROJECT_NAME_NOT_EXISTING);
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNonExistingIn_0_(testProperties.getProperty("TEST_HUB_SERVER_URL")));
    }

    @Test
    public void testCheckForProjectReleaseNotExistent() throws IOException, ServletException, BDRestException, InterruptedException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        try {
            FormValidation form = descriptor.doCreateHubProject(PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING);
            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
            Thread.sleep(2000);
            FormValidation form2 = descriptor.doCheckHubProjectVersion(PROJECT_RELEASE_NOT_EXISTING, PROJECT_NAME_EXISTING);
            Assert.assertEquals(FormValidation.Kind.ERROR, form2.kind);
            Assert.assertTrue(form2.getMessage().contains(Messages.HubBuildScan_getVersionNonExistingIn_0_(null, null).substring(0, 52)));
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectId(PROJECT_NAME_EXISTING));
        }
    }

    @Test
    public void testCheckForProjectReleaseNotExistentProject() throws IOException, ServletException, BDRestException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        FormValidation form = descriptor.doCheckHubProjectVersion(PROJECT_RELEASE_EXISTING, PROJECT_NAME_EXISTING);
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertEquals(Messages.HubBuildScan_getProjectNonExistingOrTroubleConnecting_(), form.getMessage());
    }

    @Test
    public void testCheckInvalidMemory() throws IOException, ServletException, BDRestException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

        FormValidation form = descriptor.doCheckScanMemory("This is not an Integer");
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getInvalidMemoryString());
    }

    @Test
    public void testCheckNoMemory() throws IOException, ServletException, BDRestException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

        FormValidation form = descriptor.doCheckScanMemory("");
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getNeedMemory());
    }

    @Test
    public void testCheckValidMemory() throws IOException, ServletException, BDRestException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

        FormValidation form = descriptor.doCheckScanMemory("512");
        Assert.assertEquals(FormValidation.Kind.OK, form.kind);
    }

}
