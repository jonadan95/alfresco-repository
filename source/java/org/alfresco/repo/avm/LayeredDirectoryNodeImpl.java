/*
 * Copyright (C) 2006 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */

package org.alfresco.repo.avm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.alfresco.service.cmr.avm.AVMBadArgumentException;
import org.alfresco.service.cmr.avm.AVMCycleException;
import org.alfresco.service.cmr.avm.AVMException;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;

/**
 * A layered directory node.  A layered directory node points at
 * an underlying directory, which may or may not exist.  The visible
 * contents of a layered directory node is the contents of the underlying node
 * pointed at plus those nodes added to or modified in the layered directory node minus
 * those nodes which have been deleted in the layered directory node.
 * @author britt
 */
class LayeredDirectoryNodeImpl extends DirectoryNodeImpl implements LayeredDirectoryNode
{
    static final long serialVersionUID = 4623043057918181724L;

    /**
     * The layer id.
     */
    private long fLayerID;
    
    /**
     * The pointer to the underlying directory.
     */
    private String fIndirection;
    
    /**
     * Whether this is a primary indirection node.
     */
    private boolean fPrimaryIndirection;
    
    /**
     * Whether this is opaque.
     */
    private boolean fOpacity;
    
    /**
     * Default constructor. Called by Hibernate.
     */
    protected LayeredDirectoryNodeImpl()
    {
    }
    
    /**
     * Make a new one from a specified indirection path.
     * @param indirection The indirection path to set.
     * @param store The store that owns this node.
     */
    public LayeredDirectoryNodeImpl(String indirection, AVMStore store)
    {
        super(store.getAVMRepository().issueID(), store);
        fLayerID = -1;
        fIndirection = indirection;
        fPrimaryIndirection = true;
        fOpacity = false;
        AVMContext.fgInstance.fAVMNodeDAO.save(this);
        AVMContext.fgInstance.fAVMNodeDAO.flush();
    }
    
    /**
     * Kind of copy constructor, sort of.
     * @param other The LayeredDirectoryNode we are copied from.
     * @param repos The AVMStore object we use.
     */
    @SuppressWarnings("unchecked")
    public LayeredDirectoryNodeImpl(LayeredDirectoryNode other,
                                    AVMStore repos)
    {
        super(repos.getAVMRepository().issueID(), repos);
        fIndirection = other.getUnderlying();
        fPrimaryIndirection = other.getPrimaryIndirection();
        fLayerID = -1;
        fOpacity = false;
        AVMContext.fgInstance.fAVMNodeDAO.save(this);
        for (ChildEntry child : AVMContext.fgInstance.fChildEntryDAO.getByParent(other))
        {
            ChildEntryImpl newChild = new ChildEntryImpl(child.getName(),
                                                         this,
                                                         child.getChild());
            AVMContext.fgInstance.fChildEntryDAO.save(newChild);
        }
        AVMContext.fgInstance.fAVMNodeDAO.flush();
        copyProperties(other);
        copyAspects(other);
        copyACLs(other);
    }
    
    /**
     * Construct one from a PlainDirectoryNode.  Called when a COW is performed in a layered
     * context.
     * @param other The PlainDirectoryNode.
     * @param store The AVMStore we should belong to.
     * @param lPath The Lookup object.
     */
    @SuppressWarnings("unchecked")
    public LayeredDirectoryNodeImpl(PlainDirectoryNode other,
                                    AVMStore store,
                                    Lookup lPath,
                                    boolean copyContents)
    {
        super(store.getAVMRepository().issueID(), store);
        fIndirection = null;
        fPrimaryIndirection = false;
        fLayerID = -1;
        fOpacity = false;
        AVMContext.fgInstance.fAVMNodeDAO.save(this);
        if (copyContents)
        {
            for (ChildEntry child : AVMContext.fgInstance.fChildEntryDAO.getByParent(other))
            {
                ChildEntryImpl newChild = new ChildEntryImpl(child.getName(),
                                                             this,
                                                             child.getChild());
                AVMContext.fgInstance.fChildEntryDAO.save(newChild);
            }
        }
        AVMContext.fgInstance.fAVMNodeDAO.flush();
        copyProperties(other);
        copyAspects(other);
        copyACLs(other);
    }

