package com.veertuci.plugins.anka;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertuci.plugins.AnkaMgmtCloud;
import com.veertuci.plugins.anka.exceptions.AnkaHostException;
import hudson.Extension;
import hudson.model.*;
import hudson.model.Node.Mode;
import hudson.model.labels.LabelAtom;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

/**
 * Created by avia on 10/07/2016.
 */
public class AnkaCloudSlaveTemplate implements Describable<AnkaCloudSlaveTemplate> {

    protected static final SchemeRequirement HTTP_SCHEME = new SchemeRequirement("http");
    protected static final SchemeRequirement HTTPS_SCHEME = new SchemeRequirement("https");
    public static final String BridgedNetwork = "bridge";
    public static String SharedNetwork = "shared";
    public static String HostNetwork = "host";
    private static final Logger LOGGER = Logger.getLogger(AnkaCloudSlaveTemplate.class.getName());
    private final String capsuleNamePrefix;
    //private final List<String> masterImages;
    private final String masterVmId;
    private final String tag;
    private final int launchDelay;
    private final String remoteFS;
    private final String labelString;
    private final String templateDescription;
    private final int numberOfExecutors;
    private final Mode mode;
    private final String credentialsId;
    //    private final List<? extends NodeProperty<?>> nodeProperties;
    private transient Set<LabelAtom> labelSet;
    private final boolean keepAliveOnError;
    private final int SSHPort;
    private final String cloudName;

    @DataBoundConstructor
    public AnkaCloudSlaveTemplate(final String capsuleNamePrefix, final String remoteFS, final String masterVmId,
                                  final String tag,
                                  final String labelString, final String templateDescription,
                                  final int numberOfExecutors, final int launchDelay, final String credentialsId,
                                  boolean keepAliveOnError,
                                  int SSHPort, String cloudName) {
         this.capsuleNamePrefix = capsuleNamePrefix;
        this.remoteFS = remoteFS;
        this.labelString = labelString;
        this.templateDescription = templateDescription;
        this.numberOfExecutors = numberOfExecutors;
        this.masterVmId = masterVmId;
        this.tag = tag;
        // this.selectedMasterImage=selectedMasterImage;
        this.mode = Mode.EXCLUSIVE;
        this.credentialsId = credentialsId;
        this.launchDelay = launchDelay;
        this.keepAliveOnError = keepAliveOnError;
        this.SSHPort = SSHPort;
        this.cloudName = cloudName;
        readResolve();
    }

    protected Object readResolve() {
        this.labelSet = Label.parse(labelString);

        return this;
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    public static SchemeRequirement getHTTP_SCHEME() {
        return HTTP_SCHEME;
    }

    public static SchemeRequirement getHTTPS_SCHEME() {
        return HTTPS_SCHEME;
    }


    public boolean isKeepAliveOnError() {
        return keepAliveOnError;
    }


    public String getMasterVmId() {
        return masterVmId;
    }


    public String getTag() {
        return tag;
    }


    public String getCapsuleNamePrefix() {
        return capsuleNamePrefix;
    }

    public int getLaunchDelay() {
        return launchDelay;
    }

    public String getRemoteFS() {

        return remoteFS;
    }

    public String getLabelString() {
        return labelString;
    }

    public String getTemplateDescription() {
        return templateDescription;
    }

    public int getNumberOfExecutors() {
        return numberOfExecutors;
    }

    public Mode getMode() {
        return mode;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public Set<LabelAtom> getLabelSet() {
        return this.labelSet;
    }

    @Override
    public Descriptor<AnkaCloudSlaveTemplate> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());

    }

    public boolean isNetworkBridged() {
        return false;
    }

    public int getSSHPort() {
        return SSHPort;
    }

    public String getCloudName() {
        return cloudName;
    }


    /**
     *  ui stuff
     */

    @Extension
    public static final class DescriptorImpl extends Descriptor<AnkaCloudSlaveTemplate> {

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
            //return true;
        }

        @Override
        public String getDisplayName() {
            return null;
        }
        

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
            if (!(context instanceof AccessControlled ? (AccessControlled) context : Jenkins.getInstance()).hasPermission(Computer.CONFIGURE)) {
                return new ListBoxModel();
            }
            final List<StandardUsernameCredentials> credentials = lookupCredentials(StandardUsernameCredentials.class, context, ACL.SYSTEM, HTTP_SCHEME, HTTPS_SCHEME);
            return new StandardUsernameListBoxModel().withAll(credentials);
        }

        public List<AnkaVmTemplate> getClonableVms(AnkaMgmtCloud cloud) throws AnkaHostException {
            try {
                if (cloud != null) {
                    return cloud.listVmTemplates();
                }
                return new ArrayList<>();
            }
            catch (Exception e) {
                return new ArrayList<>();
            }
        }

        public List<String> getTemplateTags(AnkaMgmtCloud cloud, String masterVmId) throws AnkaHostException {
            if (cloud != null) {
                return cloud.getTemplateTags(masterVmId);
            }
            return new ArrayList<>();
        }

        public ListBoxModel doFillMasterVmIdItems(@QueryParameter String cloudName) {
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.getInstance().getCloud(cloudName);
            ListBoxModel models = new ListBoxModel();
            models.add("Choose Vm template", null);
            if (cloud != null) {
                for (AnkaVmTemplate temp: cloud.listVmTemplates()){
                    models.add(String.format("%s(%s)", temp.getName(), temp.getId()), temp.getId());
                }
            }
            return models;
        }

        public ListBoxModel doFillTagItems(@QueryParameter String cloudName , @QueryParameter String masterVmId) {
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.getInstance().getCloud(cloudName);
            ListBoxModel models = new ListBoxModel();
            models.add("Choose a Tag or leave empty for latest", null);
            if (cloud != null) {
                for (String tagName: cloud.getTemplateTags(masterVmId)){
                    models.add(tagName, tagName);
                }
            }
            return models;
        }

        public List<String> getNetworkConfigOptions(){
            return Arrays.asList(HostNetwork, SharedNetwork);
        }

    }


}
