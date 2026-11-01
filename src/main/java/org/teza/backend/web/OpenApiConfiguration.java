package org.teza.backend.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
class OpenApiConfiguration {
    @Bean
    OpenAPI api() {
        return new OpenAPI().info(new Info().title("teza API").version("v1"))
                .components(new Components().addSecuritySchemes("csrfToken", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN")
                        .description("After login, copy token from GET /api/v1/auth/csrf. Session cookie is also required.")));
    }

    @Bean
    OpenApiCustomizer csrfHeader() {
        var writes = Set.of(PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT,
                PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE);
        return api -> api.getPaths().values().forEach(path -> path.readOperationsMap().forEach((method, operation) -> {
            if (writes.contains(method)) {
                operation.addSecurityItem(new SecurityRequirement().addList("csrfToken"));
            }
        }));
    }
}