    /**
     * Create a new layered directory based on a directory we are being named from
     * that is in not in the layer of the source lookup.
     * @param dir The directory
     * @param store The store
     * @param srcLookup The source lookup.
     * @param name The name of the target.
     */
    public LayeredDirectoryNodeImpl(DirectoryNode dir,
                                    AVMStore store,
                                    Lookup srcLookup,
                                    String name)
    {
        super(store.getAVMRepository().issueID(), store);
        fIndirection = srcLookup.getIndirectionPath() + "/" + name;
        fPrimaryIndirection = true;
        fLayerID = -1;
        fOpacity = false;
        AVMContext.fgInstance.fAVMNodeDAO.save(this);
        AVMContext.fgInstance.fAVMNodeDAO.flush();
        copyProperties(dir);
        copyAspects(dir);
        copyACLs(dir);
    }   
    
    /**
     * Is this a primary indirection node.
     * @return Whether this is a primary indirection.
     */
    public boolean getPrimaryIndirection()
    {
        return fPrimaryIndirection;
    }
    
    /**
     * Set the primary indirection state of this.
     * @param has Whether this is a primary indirection node.
     */
    public void setPrimaryIndirection(boolean has)
    {
        fPrimaryIndirection = has;
    }
    
    /**
     * Get the indirection path.
     * @return The indirection path.
     */
    public String getIndirection()
    {
        return fIndirection;
    }
    
    public String getUnderlying()
    {
        return fIndirection;
    }
    
    /**
     * Get the underlying path in the Lookup's context.
     * @param lPath The Lookup.
     * @return The underlying path.
     */
    public String getUnderlying(Lookup lPath)
    {
        if (fPrimaryIndirection)
        {
            return fIndirection;
        }
        return lPath.getCurrentIndirection();
    }
    
    /**
     * Get the layer id.
     * @return The layer id.
     */
    public long getLayerID()
    {
        return fLayerID;
    }
    
    /**
     * Set the layer id.
     * @param id The id to set.
     */
    public void setLayerID(long id)
    {
        fLayerID = id;
    }
    
    /**
     * Copy on write logic.
     * @param lPath
     * @return The copy or null.
     */
    public AVMNode copy(Lookup lPath)
    {
        // Capture the store.
        AVMStore store = lPath.getAVMStore();
        LayeredDirectoryNodeImpl newMe = null;
        if (!lPath.isInThisLayer())
        {
            // This means that this is being seen indirectly through the topmost
            // layer.  The following creates a node that will inherit its
            // indirection from its parent.
            newMe = new LayeredDirectoryNodeImpl((String)null, 
                                                 store);
            newMe.setPrimaryIndirection(false);
            newMe.setLayerID(lPath.getTopLayer().getLayerID());
        }
        else
        {
            // A simple copy is made.
            newMe = new LayeredDirectoryNodeImpl(this,
                                                 store);
            newMe.setLayerID(getLayerID());
        }
        newMe.setAncestor(this);
        return newMe;
    }

    /**
     * Insert a child node without COW.
     * @param name The name to give the child.
     */
    public void putChild(String name, AVMNode node)
    {
        ChildEntry existing = AVMContext.fgInstance.fChildEntryDAO.getByNameParent(name, this);
        if (existing != null)
        {
            existing.setChild(node);
            AVMContext.fgInstance.fChildEntryDAO.update(existing);
        }
        else
        {
            ChildEntry entry = new ChildEntryImpl(name, this, node);
            AVMContext.fgInstance.fAVMNodeDAO.flush();
            AVMContext.fgInstance.fChildEntryDAO.save(entry);
        }
    }


    /**
     * Does this node directly contain the indicated node.
     * @param node The node we are checking.
     * @return Whether node is directly contained.
     */
    public boolean directlyContains(AVMNode node)
    {
        return AVMContext.fgInstance.fChildEntryDAO.getByParentChild(this, node) != null;
    }

