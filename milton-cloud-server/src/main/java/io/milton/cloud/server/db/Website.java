/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.db;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * A Website is a repository with a theme. The name of the repository is the DNS
 * name for it
 *
 * @author brad
 */
@Entity
@DiscriminatorValue("U")
public class Website extends Repository {

    private String theme;
    private String currentBranch;

    @Column
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getCurrentBranch() {
        return currentBranch;
    }

    public void setCurrentBranch(String currentBranch) {
        this.currentBranch = currentBranch;
    }

    public Branch currentBranch() {
        String branchName = getCurrentBranch();
        if (branchName == null || branchName.isEmpty()) {
            return null;
        }
        if (getBranches() != null) {
            for (Branch b : getBranches()) {
                if( b.getName().equals(branchName)) {
                    return b;
                }
            }
        }
        return null;
    }
}
