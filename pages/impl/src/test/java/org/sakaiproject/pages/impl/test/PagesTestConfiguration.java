/**
 * Copyright (c) 2003-2017 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.pages.impl.test;

import static org.mockito.Mockito.mock;

import org.sakaiproject.elfinder.SakaiFsService;
import org.sakaiproject.springframework.orm.hibernate.AdditionalHibernateMappings;
import org.sakaiproject.test.SakaiTestConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import lombok.Getter;

@Configuration
@EnableTransactionManagement
@ImportResource("classpath:/WEB-INF/components.xml")
@PropertySource("classpath:/hibernate.properties")
public class PagesTestConfiguration extends SakaiTestConfiguration {

    @Autowired
    @Qualifier("org.sakaiproject.springframework.orm.hibernate.impl.AdditionalHibernateMappings.pages")
    @Getter
    private AdditionalHibernateMappings additionalHibernateMappings;

    @Bean(name = "org.sakaiproject.elfinder.SakaiFsService")
    public SakaiFsService sakaiFsService() {
        return mock(SakaiFsService.class);
    }
}
