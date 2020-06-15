/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.site;

import org.alfresco.service.cmr.site.SiteInfo;

/**
 * Conveys information for a member of a site.
 * 
 * @author steveglover
 *
 */
public class SiteMembership
{
    private SiteInfo siteInfo;
    private String id;          // contains both userId and authority Id
    private String role;

    // backward compatibility
    private String firstName;
    private String lastName;

    /*
     * deprecated from 7.0.0
     */
    public SiteMembership(SiteInfo siteInfo, String id, String firstName, String lastName,
            String role)
    {
        super();
        if (siteInfo == null)
        {
            throw new java.lang.IllegalArgumentException();
        }
        if (id == null)
        {
            throw new java.lang.IllegalArgumentException(
                    "Id required building site membership of " + siteInfo.getShortName());
        }
        if (firstName == null)
        {
            throw new java.lang.IllegalArgumentException(
                    "FirstName required building site membership of " + siteInfo.getShortName());
        }
        if (lastName == null)
        {
            throw new java.lang.IllegalArgumentException(
                    "LastName required building site membership of " + siteInfo.getShortName());
        }
        if (role == null)
        {
            throw new java.lang.IllegalArgumentException(
                    "Role required building site membership of " + siteInfo.getShortName());
        }
        this.siteInfo = siteInfo;
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public SiteMembership(SiteInfo siteInfo, String id, String role)
    {
        super();
        if (siteInfo == null)
        {
            throw new java.lang.IllegalArgumentException();
        }
        if (id == null)
        {
            throw new java.lang.IllegalArgumentException(
                    "Id required building site membership of " + siteInfo.getShortName());
        }
        if (role == null)
        {
            throw new java.lang.IllegalArgumentException(
                    "Role required building site membership of " + siteInfo.getShortName());
        }

        this.siteInfo = siteInfo;
        this.id = id;
        this.role = role;
    }

    public SiteInfo getSiteInfo()
    {
        return siteInfo;
    }

    /** deprecated from 7.0.0 use getId instead */
    public String getPersonId() {
        return id;
    }

    public String getId() {
        return id;
    }

    /** deprecated from 7.0.0 use SiteMember instead */
    public String getFirstName()
    {
        return firstName;
    }

    /** deprecated from 7.0.0 use SiteMember instead */
    public String getLastName()
    {
        return lastName;
    }

    public String getRole()
    {
        return role;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + ((getSiteInfo() == null) ? 0 : getSiteInfo().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SiteMembership other = (SiteMembership) obj;
        if (id == null)
        {
            if (other.id != null)
                return false;
        }
        else if (!id.equals(other.id))
            return false;
        if (role != other.role)
            return false;
        if (getSiteInfo() == null)
        {
            if (other.getSiteInfo() != null)
                return false;
        }
        else if (!getSiteInfo().equals(other.getSiteInfo()))
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "SiteMembership [siteInfo=" + getSiteInfo() + ", id=" + id
                + ", firstName=" + firstName + ", lastName=" + lastName + ", role=" + role + "]";
    }

}
