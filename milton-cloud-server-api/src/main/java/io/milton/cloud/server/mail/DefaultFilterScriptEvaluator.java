/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.mail;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.process.ProcessContext;
import io.milton.cloud.process.Rule;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.web.PrincipalResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.UserResource;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.context.RequestContext;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;

/**
 *
 * @author brad
 */
public class DefaultFilterScriptEvaluator implements FilterScriptEvaluator {

    private final ScriptParser scriptParser;
    private final CurrentDateService currentDateService;
    private final Formatter formatter;

    public DefaultFilterScriptEvaluator(ScriptParser scriptParser, CurrentDateService currentDateService, Formatter formatter) {
        this.scriptParser = scriptParser;
        this.currentDateService = currentDateService;
        this.formatter = formatter;
    }

    @Override
    public boolean checkFilterScript(EvaluationContext context, Profile p, Organisation org, RootFolder rf) {
        System.out.println("checkFilterScript: " + p.getEmail());
        Rule rule = (Rule) context.getCompiledScript();
        if (rule == null) {
            scriptParser.parse(context);
            rule = (Rule) context.getCompiledScript();
        }

        RequestContext rctx = RequestContext.getCurrent();
        ProcessContext processContext = new ProcessContext(rctx);
        processContext.put(context);
        processContext.addAttribute("rootFolder", rf);
        processContext.addAttribute("profile", p);
        processContext.addAttribute("organisation", org);
        processContext.addAttribute("website", rf);
        PrincipalResource userRes = null;
        try {
            userRes = rf.findEntity(p);
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }

        processContext.addAttribute("user", userRes);
        processContext.addAttribute("userResource", userRes);
        processContext.addAttribute("formatter", formatter);
        boolean b = rule.eval(processContext);
        System.out.println("result=" + b);
        return b;
    }
}
