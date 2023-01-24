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
package org.sakaiproject.service.gradebook.shared;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.entity.api.Entity;

import org.apache.commons.lang3.StringUtils;

@Slf4j
public class GradingReferenceReckoner {

    @Value
    public static class GradingReference {

        private Long item;
        private String student;
        private String tool;

        public String toString() {

            String ref = Entity.SEPARATOR + "grading" + Entity.SEPARATOR + item + Entity.SEPARATOR + student;
            if (StringUtils.isNotBlank(tool)) {
                ref += Entity.SEPARATOR + tool;
            }
            return ref;
        }

        public String getReference() {
            return toString();
        }
    }

    @Builder(builderMethodName = "reckoner", buildMethodName = "reckon")
    public static GradingReference newReckoner(String reference, Long item, String student, String tool) {

        if (StringUtils.isNotBlank(reference)) {
            String[] parts = reference.split(Entity.SEPARATOR);
            item = Long.parseLong(parts[2]);
            student = parts[3];
            if (parts.length > 4) {
                tool = parts[4];
            }
        }

        return new GradingReference(item, student, tool);
    }
}
