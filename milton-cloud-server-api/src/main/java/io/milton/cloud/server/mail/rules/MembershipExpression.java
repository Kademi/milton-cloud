/*
 */
package io.milton.cloud.server.mail.rules;

import io.milton.cloud.process.Expression;
import io.milton.cloud.process.ProcessContext;
import io.milton.cloud.server.mail.EvaluationContext;
import io.milton.context.Registration;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.util.List;

/**
 * Finds the membership for the given group for the current user. Returns the
 * first found
 *
 * @author brad
 */
public class MembershipExpression implements Expression<Object> {

    private List<String> groupNames;
    private Expression child;

    public MembershipExpression(List<String> groupNames, Expression child) {
        this.groupNames = groupNames;
        this.child = child;
    }

    @Override
    public Object eval(ProcessContext context) {
        Profile p = context.get(Profile.class);
        EvaluationContext evaluationContext = context.get(EvaluationContext.class);
        Organisation org = (Organisation) evaluationContext.getAttributes().get("org"); // See EmailTriggerService
        for (String groupName : groupNames) {
            Group g = org.group(groupName, SessionManager.session());
            GroupMembership memberOrg = p.membership(g);
            if (memberOrg != null) {
                if (child != null) {
                    Registration<GroupMembership> reg = context.put(memberOrg);
                    try {
                        return child.eval(context);
                    } finally {
                        reg.remove();
                    }
                }
                return memberOrg;
            }
        }
        return null;
    }
}
