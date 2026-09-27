package com.banking_microservices.customer_service_command.address;

import com.banking_microservices.customer_service_command.exception.AddressParsingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class LibpostalAddressParserClient implements AddressParserClient {
    private final RestClient restClient;

    public LibpostalAddressParserClient(RestClient.Builder builder,
                                        @Value("${customer-service.address-parser.base-url}") String baseUrl,
                                        @Value("${customer-service.address-parser.connect-timeout:2s}") Duration connectTimeout,
                                        @Value("${customer-service.address-parser.read-timeout:5s}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public ParsedAddress parse(String rawAddress, String countryCode) {
        try {
            LibpostalParseResponse response = restClient.post().uri("/v1/addresses/parse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LibpostalParseRequest(rawAddress, countryCode))
                    .retrieve().body(LibpostalParseResponse.class);
            if (response == null) throw new AddressParsingException("Address parser returned an empty response");
            return response.toParsedAddress();
        } catch (AddressParsingException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AddressParsingException("Address parser is unavailable or rejected the address", exception);
        }
    }
}
