package com.neo4j.procedure_import;

import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CSVColsValidatorTest {

   @ParameterizedTest
   @MethodSource(value = "valid_csv_lines")
   void should_pass_validation_for_valid_lines(String line) {
      Assertions.assertThat(CSVColsValidator.INSTANCE.test(line))
            .isTrue();

   }

   @ParameterizedTest
   @MethodSource(value = "invalid_csv_lines")
   void should_fail_validation_for_different_reasons(String line, String errorDescription) {
      Assertions.assertThat(CSVColsValidator.INSTANCE.test(line))
            .describedAs(errorDescription)
            .isFalse();

   }

   static Stream<Arguments> valid_csv_lines() {
      return Stream.of(
            Arguments.of("2017-01-18 19:04:09,SMS,+212612345678,+33612345678"),
            Arguments.of("2017-01-18 19:04:09,CALL,+212612345678,+33612345678")
      );
   }

   static Stream<Arguments> invalid_csv_lines() {
      return Stream.of(
            Arguments.of("2017-01-18,SMS,+212612345678,+33612345678", "should_return_false_when_timestamp_is_not_well_formatted"),
            Arguments.of("2017-01-18 19:04:09,,+33612345678,+212612345678", "should_return_false_when_type_is_empty"),
            Arguments.of("2017-01-18 19:04:09,WhatsApp,01,02", "should_return_false_when_type_is_other_than_SMS_or_CALL"),
            Arguments.of("2017-01-18 19:04:09,SMS,,+33612345678", "should_return_false_when_from_is_empty"),
            Arguments.of("2017-01-18 19:04:09,CALL,from,02", "should_return_false_when_from_does_not_contain_valid_format"),
            Arguments.of("2017-01-18 19:04:09,SMS,01,", "should_return_false_when_to_is_empty"),
            Arguments.of("2017-01-18 19:04:09,SMS,+33612345678,to", "should_return_false_when_to_does_not_contain_valid_format"),
            Arguments.of("2017-01-18 19:04:09,SMS,,", "should_return_false_when_both_to_and_from_are_empty")
      );
   }

}