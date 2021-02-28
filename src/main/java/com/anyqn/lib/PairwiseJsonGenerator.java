/*******************************************************************************
 * Copyright 2021 Renat Eskenin
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.anyqn.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.abslab.lib.pairwise.gen.PairwiseGenerator;
import com.anyqn.lib.Properties.Restriction;

import lombok.NonNull;

public class PairwiseJsonGenerator implements IMapsGenerator {

    private static final String FOR_FIELD_S_NEED_LIST_OF_POSSIBLE_VALUES = "For field [%s] need list of possible values";
    private static final String FOR_FIELD_S_NEED_STRING_NAME = "For field [%s] need string name";
    private static final String GENERATED_VARIANTS_NUMBER_LESS_THAN_ALLOWED_FOR = "Generated variants number less than allowed for";
    private static final String NEED_PROPERTIES_IN_JSON_DESCRIPTION = "Need properties in json description";
    private static final String NEED_OBJECTS_ARRAY = "Need objects array";
    private static final String NEED_MIN_RESTRICTIONS_ARRAY = "Need min restrictions array";
    private static final String NEED_MAX_RESTRICTIONS_ARRAY = "Need max restrictions array";
    private static final String VARIANTS_GENERATION_VALIDATION_MESSAGE = "Already generated variants";
    private static final String VARIANTS_COUNT_VALIDATION_MESSAGE = "We have more than one variant for object field";
    private static final String DOT = ".";
    private static final String FIELD_VALIDATION_MESSAGE = "Illegal field [%s], need fields name length more than zero and fields names can't contans dots";
    private static final String POSITION = ".position.";
    private static final String POSITION_D = "^(.*)(\\.position\\.)(\\d)$";
    private static final String MIN_RESTRICTION_LESS_THAN_ZERO = "Min restriction less than zero";
    private static final String DUPLICATE_MIN_VALUE = "Duplicate min value";
    private static final String MAX_RESTRICTION_LESS_THAN_ZERO = "Max restriction less than zero";
    private static final String DUPLICATE_MAX_VALUE = "Duplicate max value";
    private static final Pattern variantPattern = Pattern.compile(POSITION_D);

    @Override
    public List<Map<String, Object>> generate(@NonNull final Map<String, List<Object>> jsonMetadata,
            final Properties properties) {
        validate(properties);
        /*
         * Unoptimaly solution This possible because we have deduplication of possible
         * values added to variants, as example. Source possible variants
         * fieldC.position.0 = [-1,0] fieldD.position.0 = [ 0,1,2] fieldD.position.1 =
         * [-1,0,1,2] //Generated cases fieldC.position.0 = [-1, *
         * 0*,-1,0,-1,0,-1,*0*,-1,0,-1,0], fieldD.position.0 = [ 0, * 1*, 2,0, 1,2,
         * 0,*1*, 2,0, 1,2], fieldD.position.1 = [-1, *-1*,-1,0, 0,0, 1,*1*, 1,2, 2,2]
         *
         * Because we have deduplication this test will be converted to (when we will
         * add objects to test case in collapseObjectsFields) 011 -> 01-1 but we already
         * have this test, so generated 12 test cases, but deduplication will return 11
         **/
        return generateInternal(jsonMetadata, properties, null).parallelStream().distinct()
                .collect(Collectors.toList());
    }

    private static void validate(final Properties properties) {
        Validate.isTrue(!Objects.isNull(properties), NEED_PROPERTIES_IN_JSON_DESCRIPTION);
        Validate.isTrue(!Objects.isNull(properties.getMaxRestrictions()), NEED_MAX_RESTRICTIONS_ARRAY);
        Validate.isTrue(!Objects.isNull(properties.getMinRestrictions()), NEED_MIN_RESTRICTIONS_ARRAY);
        Validate.isTrue(!Objects.isNull(properties.getObjects()), NEED_OBJECTS_ARRAY);

        final Set<String> fieldPaths = new HashSet<>();

        properties.getMaxRestrictions().stream().forEach((final Restriction r) -> {
            Validate.isTrue(fieldPaths.add(r.getFieldPath()), DUPLICATE_MAX_VALUE, r.getFieldPath());
            Validate.isTrue(r.getValue() > 0, MAX_RESTRICTION_LESS_THAN_ZERO, r.getValue());
        });
        fieldPaths.clear();
        properties.getMinRestrictions().stream().forEach((final Restriction r) -> {
            Validate.isTrue(fieldPaths.add(r.getFieldPath()), DUPLICATE_MIN_VALUE, r.getFieldPath());
            Validate.isTrue(r.getValue() > 0, MIN_RESTRICTION_LESS_THAN_ZERO, r.getValue());
        });
    }

    private @NonNull List<Map<String, Object>> generateInternal(@NonNull final Map<String, List<Object>> jsonMetadata,
            @NonNull final Properties properties, final String rootPath) {

        final Map<String, List<Object>> params = new LinkedHashMap<>();
        final Map<String, List<Map<String, Object>>> variants = new HashMap<>();

        for (final Entry<String, List<Object>> entry : jsonMetadata.entrySet()) {

            List<Map<String, List<Object>>> objectDescriptions = entry.getValue().stream()
                    .map(PairwiseJsonGenerator::getMapObjectDescription).collect(Collectors.toList());
            if (objectDescriptions.stream().allMatch(o -> !Objects.isNull(o))) {
                calculateVariants(properties, rootPath, params, variants, entry.getKey(), objectDescriptions);
            } else {
                String key = entry.getKey();
                castField(key);
                params.put(key, entry.getValue());
            }

        }

        final PairwiseGenerator<String, Object> gen = new PairwiseGenerator<>(params);

        return generateResult(gen, Collections.unmodifiableMap(variants), properties);
    }

    /**
     * Calculate possible variants for objects for parent object
     *
     * @param properties         restrictions to generated result
     * @param rootPath           to the current calculated parent object
     * @param params             for filling to generate variants of parent object
     * @param variants           possible variants of child object
     * @param objectDescriptions field from parent object
     */
    private void calculateVariants(@NonNull final Properties properties, final String rootPath,
            @NonNull final Map<String, List<Object>> params,
            @NonNull final Map<String, List<Map<String, Object>>> variants, @NonNull final String childKey,
            @NonNull final List<Map<String, List<Object>>> objectDescriptions) {

        final String calculatedPath;
        if (Objects.isNull(rootPath)) {
            calculatedPath = childKey;
        } else {
            calculatedPath = rootPath + DOT + childKey;
        }

        for (final Map<String, List<Object>> object : objectDescriptions) {
            final List<Map<String, Object>> variantsList = generateInternal(object, properties, calculatedPath);

            if (variants.containsKey(calculatedPath)) {
                throw new IllegalStateException(VARIANTS_GENERATION_VALIDATION_MESSAGE);
            } else {
                variants.put(calculatedPath, Collections.unmodifiableList(variantsList));
            }
        }

        final List<Map<String, Object>> variantsForField = variants.get(calculatedPath);

        Validate.isTrue(variantsForField.size() >= properties.getMinRestriction(calculatedPath),
                GENERATED_VARIANTS_NUMBER_LESS_THAN_ALLOWED_FOR, calculatedPath);

        for (int i = 0; i < Math.min(variantsForField.size(), properties.getMaxRestriction(calculatedPath)); i++) {
            // -1 variant number is value can be skipt in result generation process
            params.put(calculatedPath + POSITION + i,
                    IntStream.range(i < properties.getMinRestriction(calculatedPath) ? 0 : -1, variantsForField.size())
                            .boxed().collect(Collectors.toList()));
        }
    }

    /**
     * Generate result object from generated possible test cases
     *
     * @param gen        Pairwise generator
     * @param variants   possible variants of child objects for some fields
     * @param properties
     * @return list of possible variants of generated object
     */
    private static List<Map<String, Object>> generateResult(@NonNull final PairwiseGenerator<String, Object> gen,
            @NonNull final Map<String, List<Map<String, Object>>> variants, @NonNull final Properties properties) {

        final List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 0; i < gen.getRowsCount(); i++) {
            final Map<String, Object> testCase = gen.getTestCase(i);
            collapseObjectsFields(variants, testCase, properties);
            result.add(testCase);
        }

        return result;

    }

    /**
     * Generate one field with list of possible variants of generated child object
     *
     * @param variants possible variants of child object
     * @param testCase generated test case with fields for collapsing
     */
    private static void collapseObjectsFields(@NonNull final Map<String, @NonNull List<Map<String, Object>>> variants,
            @NonNull final Map<String, Object> testCase, @NonNull final Properties properties) {
        addAllValuesToTestCase(testCase, properties, findAllValuesVariants(variants, testCase));
    }

    /**
     * Add to test cases list of possible variants (real values) for field or one
     * possible value for objects
     *
     * @param testCase             generated test case
     * @param properties
     * @param addedObjectsVariants generated possible values for fields
     */
    private static void addAllValuesToTestCase(final Map<String, Object> testCase, final Properties properties,
            final Map<String, List<Object>> addedObjectsVariants) {
        for (final Map.Entry<String, List<Object>> addedVariant : addedObjectsVariants.entrySet()) {
            // If this is object, but not list of values, just set first value from variants
            final String fieldName = addedVariant.getKey().substring(addedVariant.getKey().lastIndexOf(DOT) + 1);
            if (properties.isObject(addedVariant.getKey())) {
                final List<Object> variant = addedVariant.getValue();
                if (Objects.isNull(variant) || variant.isEmpty()) {
                    testCase.put(fieldName, null);
                } else if (variant.size() != 1) {
                    throw new IllegalStateException(VARIANTS_COUNT_VALIDATION_MESSAGE);
                } else {
                    testCase.put(fieldName, variant.get(0));
                }
            } else {
                testCase.put(fieldName, addedVariant.getValue());
            }
        }
    }

    /**
     * Convert all params described object variants for field value to list of
     * variants
     *
     * @param variants precalculated possible variants
     * @param testCase test cases with params descriptions as
     *                 fieldName.POSITION.[0...X]
     * @return
     */
    private static Map<String, List<Object>> findAllValuesVariants(
            final Map<String, List<Map<String, Object>>> variants, final Map<String, Object> testCase) {
        // Set of added variants for deduplication of added variants numbers (because we
        // have Pairwise theory method and in this method we do not need duplicates)
        final Map<String, Set<Integer>> addedVariants = new HashMap<>();
        // List of added objects as variants to test case
        final Map<String, List<Object>> addedObjectsVariants = new HashMap<>();

        for (final Iterator<Entry<String, Object>> fieldIterator = testCase.entrySet().iterator(); fieldIterator
                .hasNext();) {
            final Entry<String, Object> entry = fieldIterator.next();
            final Matcher matcher = variantPattern.matcher(entry.getKey());
            if (matcher.find()) {
                final String fieldName = matcher.group(1);
                final Integer variantNumber = (Integer) entry.getValue();
                // We will add empty list of possible values for all variants, because we need add
                // this field to target json may be with null value
                List<Object> possibleValues = addedObjectsVariants.computeIfAbsent(fieldName, k -> new ArrayList<>());
                // if -1 then we need just skip this variant
                if (variantNumber != -1
                        && addedVariants.computeIfAbsent(fieldName, k -> new HashSet<>()).add(variantNumber)) {
                    possibleValues.add(variants.get(fieldName).get(variantNumber));
                }
                fieldIterator.remove();
            }
        }

        return addedObjectsVariants;
    }

    /**
     * Assert object can be cast to Map with fields
     *
     * @param v
     * @return true if we can cast
     */
    private static Map<String, List<Object>> getMapObjectDescription(final Object v) {
        try {
            if (Objects.isNull(v)) {
                return null;
            }
            return castObjectDescription((Map<?, ?>) v);
        } catch (final ClassCastException e) {
            return null;
        }
    }

    /**
     * Convert Map to object description object
     *
     * @param mapValue map of fields and values describing object
     * @return
     */
    private static Map<String, List<Object>> castObjectDescription(final Map<?, ?> mapValue) {
        return mapValue.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(e -> castField(e.getKey()), e -> castValue(e.getValue())));

    }

    /**
     * Convert value to list of values
     *
     * @param v
     * @return list of possible values of field
     */
    private static List<Object> castValue(Object v) {
        if (v instanceof List) {
            return ((List<?>) v).stream().collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException(String.format(FOR_FIELD_S_NEED_LIST_OF_POSSIBLE_VALUES, v));
        }
    }

    /**
     * Convert object to field name
     *
     * @param k
     * @return firld name
     */
    private static String castField(Object k) {
        if (k instanceof String) {
            if (StringUtils.isBlank((String) k) || ((String) k).contains(DOT)) {
                throw new IllegalArgumentException(String.format(FIELD_VALIDATION_MESSAGE, k));
            } else {
                return (String) k;
            }
        } else {
            throw new IllegalArgumentException(String.format(FOR_FIELD_S_NEED_STRING_NAME, k));
        }
    }

}
