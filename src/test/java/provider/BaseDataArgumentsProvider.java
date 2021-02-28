package provider;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import provider.TestData.Case;

public class BaseDataArgumentsProvider implements ArgumentsProvider {

    private final ObjectMapper mapper = new ObjectMapper();

    public BaseDataArgumentsProvider() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
        try {

            final TestData loadedData = mapper
                    .readValue(new URL("file:src/test/resources/pairwise-json-generation-data.json"), TestData.class);

            return loadedData.getTestCases().stream().map((final Case test) -> Arguments.of(test))
                    .collect(Collectors.toList()).stream();
        } catch (final IOException e) {
            throw new IllegalStateException(String.format("Unable to load json with tests data with exception %s", e));
        }
    }
}
