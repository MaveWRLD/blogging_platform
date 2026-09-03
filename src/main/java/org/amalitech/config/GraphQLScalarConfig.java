package org.amalitech.config;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.*;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Configuration
public class GraphQLScalarConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(instantScalar());
    }

    private GraphQLScalarType instantScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("ISO 8601 instant in UTC (e.g. 2025-03-15T14:30:00Z or 2025-03-15T14:30:00.123Z)")
                .coercing(new Coercing<Instant, String>() {

                    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

                    @Override
                    public String serialize(@NonNull Object dataFetcherResult,
                                            @NonNull GraphQLContext graphQLContext,
                                            @NonNull Locale locale) throws CoercingSerializeException {

                        if (dataFetcherResult instanceof Instant instant) {
                            return formatter.format(instant);
                        }

                        throw new CoercingSerializeException(
                                "Expected Instant, got: " + dataFetcherResult.getClass().getName());
                    }

                    @Override
                    public Instant parseValue(@NonNull Object input,
                                              @NonNull GraphQLContext graphQLContext,
                                              @NonNull Locale locale) throws CoercingParseValueException {

                        if (input instanceof String inputString) {
                            try {
                                return Instant.from(formatter.parse(inputString));
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseValueException(
                                        "Invalid ISO-8601 instant format: " + inputString, e);
                            }
                        }

                        throw new CoercingParseValueException(
                                "Expected String input, got: " + input.getClass().getName());
                    }

                    @Override
                    public Instant parseLiteral(@NonNull Value<?> input,
                                                @NonNull CoercedVariables variables,
                                                @NonNull GraphQLContext context,
                                                @NonNull Locale locale) throws CoercingParseLiteralException {

                        if (input instanceof StringValue stringValue) {
                            String value = stringValue.getValue();
                            try {
                                return Instant.from(formatter.parse(value));
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseLiteralException(
                                        "Invalid ISO-8601 instant literal: " + value, e);
                            }
                        }

                        throw new CoercingParseLiteralException(
                                "Expected StringValue, got: " + input.getClass().getName());
                    }
                })
                .build();
    }
}