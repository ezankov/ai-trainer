package com.trainer.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ProfileRequestValidatorTest {

    private ProfileRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProfileRequestValidator();
    }

    // --- Heart rate validations ---

    @Test
    void shouldRejectWhenMaxHREqualsRestingHR() {
        CreateProfileRequest request = validRequestBuilder()
                .withRestingHR(60)
                .withMaxHR(60)
                .build();

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        assertThat(ex.getErrors()).hasSize(1);
        assertThat(ex.getErrors().get(0).field()).isEqualTo("maxHR");
        assertThat(ex.getErrors().get(0).message()).contains("greater than resting");
    }

    @Test
    void shouldRejectWhenMaxHRLessThanRestingHR() {
        CreateProfileRequest request = validRequestBuilder()
                .withRestingHR(80)
                .withMaxHR(100)  // valid range but <= restingHR won't happen here since 100 > 80
                .build();

        // This should pass - maxHR (100) > restingHR (80)
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldRejectWhenLthrLessThanOrEqualToRestingHR() {
        CreateProfileRequest request = validRequestBuilder()
                .withRestingHR(60)
                .withMaxHR(190)
                .withLthr(60)  // lthr == restingHR
                .build();

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        assertThat(ex.getErrors())
                .anyMatch(e -> e.field().equals("lthr") && e.message().contains("greater than resting"));
    }

    @Test
    void shouldRejectWhenLthrGreaterThanMaxHR() {
        CreateProfileRequest request = validRequestBuilder()
                .withRestingHR(50)
                .withMaxHR(180)
                .withLthr(200)  // lthr > maxHR
                .build();

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        assertThat(ex.getErrors())
                .anyMatch(e -> e.field().equals("lthr") && e.message().contains("less than or equal to max"));
    }

    @Test
    void shouldAcceptLthrEqualToMaxHR() {
        CreateProfileRequest request = validRequestBuilder()
                .withRestingHR(50)
                .withMaxHR(180)
                .withLthr(180)  // lthr == maxHR is valid
                .build();

        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldSkipLthrValidationWhenLthrIsNull() {
        CreateProfileRequest request = validRequestBuilder()
                .withRestingHR(50)
                .withMaxHR(180)
                .withLthr(null)
                .build();

        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    // --- Date of birth validations ---

    @Test
    void shouldRejectDateOfBirthInTheFuture() {
        CreateProfileRequest request = validRequestBuilder()
                .withDateOfBirth(LocalDate.now().plusDays(1))
                .build();

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        assertThat(ex.getErrors())
                .anyMatch(e -> e.field().equals("dateOfBirth") && e.message().contains("future"));
    }

    @Test
    void shouldRejectDateOfBirthResultingInAgeLessThan13() {
        CreateProfileRequest request = validRequestBuilder()
                .withDateOfBirth(LocalDate.now().minusYears(12))
                .build();

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        assertThat(ex.getErrors())
                .anyMatch(e -> e.field().equals("dateOfBirth") && e.message().contains("13"));
    }

    @Test
    void shouldAcceptDateOfBirthResultingInAgeExactly13() {
        CreateProfileRequest request = validRequestBuilder()
                .withDateOfBirth(LocalDate.now().minusYears(13))
                .build();

        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    // --- Race time ordering validations ---

    @Test
    void shouldRejectWhen10KLessThanOrEqualTo5K() {
        CreateProfileRequest request = validRequestBuilder()
                .withFiveKSeconds(1200)
                .withTenKSeconds(1200)  // equal
                .build();

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        assertThat(ex.getErrors())
                .anyMatch(e -> e.field().equals("tenKSeconds") && e.message().contains("greater than 5K"));
    }

    @Test
    void shouldRejectWhenHalfMarathonLessThanOrEqualTo10K() {
        CreateProfileRequest request = validRequestBuilder()
                .withTenKSeconds(2400)
                .withHalfMarathonSeconds(2400)  // equal
                .build();

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        assertThat(ex.getErrors())
                .anyMatch(e -> e.field().equals("halfMarathonSeconds") && e.message().contains("greater than 10K"));
    }

    @Test
    void shouldRejectWhenMarathonLessThanOrEqualToHalfMarathon() {
        CreateProfileRequest request = validRequestBuilder()
                .withHalfMarathonSeconds(5400)
                .withMarathonSeconds(5000)  // less than half
                .build();

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        assertThat(ex.getErrors())
                .anyMatch(e -> e.field().equals("marathonSeconds") && e.message().contains("greater than half-marathon"));
    }

    @Test
    void shouldSkipRaceTimeValidationWhenOneOfPairIsNull() {
        // 5K is null, 10K is provided — no validation between them
        CreateProfileRequest request = validRequestBuilder()
                .withFiveKSeconds(null)
                .withTenKSeconds(1200)
                .build();

        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldValidateNonAdjacentNullPairsIndependently() {
        // 5K null, 10K and Half both provided — validate 10K < Half
        CreateProfileRequest request = validRequestBuilder()
                .withFiveKSeconds(null)
                .withTenKSeconds(5000)
                .withHalfMarathonSeconds(4000)  // half < 10K — invalid
                .build();

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        assertThat(ex.getErrors())
                .anyMatch(e -> e.field().equals("halfMarathonSeconds"));
    }

    // --- Multiple errors collected ---

    @Test
    void shouldCollectMultipleValidationErrors() {
        CreateProfileRequest request = validRequestBuilder()
                .withRestingHR(100)
                .withMaxHR(100)  // maxHR == restingHR
                .withLthr(100)  // lthr == restingHR
                .withFiveKSeconds(2000)
                .withTenKSeconds(1000)  // 10K < 5K
                .build();

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        // Should have at least: maxHR error, lthr error, tenKSeconds error
        assertThat(ex.getErrors().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldPassValidRequest() {
        CreateProfileRequest request = validRequestBuilder().build();

        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldValidateUpdateRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                LocalDate.of(1990, 5, 15),
                new BigDecimal("72.5"),
                100,  // restingHR
                100,  // maxHR == restingHR — invalid
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        ProfileValidationException ex = catchThrowableOfType(
                () -> validator.validate(request), ProfileValidationException.class);

        assertThat(ex.getErrors())
                .anyMatch(e -> e.field().equals("maxHR"));
    }

    // --- Test helper ---

    private TestRequestBuilder validRequestBuilder() {
        return new TestRequestBuilder();
    }

    private static class TestRequestBuilder {
        private LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
        private BigDecimal weightKg = new BigDecimal("72.5");
        private Integer restingHR = 50;
        private Integer maxHR = 190;
        private Integer lthr = null;
        private Integer thresholdPaceSecondsPerKm = null;
        private BigDecimal vo2Max = null;
        private Integer fiveKSeconds = null;
        private Integer tenKSeconds = null;
        private Integer halfMarathonSeconds = null;
        private Integer marathonSeconds = null;

        TestRequestBuilder withDateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        TestRequestBuilder withRestingHR(Integer restingHR) {
            this.restingHR = restingHR;
            return this;
        }

        TestRequestBuilder withMaxHR(Integer maxHR) {
            this.maxHR = maxHR;
            return this;
        }

        TestRequestBuilder withLthr(Integer lthr) {
            this.lthr = lthr;
            return this;
        }

        TestRequestBuilder withFiveKSeconds(Integer fiveKSeconds) {
            this.fiveKSeconds = fiveKSeconds;
            return this;
        }

        TestRequestBuilder withTenKSeconds(Integer tenKSeconds) {
            this.tenKSeconds = tenKSeconds;
            return this;
        }

        TestRequestBuilder withHalfMarathonSeconds(Integer halfMarathonSeconds) {
            this.halfMarathonSeconds = halfMarathonSeconds;
            return this;
        }

        TestRequestBuilder withMarathonSeconds(Integer marathonSeconds) {
            this.marathonSeconds = marathonSeconds;
            return this;
        }

        CreateProfileRequest build() {
            return new CreateProfileRequest(
                    dateOfBirth, weightKg, restingHR, maxHR, lthr,
                    thresholdPaceSecondsPerKm, vo2Max,
                    fiveKSeconds, tenKSeconds, halfMarathonSeconds, marathonSeconds
            );
        }
    }
}
