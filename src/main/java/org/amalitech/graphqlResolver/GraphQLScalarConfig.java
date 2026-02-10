package org.amalitech.graphqlResolver;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.*;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Configuration
public class GraphQLScalarConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(dateTimeScalar());
    }

    private GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("ISO 8601 DateTime (supports both local and UTC/Z formats)")
                .coercing(new Coercing<LocalDateTime, String>() {
                    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

                    @Override
                    public String serialize(@NonNull Object dataFetcherResult,
                                            @NonNull GraphQLContext graphQLContext,
                                            @NonNull Locale locale) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDateTime local) {
                            return local.format(formatter);
                        }
                        throw new CoercingSerializeException("Expected LocalDateTime");
                    }

                    @Override
                    public LocalDateTime parseValue(@NonNull Object input,
                                                    @NonNull GraphQLContext graphQLContext,
                                                    @NonNull Locale locale) throws CoercingParseValueException {
                        try {
                            if (input instanceof String s) {
                                return LocalDateTime.parse(s, formatter);
                            }
                            throw new CoercingParseValueException("Expected String");
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Invalid DateTime: " + input, e);
                        }
                    }

                    @Override
                    public LocalDateTime parseLiteral(@NonNull Value<?> input,
                                                      @NonNull CoercedVariables variables,
                                                      @NonNull GraphQLContext context,
                                                      @NonNull Locale locale) throws CoercingParseLiteralException {
                        if (input instanceof StringValue sv) {
                            try {
                                return LocalDateTime.parse(sv.getValue(), formatter);
                            } catch (Exception e) {
                                throw new CoercingParseLiteralException("Invalid DateTime format: " + sv.getValue(), e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected StringValue");
                    }
                })
                .build();
    }
}
