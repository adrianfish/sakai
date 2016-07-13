package org.sakaiproject.snappoll.tool.entityprovider;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;

import org.apache.commons.lang.StringUtils;

import lombok.Setter;

public class SnapPollEntityProvider extends AbstractEntityProvider implements AutoRegisterEntityProvider, Describeable, ActionsExecutable {
    
    public final static String ENTITY_PREFIX = "snap-poll";

    public String getEntityPrefix() {
        return ENTITY_PREFIX;
    }

    public String[] getHandledOutputFormats() {
        return new String[] { Formats.JSON };
    }

    @EntityCustomAction(action = "submitResponse", viewKey = EntityView.VIEW_NEW)
    public void handleSubmitResponse(EntityView view, Map<String, Object> params) {
        
        String userId = developerHelperService.getCurrentUserId();
        
        if (userId == null) {
            throw new EntityException("Not logged in", "", HttpServletResponse.SC_UNAUTHORIZED);
        }
        
        String siteId = (String) params.get("siteId");
        String tool = (String) params.get("tool");
        String option = (String) params.get("option");
        String context = (String) params.get("context");
        String comment = (String) params.get("comment");
        
        if (StringUtils.isEmpty(siteId) || StringUtils.isEmpty(tool)
                || StringUtils.isEmpty(option) || StringUtils.isEmpty(context) || StringUtils.isEmpty(comment)) {
            throw new EntityException("Bad request", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        System.out.println("siteId: " + siteId);
    }
}
