package provider;

import java.util.List;
import java.util.Map;

import com.anyqn.lib.Properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class TestData {

    @NonNull
    private List<Case> testCases;

    @Data
    @NoArgsConstructor
    public static class Case {
        @NonNull
        private Properties properties;
        @NonNull
        private Map<String, List<Object>> source;
        @NonNull
        private List<Map<String, Object>> expected;
    }
}
