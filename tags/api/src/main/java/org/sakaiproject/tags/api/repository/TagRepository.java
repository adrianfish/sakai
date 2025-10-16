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

import org.sakaiproject.springframework.data.SpringCrudRepository;
import org.sakaiproject.tags.api.model.Tag;
import org.sakaiproject.tags.api.model.TagCollection;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends SpringCrudRepository<Tag, String> {

    List<Tag> findByCollection(String tagCollectionId);
    List<Tag> findByCollectionPaged(String tagCollectionId, Pageable pageable);
    List<Tag> findByLabel(String tagLabel);
    Optional<Tag> findByLabelAndCollection(String label, TagCollection collection);
    List<Tag> findByLabelLike(String pattern);
    List<Tag> findByLabelLikePaged(String pattern, Pageable pageable);
    List<Tag> findByExternalIdAndCollection(String externalId, String collectionId);
    List<Tag> findByCollectionAndLastModificationDateLessThan(String tagCollectionId, long lastModificationDate);
    void deleteByCollectionAndLastModificationDateLessThan(String tagCollectionId, long lastModificationDate);
    void deleteByExternalIdAndCollection(String externalId, String tagCollectionId);
    long countByLabelLike(String pattern);
    long countByCollection(String collectionId);
}
