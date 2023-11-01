package org.sakaiproject.pages.impl;

import org.sakaiproject.pages.api.PagesService;
import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.pages.api.repository.PageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class PagesServiceImpl implements PagesService {

    @Autowired PageRepository pageRepository;

    public void savePage(PageTransferBean bean) {

        pageRepository.save(bean.asPage());
    }
}
