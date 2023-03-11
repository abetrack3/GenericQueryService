package com.abetrack3.GenericQueryService.Controller;

import org.bson.Document;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class AppRuntimeConfiguration {

    private static final String CONFIGURATION_FILE_NAME = "app-settings.json";
    private static final String REGISTERED_SERVICES = "registeredServices";
    private static final String TRUSTED_ISSUERS = "trustedIssuers";
    private static final String TOKEN_CONFIGURATION = "token";
    private static final String JSON_WEB_KEY_HOST_SUFFIX = "jwkHostSuffix";

    private Document configuration;

    private static AppRuntimeConfiguration singleton;

    private AppRuntimeConfiguration() {
    }

    public static void resolve() {

        if (isConfigurationNotResolved()) {
            singleton = new AppRuntimeConfiguration();
            singleton.configuration = loadConfiguration();
        }

    }

    private static Document loadConfiguration() {

        File file = new File(CONFIGURATION_FILE_NAME);
        try (FileReader fileReader = new FileReader(file)){

            StringBuilder stringBuilder = new StringBuilder();
            while (fileReader.ready()) {

                stringBuilder.append((char)fileReader.read());

            }

            return Document.parse(stringBuilder.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isConfigurationNotResolved() {

        if (singleton == null) {
            return true;
        }

        return singleton.configuration == null;
    }

    public static Document getServiceInfo() {

        if (isConfigurationNotResolved()) {
            resolve();
        }
        return singleton.configuration.get(REGISTERED_SERVICES, Document.class);
    }

    public static List<String> getTrustedIssuers() {

        if (isConfigurationNotResolved()) {
            resolve();
        }

        return singleton.configuration
                .getList(TRUSTED_ISSUERS, String.class)
                .parallelStream()
                .map(item -> URI.create(item).getHost())
                .collect(Collectors.toUnmodifiableList());
    }

    public static String getJwkHostSuffix() {

        if (isConfigurationNotResolved()) {
            resolve();
        }

        return singleton
                .configuration
                .get(TOKEN_CONFIGURATION, Document.class)
                .getString(JSON_WEB_KEY_HOST_SUFFIX);
    }

}
