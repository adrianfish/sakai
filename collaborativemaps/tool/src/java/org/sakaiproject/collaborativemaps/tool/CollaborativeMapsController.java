package org.sakaiproject.collaborativemaps.tool;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.sakaiproject.collaborativemaps.api.model.CollaborativeMap;
import org.sakaiproject.collaborativemaps.api.persistence.CollaborativeMapRepository;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class CollaborativeMapsController {

    @Resource(name = "org.sakaiproject.collaborativemaps.api.persistence.CollaborativeMapRepository")
    private CollaborativeMapRepository repo;

    @RequestMapping(value = {"/", "/index"}, method = RequestMethod.GET)
    public String showIndex(Model model) {

        if (repo != null) {
            System.out.println("Found collaborativeMapRepository.");
            CollaborativeMap map = new CollaborativeMap();
            map.setSiteId("BALLS");
            map.setGroupId("BALLS");
            map.setName("BALLS");
            System.out.println("Before: " + map.toString());
            map = repo.save(map);
            System.out.println("After: " + map.toString());
            for (CollaborativeMap m : repo.findAll()) {
                System.out.println(m.toString());
            }
        } else {
            System.out.println("repo not set.");
        }
        return "index";
    }
}