    /**
     * Get a listing of the virtual contents of this directory.
     * @param lPath The Lookup.
     * @return A Map from names to nodes. This is a sorted Map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, AVMNode> getListing(Lookup lPath)
    {
        // Get the base listing from the thing we indirect to.
        Map<String, AVMNode> listing = null;
        if (fOpacity)
        {
            listing = new HashMap<String, AVMNode>();
        }
        else
        {
            try
            {
                Lookup lookup = AVMRepository.GetInstance().lookupDirectory(-1, getUnderlying(lPath));
                DirectoryNode dir = (DirectoryNode)lookup.getCurrentNode();
                listing = dir.getListing(lookup);
            }
            catch (AVMException re)
            {
                if (re instanceof AVMCycleException)
                {
                    throw re;
                }
                // It's OK for an indirection to dangle.
                listing = new HashMap<String, AVMNode>();
            }
        }
        for (ChildEntry entry : AVMContext.fgInstance.fChildEntryDAO.getByParent(this))
        {
            if (entry.getChild().getType() == AVMNodeType.DELETED_NODE)
            {
                listing.remove(entry.getName());
            }
            else
            {
                listing.put(entry.getName(), entry.getChild());
            }
        }
        return listing;
    }

    /**
     * Get a listing of the nodes directly contained by a directory.
     * @param lPath The Lookup to this directory.
     * @return A Map of names to nodes.
     */
    public Map<String, AVMNode> getListingDirect(Lookup lPath)
    {
        Map<String, AVMNode> listing = new HashMap<String, AVMNode>();
        for (ChildEntry entry : AVMContext.fgInstance.fChildEntryDAO.getByParent(this))
        {
            if (entry.getChild().getType() != AVMNodeType.DELETED_NODE)
            {
                listing.put(entry.getName(), entry.getChild());
            }
        }
        return listing;
    }

    /**
     * Get a listing from a directory node descriptor.
     * @param dir The directory node descriptor.
     * @return A Map of names to node descriptors.
     */
    public SortedMap<String, AVMNodeDescriptor> getListing(AVMNodeDescriptor dir)
    {
        if (dir.getPath() == null || dir.getIndirection() == null)
        {
            throw new AVMBadArgumentException("Illegal null argument.");
        }
        SortedMap<String, AVMNodeDescriptor> baseListing = new TreeMap<String, AVMNodeDescriptor>();
        // If we are not opaque, get the underlying base listing.
        if (!fOpacity)
        {
            try
            {
                Lookup lookup = AVMRepository.GetInstance().lookupDirectory(-1, dir.getIndirection());
                DirectoryNode dirNode = (DirectoryNode)lookup.getCurrentNode();
                Map<String, AVMNode> listing = dirNode.getListing(lookup);
                for (String name : listing.keySet())
                {
                    baseListing.put(name,
                                    listing.get(name).getDescriptor(dir.getPath(), name,
                                                                    lookup.getCurrentIndirection()));
                }
            }
            catch (AVMException e)
            {
                if (e instanceof AVMCycleException)
                {   
                    throw e;
                }
            }
        }
        List<ChildEntry> children = AVMContext.fgInstance.fChildEntryDAO.getByParent(this);
        for (ChildEntry child : children)
        {
            if (child.getChild().getType() == AVMNodeType.DELETED_NODE)
            {
                baseListing.remove(child.getName());
            }
            else
            {
                baseListing.put(child.getName(),
                        child.getChild().getDescriptor(dir.getPath(),
                                child.getName(),
                                dir.getIndirection()));
            }
        }
        return baseListing;
    }

    /**
     * Get the names of nodes deleted in this directory.
     * @return A List of names.
     */
    public List<String> getDeletedNames()
    {
        List<ChildEntry> children = AVMContext.fgInstance.fChildEntryDAO.getByParent(this);
        List<String> listing = new ArrayList<String>();
        for (ChildEntry entry : children)
        {
            if (entry.getChild().getType() == AVMNodeType.DELETED_NODE)
            {
                listing.add(entry.getName());
            }
        }
        return listing;
    }

