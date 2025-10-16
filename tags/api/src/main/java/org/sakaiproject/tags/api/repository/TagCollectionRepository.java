/*
 * Copyright (c) 2003-2021 The Apereo Foundation
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
package org.sakaiproject.tags.api.repository;

import java.util.List;
import java.util.Optional;

import org.sakaiproject.springframework.data.SpringCrudRepository;
import org.sakaiproject.tags.api.model.TagCollection;

import org.springframework.data.domain.Pageable;

public interface TagCollectionRepository extends SpringCrudRepository<TagCollection, String> {

    Optional<TagCollection> findByName(String name);
    List<TagCollection> findAllOrderByName();
    List<TagCollection> findAllPaginatedOrderByName(Pageable pageable);
    Optional<TagCollection> findByExternalSourceName(String externalSourceName);
}
