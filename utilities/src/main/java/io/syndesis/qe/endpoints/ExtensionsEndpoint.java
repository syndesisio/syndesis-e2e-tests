package io.syndesis.qe.endpoints;

import static org.assertj.core.api.Fail.fail;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import org.assertj.core.api.Fail;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Optional;

import io.syndesis.common.model.extension.Extension;

@Component
public class ExtensionsEndpoint extends AbstractEndpoint<Extension> {
    public ExtensionsEndpoint() {
        super(Extension.class, "/extensions");
    }

    public Extension uploadExtension(File extension) {
        MultipartFormDataOutput mpo = new MultipartFormDataOutput();
        try {
            mpo.addFormData("file", new FileInputStream(extension), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (FileNotFoundException e) {
            fail("Extension file not found", e);
        }

        Client client = ClientBuilder.newClient();
        final Invocation.Builder invocation = client
                .target(getEndpointUrl())
                .request(MediaType.MULTIPART_FORM_DATA_TYPE)
                .headers(COMMON_HEADERS);
        final JsonNode response = invocation.post(Entity.entity(mpo, MediaType.MULTIPART_FORM_DATA), JsonNode.class);

        return transformJsonNode(response, Extension.class);
    }

    public void installExtension(Extension e) {
        Optional<String> id = e.getId();
        Client client = ClientBuilder.newClient();
        final Invocation.Builder invocation = client
                .target(getEndpointUrl() + "/" +id.get() + "/install")
                .request(MediaType.APPLICATION_JSON)
                .headers(COMMON_HEADERS);
        invocation.post(Entity.entity(e.toString(), MediaType.APPLICATION_JSON), JsonNode.class);
    }
}