    /**
     * Lookup a child by name. 
     * @param lPath The Lookup.
     * @param name The name we are looking.
     * @param version The version in which we are looking.
     * @param write Whether this lookup is occurring in a write context.
     * @return The child or null if not found.
     */
    @SuppressWarnings("unchecked")
    public AVMNode lookupChild(Lookup lPath, String name, int version, boolean write)
    {
        ChildEntry entry = AVMContext.fgInstance.fChildEntryDAO.getByNameParent(name, this);
        if (entry != null)
        {
            if (entry.getChild().getType() == AVMNodeType.DELETED_NODE)
            {
                return null;
            }
            return AVMNodeUnwrapper.Unwrap(entry.getChild());
        }
        // Don't check our underlying directory if we are opaque.
        if (fOpacity)
        {
            return null;
        }
        // Not here so check our indirection.
        try
        {
            Lookup lookup = AVMRepository.GetInstance().lookupDirectory(-1, getUnderlying(lPath));
            DirectoryNode dir = (DirectoryNode)lookup.getCurrentNode();
            AVMNode retVal = dir.lookupChild(lookup, name, -1, false);
            lPath.setFinalStore(lookup.getFinalStore());
            return retVal;
        }
        catch (AVMException re)
        {
            if (re instanceof AVMCycleException)
            {
                throw re;
            }
            return null;
        }
    }

    /**
     * Lookup a child using a node descriptor as context.
     * @param mine The node descriptor for this,
     * @param name The name to lookup,
     * @return The node descriptor.
     */
    public AVMNodeDescriptor lookupChild(AVMNodeDescriptor mine, String name)
    {
        if (mine.getPath() == null || mine.getIndirection() == null)
        {
            throw new AVMBadArgumentException("Illegal null argument.");
        }
        ChildEntry entry = AVMContext.fgInstance.fChildEntryDAO.getByNameParent(name, this);
        if (entry != null)
        {
            if (entry.getChild().getType() == AVMNodeType.DELETED_NODE)
            {
                return null;
            }
            return entry.getChild().getDescriptor(mine.getPath(),
                                                  name,
                                                  mine.getIndirection());
        }
        // If we are opaque don't check underneath.
        if (fOpacity)
        {
            return null;
        }
        try
        {
            Lookup lookup = AVMRepository.GetInstance().lookupDirectory(-1, mine.getIndirection());
            DirectoryNode dir = (DirectoryNode)lookup.getCurrentNode();
            AVMNode child = dir.lookupChild(lookup, name, -1, false);
            if (child == null)
            {
                return null;
            }
            return child.getDescriptor(lookup);
        }
        catch (AVMException e)
        {
            if (e instanceof AVMCycleException)
            {
                throw e;
            }
            return null;
        }
    }

    /**
     * Directly remove a child. Do not COW. Do not pass go etc.
     * @param lPath The lookup that arrived at this.
     * @param name The name of the child to remove.
     */
    @SuppressWarnings("unchecked")
    public void removeChild(Lookup lPath, String name)
    {
        ChildEntry entry = AVMContext.fgInstance.fChildEntryDAO.getByNameParent(name, this);
        AVMNode child = null;
        if (entry != null)
        {
            child = entry.getChild();
            if (child.getType() == AVMNodeType.DELETED_NODE)
            {
                return;
            }
            AVMContext.fgInstance.fChildEntryDAO.delete(entry);
        }
        else
        {
            child = lookupChild(lPath, name, -1, false);
        }
        AVMNode ghost = new DeletedNodeImpl(lPath.getAVMStore().getAVMRepository().issueID(),
                lPath.getAVMStore());
        AVMContext.fgInstance.fAVMNodeDAO.save(ghost);
        AVMContext.fgInstance.fAVMNodeDAO.flush();
        ghost.setAncestor(child);
        this.putChild(name, ghost);
    }
    
    /**
     * Get the type of this node.
     * @return The type of this node.
     */
    public int getType()
    {
        return AVMNodeType.LAYERED_DIRECTORY;
    }

    /**
     * For diagnostics. Get a String representation.
     * @param lPath The Lookup.
     * @return A String representation.
     */
    public String toString(Lookup lPath)
    {
        return "[LD:" + getId() + ":" + getUnderlying(lPath) + "]";
    }
    
    /**
     * Set the primary indirection. No COW.
     * @param path The indirection path.
     */
    public void rawSetPrimary(String path)
    {
        fIndirection = path;
        fPrimaryIndirection = true;
    }
    
    /**
     * Make this node become a primary indirection.  COW.
     * @param lPath The Lookup.
     */
    public void turnPrimary(Lookup lPath)
    {
        String path = lPath.getCurrentIndirection();
        rawSetPrimary(path);
    }

    /**
     * Make this point at a new target.
     * @param lPath The Lookup.
     */
    public void retarget(Lookup lPath, String target)
    {
        rawSetPrimary(target);
    }
    
