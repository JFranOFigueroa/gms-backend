package com.gymback;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Punto de entrada cuando la aplicación se despliega como WAR en WildFly.
 * En local se sigue usando {@link GymBackApplication#main(String[])}.
 */
public class ServletInitializer extends SpringBootServletInitializer {
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(GymBackApplication.class);
  }
}
