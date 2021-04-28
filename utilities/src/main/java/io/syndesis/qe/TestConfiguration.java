package io.syndesis.qe;

import io.syndesis.qe.image.Image;
import io.syndesis.qe.marketplace.openshift.OpenShiftConfiguration;
import io.syndesis.qe.marketplace.openshift.OpenShiftService;
import io.syndesis.qe.marketplace.openshift.OpenShiftUser;
import io.syndesis.qe.marketplace.quay.QuayUser;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestConfiguration {

    private static final String TEST_PROPERTIES_FILE = "syndesis.config.test.properties";

    public static final String SYNDESIS_VERSION = "syndesis.version";

    public static final String OPENSHIFT_URL = "syndesis.config.openshift.url";
    public static final String OPENSHIFT_TOKEN = "syndesis.config.openshift.token";
    public static final String OPENSHIFT_NAMESPACE = "syndesis.config.openshift.namespace";
    public static final String OPENSHIFT_SAR_NAMESPACE = "syndesis.config.openshift.sar_namespace";
    public static final String OPENSHIFT_NAMESPACE_CLEANUP = "syndesis.config.openshift.namespace.cleanup";
    public static final String OPENSHIFT_ROUTE_SUFFIX = "syndesis.config.openshift.route.suffix";

    public static final String SYNDESIS_SINGLE_USER = "syndesis.config.single.user";
    public static final String SYNDESIS_ADMIN_USERNAME = "syndesis.config.admin.username";
    public static final String SYNDESIS_ADMIN_PASSWORD = "syndesis.config.admin.password";
    public static final String SYNDESIS_UI_USERNAME = "syndesis.config.ui.username";
    public static final String SYNDESIS_UI_PASSWORD = "syndesis.config.ui.password";
    public static final String SYNDESIS_UI_URL = "syndesis.config.ui.url";
    public static final String SYNDESIS_UI_BROWSER = "syndesis.config.ui.browser";
    public static final String SYNDESIS_URL_SUFFIX = "syndesis.config.url.suffix";
    public static final String SYNDESIS_CALLBACK_URL_SUFFIX = "syndesis.config.callbackUrlSuffix";
    public static final String SYNDESIS_ENABLE_TEST_SUPPORT = "syndesis.config.enableTestSupport";
    public static final String SYNDESIS_ENVIRONMENT_DELOREAN = "syndesis.config.environment.delorean";

    public static final String SYNDESIS_LOCAL_REST_URL = "syndesis.config.local.rest.url";

    public static final String SYNDESIS_CREDENTIALS_FILE = "syndesis.config.credentials.file";
    public static final String SYNDESIS_VERSIONS_FILE = "syndesis.config.versions.file";

    public static final String SYNDESIS_INSTALL_VERSION = "syndesis.config.install.version";
    public static final String SYNDESIS_CRD_URL = "syndesis.config.crd.url";
    public static final String SYNDESIS_OPERATOR_IMAGE = "syndesis.config.operator.image";
    public static final String SYNDESIS_CR_URL = "syndesis.config.cr.url";

    public static final String SYNDESIS_BUILD_PROPERTIES_URL = "syndesis.config.build.properties.url";
    public static final String SYNDESIS_DOCKER_REGISTRY = "syndesis.config.docker.registry";

    public static final String SYNDESIS_PULL_SECRET = "syndesis.config.pull.secret";
    public static final String SYNDESIS_PULL_SECRET_NAME = "syndesis.config.pull.secret.name";

    public static final String SYNDESIS_PUBLIC_OAUTH_PROXY_URL = "syndesis.config.template.public.oath.proxy.url";

    public static final String SYNDESIS_CUSTOM_RESOURCE_PLURAL = "syndesis.config.custom.resource.plural";

    public static final String SYNDESIS_NIGHTLY_VERSION = "syndesis.nightly.version";

    public static final String JENKINS_DELAY = "jenkins.delay";
    public static final String TESTSUITE_TIMEOUT = "syndesis.config.timeout";

    public static final String DB_ALLOCATOR_URL = "syndesis.dballocator.url";

    public static final String PROD_REPOSITORY = "syndesis.config.prod.repository";
    public static final String UPSTREAM_REPOSITORY = "syndesis.config.upstream.repository";

    public static final String SKIP_TESTS_WITH_OPEN_ISSUES = "syndesis.skip.open.issues";

    public static final String STATE_CHECK_INTERVAL = "syndesis.server.state.check.interval";
    public static final String SNOOP_SELECTORS = "syndesis.config.snoop.selectors";

    public static final String SYNDESIS_UPGRADE_PREVIOUS_VERSION = "syndesis.upgrade.previous.version";

    public static final String SYNDESIS_UPGRADE_CURRENT_VERSION = "syndesis.upgrade.current.version";

    public static final String SYNDESIS_UPGRADE_PREVIOUS_OPERATOR_IMAGE = "syndesis.upgrade.previous.operator";

    public static final String SYNDESIS_UPGRADE_PREVIOUS_CRD = "syndesis.upgrade.previous.crd";

    public static final String SYNDESIS_UPGRADE_PREVIOUS_CR = "syndesis.upgrade.previous.cr";

    private static final String BROWSER_BINARY_PATH = "syndesis.config.browser.path";

    private static final String CAMEL_K_VERSION = "syndesis.camelk.version";
    private static final String SYNDESIS_RUNTIME = "syndesis.config.runtime";

    private static final String JAEGER_VERSION = "syndesis.jaeger.version";
    private static final String CAMEL_VERSION = "camel.version";

    private static final String SYNDESIS_APPEND_REPOSITORY = "syndesis.config.append.repository";

    private static final String APP_MONITORING_VERSION = "syndesis.config.monitoring.version";

    private static final String QUAY_USERNAME = "syndesis.config.quay.username";
    private static final String QUAY_PASSWORD = "syndesis.config.quay.password";
    private static final String INDEX_IMAGE = "syndesis.config.index.image";
    private static final String BUNDLE_IMAGE = "syndesis.config.bundle.image";
    private static final String BUNDLE_ICSP_FILE = "syndesis.config.bundle.icsp.file.url";
    private static final String BUNDLE_REPOSITORY = "syndesis.config.bundle.repository.name";
    private static final String BUNDLE_REPOSITORY_USERNAME = "syndesis.config.bundle.repository.username";
    private static final String BUNDLE_REPOSITORY_PASSWORD = "syndesis.config.bundle.repository.password";

    private static final String KEYCLOAK_NAMESPACE = "keycloak.namespace";
    private static final String KEYCLOAK_SYNDESIS_REALM = "keycloak.syndesis.realm";
    private static final String KEYCLOAK_IDP_NAME = "keycloak.idp.name";

    private static final TestConfiguration INSTANCE = new TestConfiguration();

    private final Properties properties = new Properties();

    private TestConfiguration() {
        try {
            // first let's try properties in module dir
            copyValues(fromPath("test.properties"), true);

            // then properties in repo root
            copyValues(fromPath("../" + System.getProperty(TEST_PROPERTIES_FILE, "test.properties")));

            // build properties file
            copyValues(fromPath(System.getProperty(SYNDESIS_BUILD_PROPERTIES_URL)));

            // then system variables
            copyValues(System.getProperties());

            // then environment variables
            // TODO: copyValues(fromEnvironment());

            // then defaults
            copyValues(defaultValues());
        } catch (Exception e) {
            // If it fails here, then the user doesn't get any meaningful error other than:
            // java.lang.NoClassDefFoundError: Could not initialize class io.syndesis.qe.TestConfiguration
            log.error(
                "Error while creating TestConfiguration class! Please check if all mandatory properties are set" +
                    " (either in test.properties or via the command line)");
            e.printStackTrace();
            throw e;
        }
    }

    public static TestConfiguration get() {
        return INSTANCE;
    }

    public static String syndesisVersion() {
        return get().readValue(SYNDESIS_VERSION);
    }

    public static String openShiftUrl() {
        return get().readValue(OPENSHIFT_URL);
    }

    public static String openShiftToken() {
        return get().readValue(OPENSHIFT_TOKEN);
    }

    public static String openShiftNamespace() {
        return get().readValue(OPENSHIFT_NAMESPACE);
    }

    // Namespace for the subject access review. If unspecified, will use the same namespace as for the deployment.
    public static String openShiftSARNamespace() {
        return get().readValue(OPENSHIFT_SAR_NAMESPACE, get().readValue(OPENSHIFT_NAMESPACE));
    }

    public static String openShiftRouteSuffix() {
        if (get().readValue(OPENSHIFT_ROUTE_SUFFIX) == null) {
            final String prefix = "apps.";
            if (get().readValue(SYNDESIS_UI_URL) != null && get().readValue(SYNDESIS_UI_URL).contains("syndesis.my-minishift.syndesis.io")) {
                //for jenkins
                get().overrideProperty(OPENSHIFT_ROUTE_SUFFIX, "my-minishift.syndesis.io");
            } else if (openShiftUrl().endsWith("6443")) {
                //OCP 4.x
                if (openShiftUrl().contains("https://api.crc.testing:6443")) {
                    //CRC
                    get().overrideProperty(OPENSHIFT_ROUTE_SUFFIX, "apps-crc.testing");
                } else {
                    get().overrideProperty(OPENSHIFT_ROUTE_SUFFIX, prefix + StringUtils.substringBetween(openShiftUrl(), "https://api.", ":6443"));
                }
            } else {
                //OCP 3.11
                String port = Arrays.asList(openShiftUrl().split(":")).stream().filter(part -> part.contains("443")).findFirst().orElse("8443");
                if (openShiftUrl().matches("https:\\/\\/(\\d{1,4}\\.){3}\\d{1,4}:" + port)) {
                    //minishift
                    get().overrideProperty(OPENSHIFT_ROUTE_SUFFIX, StringUtils.substringBetween(openShiftUrl(), "https://", ":" + port) + ".nip.io");
                } else {
                    //remote instance
                    get().overrideProperty(OPENSHIFT_ROUTE_SUFFIX,
                        prefix + StringUtils.substringBetween(openShiftUrl(), "https://master.", ":" + port));
                }
            }
        }
        return get().readValue(OPENSHIFT_ROUTE_SUFFIX);
    }

    public static String syndesisUsername() {
        return get().readValue(SYNDESIS_UI_USERNAME);
    }

    public static String syndesisPassword() {
        return get().readValue(SYNDESIS_UI_PASSWORD);
    }

    public static String adminUsername() {
        return get().readValue(SYNDESIS_ADMIN_USERNAME);
    }

    public static String adminPassword() {
        return get().readValue(SYNDESIS_ADMIN_PASSWORD);
    }

    public static String syndesisUrl() {
        if (get().readValue(SYNDESIS_UI_URL) == null) {
            // If the UI URL isn't set, use the same style as the default route created by the operator
            // "https://syndesis-<namespace>.apps.<cluster>"
            final String prefix = "https://syndesis-" + openShiftNamespace() + ".";
            get().overrideProperty(SYNDESIS_UI_URL, prefix + openShiftRouteSuffix());
        }
        return get().readValue(SYNDESIS_UI_URL);
    }

    public static String syndesisCallbackUrlSuffix() {
        return get().readValue(SYNDESIS_CALLBACK_URL_SUFFIX);
    }

    public static String syndesisLocalRestUrl() {
        return get().readValue(SYNDESIS_LOCAL_REST_URL);
    }

    public static String syndesisCredentialsFile() {
        return get().readValue(SYNDESIS_CREDENTIALS_FILE);
    }

    public static String syndesisVersionsFile() {
        return get().readValue(SYNDESIS_VERSIONS_FILE);
    }

    public static String syndesisPublicOauthProxyTemplateUrl() {
        return get().readValue(SYNDESIS_PUBLIC_OAUTH_PROXY_URL);
    }

    public static String syndesisPullSecret() {
        return get().readValue(SYNDESIS_PULL_SECRET);
    }

    public static String syndesisPullSecretName() {
        return get().readValue(SYNDESIS_PULL_SECRET_NAME);
    }

    public static boolean enableTestSupport() {
        return Boolean.parseBoolean(get().readValue(SYNDESIS_ENABLE_TEST_SUPPORT, "false"));
    }

    public static boolean isDeloreanEnvironment() {
        return Boolean.parseBoolean(get().readValue(SYNDESIS_ENVIRONMENT_DELOREAN));
    }

    public static String syndesisCrdUrl() {
        return get().readValue(SYNDESIS_CRD_URL);
    }

    public static String syndesisOperatorImage() {
        return get().readValue(SYNDESIS_OPERATOR_IMAGE);
    }

    public static String syndesisCrUrl() {
        return get().readValue(SYNDESIS_CR_URL);
    }

    public static String syndesisBrowser() {
        return get().readValue(SYNDESIS_UI_BROWSER);
    }

    public static boolean namespaceCleanup() {
        return Boolean.parseBoolean(get().readValue(OPENSHIFT_NAMESPACE_CLEANUP));
    }

    public static String customResourcePlural() {
        return get().readValue(SYNDESIS_CUSTOM_RESOURCE_PLURAL);
    }

    public static int getJenkinsDelay() {
        return Integer.parseInt(get().readValue(JENKINS_DELAY, "1"));
    }

    public static int getConfigTimeout() {
        return Integer.parseInt(get().readValue(TESTSUITE_TIMEOUT, "30"));
    }

    public static String getDbAllocatorUrl() {
        return get().readValue(DB_ALLOCATOR_URL, "localhost:8080");
    }

    public static String prodRepository() {
        return get().readValue(PROD_REPOSITORY);
    }

    public static String upstreamRepository() {
        return get().readValue(UPSTREAM_REPOSITORY);
    }

    public static Boolean skipTestsWithOpenIssues() {
        return Boolean.parseBoolean(get().readValue(SKIP_TESTS_WITH_OPEN_ISSUES));
    }

    public static int stateCheckInterval() {
        return Integer.parseInt(get().readValue(STATE_CHECK_INTERVAL));
    }

    public static boolean snoopSelectors() {
        return Boolean.parseBoolean(get().readValue(SNOOP_SELECTORS));
    }

    public static String image(Image image) {
        return get().readValue(image.name());
    }

    public static String camelKVersion() {
        return get().readValue(CAMEL_K_VERSION);
    }

    public static String jaegerVersion() {
        return get().readValue(JAEGER_VERSION);
    }

    public static String syndesisInstallVersion() {
        return get().readValue(SYNDESIS_INSTALL_VERSION);
    }

    public static String syndesisNightlyVersion() {
        return get().readValue(SYNDESIS_NIGHTLY_VERSION);
    }

    public static String upgradePreviousVersion() {
        return get().readValue(SYNDESIS_UPGRADE_PREVIOUS_VERSION);
    }

    public static String upgradeCurrentVersion() {
        return get().readValue(SYNDESIS_UPGRADE_CURRENT_VERSION);
    }

    public static String upgradePreviousOperatorImage() {
        return get().readValue(SYNDESIS_UPGRADE_PREVIOUS_OPERATOR_IMAGE);
    }

    public static String upgradePreviousCrd() {
        return get().readValue(SYNDESIS_UPGRADE_PREVIOUS_CRD);
    }

    public static String upgradePreviousCr() {
        return get().readValue(SYNDESIS_UPGRADE_PREVIOUS_CR);
    }

    public static String syndesisRuntime() {
        return get().readValue(SYNDESIS_RUNTIME);
    }

    public static boolean appendRepository() {
        return Boolean.parseBoolean(get().readValue(SYNDESIS_APPEND_REPOSITORY, "false"));
    }

    public static String appMonitoringVersion() {
        return get().readValue(APP_MONITORING_VERSION, "v1.1.6");
    }

    public static boolean isSingleUser() {
        return Boolean.parseBoolean(get().readValue(SYNDESIS_SINGLE_USER, "false"));
    }

    public static String quayUsername() {
        return get().readValue(QUAY_USERNAME);
    }

    public static String quayPassword() {
        return get().readValue(QUAY_PASSWORD);
    }

    public static String getIndexImage() {
        return get().readValue(INDEX_IMAGE);
    }

    public static String getBundleImage() {
        return get().readValue(BUNDLE_IMAGE);
    }

    public static String getBundleRepository() {
        return get().readValue(BUNDLE_REPOSITORY);
    }

    public static String getBundleRepositoryUsername() {
        return get().readValue(BUNDLE_REPOSITORY_USERNAME);
    }

    public static String getBundleRepositoryPassword() {
        return get().readValue(BUNDLE_REPOSITORY_PASSWORD);
    }

    public static String getIcspFile() {
        return get().readValue(BUNDLE_ICSP_FILE);
    }

    public static String keycloakNamespace() {
        return get().readValue(KEYCLOAK_NAMESPACE);
    }

    public static String keyCloakSyndesisRealm() {
        return get().readValue(KEYCLOAK_SYNDESIS_REALM);
    }

    public static String keyCloakIdpName() {
        return get().readValue(KEYCLOAK_IDP_NAME);
    }

    private Properties defaultValues() {
        final Properties defaultProps = new Properties();

        defaultProps.setProperty(OPENSHIFT_URL, "");
        defaultProps.setProperty(OPENSHIFT_TOKEN, "");
        defaultProps.setProperty(SYNDESIS_LOCAL_REST_URL, "http://localhost:8080");

        defaultProps.setProperty(SYNDESIS_CREDENTIALS_FILE, "../credentials.json");
        defaultProps.setProperty(SYNDESIS_VERSIONS_FILE, "src/test/resources/dependencyVersions.properties");

        defaultProps.setProperty(SYNDESIS_UI_BROWSER, "chrome");

        defaultProps.setProperty(OPENSHIFT_NAMESPACE_CLEANUP, "false");

        // to keep backward compatibility
        if (properties.getProperty(SYNDESIS_URL_SUFFIX) != null && properties.getProperty(OPENSHIFT_ROUTE_SUFFIX) == null) {
            defaultProps.setProperty(OPENSHIFT_ROUTE_SUFFIX, properties.getProperty(SYNDESIS_URL_SUFFIX));
        }

        String syndesisVersion;
        if (System.getProperty(SYNDESIS_INSTALL_VERSION) != null) {
            syndesisVersion = System.getProperty(SYNDESIS_INSTALL_VERSION);
        } else {
            syndesisVersion = "master";
            // only use defined system property if it doesnt end with SNAPSHOT and it is not prod build
            if (!properties.getProperty(SYNDESIS_VERSION).endsWith("SNAPSHOT") && !properties.getProperty(SYNDESIS_VERSION).contains("redhat")) {
                syndesisVersion = System.getProperty(SYNDESIS_VERSION);
            }
        }

        if (properties.getProperty(SYNDESIS_PUBLIC_OAUTH_PROXY_URL) == null) {
            defaultProps.setProperty(SYNDESIS_PUBLIC_OAUTH_PROXY_URL, String
                .format("https://raw.githubusercontent.com/syndesisio/syndesis/%s/install/support/syndesis-public-oauth-proxy.yml", syndesisVersion));
        }

        defaultProps.setProperty(SYNDESIS_CRD_URL, getClass().getClassLoader().getResource("syndesis-crd.yaml").toString());
        String operatorVersion;
        if (this.properties.getProperty(SYNDESIS_INSTALL_VERSION) != null) {
            operatorVersion = this.properties.getProperty(SYNDESIS_INSTALL_VERSION);
        } else {
            operatorVersion = "latest";
        }

        if (properties.getProperty(SYNDESIS_DOCKER_REGISTRY) == null) {
            defaultProps.setProperty(SYNDESIS_DOCKER_REGISTRY, "quay.io");
            defaultProps.setProperty(SYNDESIS_OPERATOR_IMAGE,
                String.format("%s/syndesis/syndesis-operator:%s", defaultProps.get(SYNDESIS_DOCKER_REGISTRY), operatorVersion));
        } else {
            defaultProps.setProperty(SYNDESIS_OPERATOR_IMAGE,
                String.format("%s/syndesis/syndesis-operator:%s", properties.get(SYNDESIS_DOCKER_REGISTRY), operatorVersion));
        }

        if (properties.getProperty(SYNDESIS_CR_URL) == null) {
            defaultProps.setProperty(SYNDESIS_CR_URL, "https://raw.githubusercontent.com/syndesisio/fuse-online-install/1.12.x/default-cr.yml");
        }

        defaultProps.setProperty(SYNDESIS_CUSTOM_RESOURCE_PLURAL, "syndesises");

        // When the single user property is set (for the env where the syndesis is already deployed and you are not an admin)
        // Make the user "admin" anyway, as that user is used in all k8s client invocations by default
        if (Boolean.parseBoolean(properties.getProperty(SYNDESIS_SINGLE_USER, "false"))) {
            //need to be in the main properties because it will be used for xtf settings
            properties.setProperty(SYNDESIS_ADMIN_USERNAME, properties.getProperty(SYNDESIS_UI_USERNAME));
            properties.setProperty(SYNDESIS_ADMIN_PASSWORD, properties.getProperty(SYNDESIS_UI_PASSWORD));
        }

        if (!Boolean.parseBoolean(String.valueOf(properties.get(SYNDESIS_ENVIRONMENT_DELOREAN)))) {
            // Copy syndesis properties to their xtf counterparts - used by binary oc client
            System.setProperty("xtf.openshift.url", properties.getProperty(OPENSHIFT_URL));
            System.setProperty("xtf.openshift.master.username", properties.getProperty(SYNDESIS_ADMIN_USERNAME));
            System.setProperty("xtf.openshift.master.password", properties.getProperty(SYNDESIS_ADMIN_PASSWORD));
        } else {
            // get openshift url from the default client which use KUBECONFIG
            DefaultKubernetesClient defaultKubernetesClient = new DefaultKubernetesClient();
            defaultProps.setProperty(OPENSHIFT_URL, defaultKubernetesClient.getMasterUrl().toString());
            System.setProperty("xtf.openshift.url", defaultKubernetesClient.getMasterUrl().toString());
            log.info("Kubernetes master URL: " + defaultKubernetesClient.getMasterUrl().toString());
            defaultKubernetesClient.close();
        }
        System.setProperty("xtf.openshift.namespace", properties.getProperty(OPENSHIFT_NAMESPACE));

        // Set oc version - this version of the client will be used as the binary client
        System.setProperty("xtf.openshift.version", "4.3");

        // Set jackson properties for productized build
        System.setProperty("jackson.deserialization.whitelist.packages", "io.syndesis.common.model,io.atlasmap");

        /*
         * Resteasy 4.4 uses 2.10 jackson that has this security feature due to CVEs and the only option is to use system properties for that
         * Needed for atlasmap to be able to deserialize inspection response
         */
        System.setProperty("resteasy.jackson.deserialization.whitelist.allowIfBaseType.prefix", "*");
        System.setProperty("resteasy.jackson.deserialization.whitelist.allowIfSubType.prefix", "*");

        if (properties.getProperty(SYNDESIS_RUNTIME) == null) {
            defaultProps.setProperty(SYNDESIS_RUNTIME, "springboot");
        }
        if (properties.getProperty(SYNDESIS_PULL_SECRET_NAME) == null) {
            defaultProps.setProperty(SYNDESIS_PULL_SECRET_NAME, "syndesis-pull-secret");
        }

        defaultProps.setProperty(STATE_CHECK_INTERVAL, "" + (System.getenv("WORKSPACE") != null ? 150 : 90));
        if (properties.getProperty(SNOOP_SELECTORS) == null) {
            defaultProps.setProperty(SNOOP_SELECTORS, "false");
        }

        if (properties.getProperty(CAMEL_K_VERSION) == null) {
            defaultProps.setProperty(CAMEL_K_VERSION, "0.3.4");
        }

        if (properties.getProperty(JAEGER_VERSION) == null) {
            defaultProps.setProperty(JAEGER_VERSION, "1.20.0");
        }

        if (properties.getProperty(SYNDESIS_APPEND_REPOSITORY) == null) {
            defaultProps.setProperty(SYNDESIS_APPEND_REPOSITORY, "true");
        }

        if (properties.getProperty(KEYCLOAK_NAMESPACE) == null) {
            defaultProps.setProperty(KEYCLOAK_NAMESPACE, "keycloak");
        }

        if (properties.getProperty(KEYCLOAK_SYNDESIS_REALM) == null) {
            defaultProps.setProperty(KEYCLOAK_SYNDESIS_REALM, "syndesis");
        }

        if (properties.getProperty(KEYCLOAK_IDP_NAME) == null) {
            defaultProps.setProperty(KEYCLOAK_IDP_NAME, "OpenID_keycloak");
        }
        return defaultProps;
    }

    public String readValue(final String key) {
        return readValue(key, null);
    }

    public String readValue(final String key, final String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }

    private Properties fromPath(final String path) {
        final Properties props = new Properties();

        if (path == null) {
            return props;
        }

        final Path propsPath;
        if (path.startsWith("file:")) {
            propsPath = Paths.get(URI.create(path));
        } else {
            propsPath = Paths.get(path).toAbsolutePath();
        }

        InputStream is = null;

        try {
            if (path.startsWith("http")) {
                is = new URL(path).openStream();
            } else if (Files.isReadable(propsPath)) {
                is = Files.newInputStream(propsPath);
            }

            if (is != null) {
                log.debug("Loading properties from " + path);
                props.load(is);
                is.close();
            }
        } catch (IOException e) {
            log.warn("Unable to read properties from " + propsPath, e);
        }

        return props;
    }

    private void copyValues(final Properties source) {
        copyValues(source, false);
    }

    private void copyValues(final Properties source, final boolean overwrite) {
        source.stringPropertyNames().stream()
            .filter(key -> overwrite || !this.properties.containsKey(key))
            .forEach(key -> this.properties.setProperty(key, source.getProperty(key)));
    }

    public static Optional<String> browserBinary() {
        return Optional.ofNullable(get().readValue(BROWSER_BINARY_PATH));
    }

    /**
     * Overrides the property held in test configuration.
     *
     * @param key key
     * @param value value
     */
    public void overrideProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * Clears the property from test configuration.
     *
     * @param key key
     */
    public void clearProperty(String key) {
        properties.remove(key);
    }

    public static QuayUser getQuayUser() {
        return new QuayUser(
            TestConfiguration.quayUsername(),
            TestConfiguration.quayPassword()
        );
    }

    public static OpenShiftService getOpenShiftService(String quayProject) {
        OpenShiftUser defaultUser = new OpenShiftUser(
            syndesisUsername(),
            syndesisPassword(),
            openShiftUrl()
        );
        OpenShiftUser adminUser = new OpenShiftUser(
            adminUsername(),
            adminPassword(),
            openShiftUrl()
        );
        OpenShiftConfiguration openShiftConfiguration = OpenShiftConfiguration.builder()
            .namespace(openShiftNamespace())
            .dockerUsername(getBundleRepositoryUsername())
            .dockerPassword(getBundleRepositoryPassword())
            .dockerRegistry(getBundleRepository())
            .icspFile(getIcspFile())
            .build();
        return new OpenShiftService(
            openShiftConfiguration,
            adminUser,
            defaultUser
        );
    }
}
