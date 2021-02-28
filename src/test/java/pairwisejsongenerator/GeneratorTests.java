package pairwisejsongenerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import com.anyqn.lib.PairwiseJsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;
import provider.BaseDataArgumentsProvider;
import provider.TestData.Case;

@Slf4j
class GeneratorTests {

    public GeneratorTests() {
        // TODO Auto-generated constructor stub
    }

    @ParameterizedTest
    @DisplayName("Check mapping from source json metadata to result jsons")
    @ArgumentsSource(BaseDataArgumentsProvider.class)
    void testPairwiseVariants(final Case testCase) throws JsonProcessingException {
        final PairwiseJsonGenerator gen = new PairwiseJsonGenerator();

        final List<Map<String, Object>> generated = gen.generate(testCase.getSource(), testCase.getProperties());

//        ObjectMapper mapper = new ObjectMapper();
//        String testCaseJson = mapper.writeValueAsString(testCase);
//        String expected = mapper.writeValueAsString(generated);
//        log.info("Test case:\n{}\nGenerated json:\n{}", testCaseJson, expected);

        log.info("Generated jsons count {}", generated.size());
        assertThat(generated).as("Check all generated variants is excepted")
                .overridingErrorMessage("We have not allowed response").containsAll(testCase.getExpected());
    }

}
