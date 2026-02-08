package com.urfu.catbot.telegram.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class I18nConfig {

  @Bean
  public ResourceBundleMessageSource messageSource() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource();
    source.setBasename("i18n/messages");
    source.setDefaultEncoding("UTF-8");
    source.setDefaultLocale(java.util.Locale.forLanguageTag("fa"));
    return source;
  }
}
