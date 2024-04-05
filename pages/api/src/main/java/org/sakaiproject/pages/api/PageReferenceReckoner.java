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
package org.sakaiproject.pages.api;

import static org.sakaiproject.pages.api.PagesService.REFERENCE_ROOT;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.entity.api.Entity;

/**
 * Created by enietzel on 5/11/17.
 */

@Slf4j
public class PageReferenceReckoner {

    @Value
    public static class PageReference {

        private String siteId;
        private String id;

        @Override
        public String toString() {
            return REFERENCE_ROOT + Entity.SEPARATOR + siteId + Entity.SEPARATOR + id;
        }

        public String getReference() {
            return toString();
        }
    }

    /**
     * This is a builder for a PageReference
     *
     * @param page
     * @param siteId
     * @param id
     * @param reference
     * @return A PageReference
     */
    @Builder(builderMethodName = "reckoner", buildMethodName = "reckon")
    public static PageReference referenceReckoner(PageTransferBean page, String siteId, String id, String reference) {

        if (StringUtils.startsWith(reference, REFERENCE_ROOT)) {
            String[] parts = StringUtils.splitPreserveAllTokens(reference, Entity.SEPARATOR);
            if (siteId == null) siteId = parts[2];
            if (id == null) id = parts[3];
        } else if (page != null) {
            siteId = page.siteId;
            id = page.id;
        }

        return new PageReference(
            (siteId == null) ? "" : siteId,
            (id == null) ? "" : id);
    }
}
