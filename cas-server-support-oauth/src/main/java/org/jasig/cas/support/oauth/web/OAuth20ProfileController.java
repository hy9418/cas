package org.jasig.cas.support.oauth.web;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.support.oauth.OAuthConstants;
import org.jasig.cas.ticket.TicketGrantingTicketImpl;
import org.jasig.cas.ticket.registry.TicketRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * This controller returns a profile for the authenticated user (identifier + attributes), found with the access token (CAS granting
 * ticket).
 * 
 * @author Jerome Leleu
 */
public class OAuth20ProfileController extends AbstractController {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuth20ProfileController.class);
    
    private TicketRegistry ticketRegistry;
    
    public OAuth20ProfileController(TicketRegistry ticketRegistry) {
        this.ticketRegistry = ticketRegistry;
    }
    
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        String accessToken = request.getParameter(OAuthConstants.ACCESS_TOKEN);
        logger.debug("accessToken : {}", accessToken);
        
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(response.getWriter());
        
        // accessToken is required
        if (StringUtils.isBlank(accessToken)) {
            logger.error("missing accessToken");
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("error", "missing_accessToken");
            jsonGenerator.writeEndObject();
            jsonGenerator.close();
            response.flushBuffer();
            return null;
        }
        
        // get ticket granting ticket
        TicketGrantingTicketImpl ticketGrantingTicketImpl = (TicketGrantingTicketImpl) ticketRegistry
            .getTicket(accessToken);
        if (ticketGrantingTicketImpl == null || ticketGrantingTicketImpl.isExpired()) {
            logger.error("expired accessToken : {}", accessToken);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("error", "expired_accessToken");
            jsonGenerator.writeEndObject();
            jsonGenerator.close();
            response.flushBuffer();
            return null;
        }
        
        // generate profile : identifier + attributes
        Principal principal = ticketGrantingTicketImpl.getAuthentication().getPrincipal();
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("id", principal.getId());
        jsonGenerator.writeArrayFieldStart("attributes");
        Map<String, Object> attributes = principal.getAttributes();
        for (String key : attributes.keySet()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField(key, attributes.get(key));
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
        jsonGenerator.close();
        response.flushBuffer();
        return null;
    }
}