    /**
     * Let anything behind name in this become visible.
     * @param lPath The Lookup.
     * @param name The name to uncover.
     */
    public void uncover(Lookup lPath, String name)
    {
        ChildEntry entry = AVMContext.fgInstance.fChildEntryDAO.getByNameParent(name, this);
        if (entry != null)
        {
            AVMContext.fgInstance.fChildEntryDAO.delete(entry);
        }
    }
    
    /**
     * Get the descriptor for this node.
     * @param lPath The Lookup.
     * @return A descriptor.
     */
    public AVMNodeDescriptor getDescriptor(Lookup lPath, String name)
    {
        BasicAttributes attrs = getBasicAttributes();
        String path = lPath.getRepresentedPath();
        if (path.endsWith("/"))
        {
            path = path + name;
        }
        else
        {
            path = path + "/" + name;
        }
        return new AVMNodeDescriptor(path,
                                     name,
                                     AVMNodeType.LAYERED_DIRECTORY,
                                     attrs.getCreator(),
                                     attrs.getOwner(),
                                     attrs.getLastModifier(),
                                     attrs.getCreateDate(),
                                     attrs.getModDate(),
                                     attrs.getAccessDate(),
                                     getId(),
                                     getVersionID(),
                                     getUnderlying(lPath),
                                     fPrimaryIndirection,
                                     fLayerID,
                                     fOpacity,
                                     -1);
    }

    /**
     * Get the descriptor for this node.
     * @param lPath The Lookup.
     * @return A descriptor.
     */
    public AVMNodeDescriptor getDescriptor(Lookup lPath)
    {
        BasicAttributes attrs = getBasicAttributes();
        String path = lPath.getRepresentedPath();
        String name = path.substring(path.lastIndexOf("/") + 1);
        return new AVMNodeDescriptor(path,
                                     name,
                                     AVMNodeType.LAYERED_DIRECTORY,
                                     attrs.getCreator(),
                                     attrs.getOwner(),
                                     attrs.getLastModifier(),
                                     attrs.getCreateDate(),
                                     attrs.getModDate(),
                                     attrs.getAccessDate(),
                                     getId(),
                                     getVersionID(),
                                     getUnderlying(lPath),
                                     fPrimaryIndirection,
                                     fLayerID,
                                     fOpacity,
                                     -1);
    }

    /**
     * Get a descriptor for this.
     * @param parentPath The parent path.
     * @param name The name this was looked up with.
     * @param parentIndirection The indirection of the parent.
     * @return The descriptor.
     */
    public AVMNodeDescriptor getDescriptor(String parentPath, String name, String parentIndirection)
    {
        BasicAttributes attrs = getBasicAttributes();
        String path = parentPath.endsWith("/") ? parentPath + name : parentPath + "/" + name;
        String indirection = null;
        if (fPrimaryIndirection)
        {
            indirection = fIndirection;
        }
        else
        {
            indirection = parentIndirection.endsWith("/") ? parentIndirection + name : 
                parentIndirection + "/" + name;
        }
        return new AVMNodeDescriptor(path,
                                     name,
                                     AVMNodeType.LAYERED_DIRECTORY,
                                     attrs.getCreator(),
                                     attrs.getOwner(),
                                     attrs.getLastModifier(),
                                     attrs.getCreateDate(),
                                     attrs.getModDate(),
                                     attrs.getAccessDate(),
                                     getId(),
                                     getVersionID(),
                                     indirection,
                                     fPrimaryIndirection,
                                     fLayerID,
                                     fOpacity,
                                     -1);
    }

    /**
     * Set the indirection.
     * @param indirection
     */
    public void setIndirection(String indirection)
    {
        fIndirection = indirection;
    }

    /**
     * Does nothing because LayeredDirectoryNodes can't be roots.
     * @param isRoot
     */
    public void setIsRoot(boolean isRoot)
    {
    }

    /**
     * Get the opacity of this.
     * @return The opacity.
     */
    public boolean getOpacity()
    {
        return fOpacity;
    }

    /**
     * Set the opacity of this, ie, whether it blocks things normally
     * seen through its indirection.
     * @param opacity
     */
    public void setOpacity(boolean opacity)
    {
        fOpacity = opacity;
    }
}
