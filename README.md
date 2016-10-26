Milton-cloud
============

So ... I had set out to build an open source drop box clone in java. I'm not doing that
now and the server module is gone from this project, but i do have this sweet little VFS library.

What is Milton VFS?
==================
Its a versioned, distributed, virtual file system library. That means a java web app 
can use the milton-vfs api to persist data which looks like a file system. The data ultimately
gets stored in a database (using hibernate) and a blobstore. The blobstore is an
abstraction and can be a simple directory, or it can be a distributed, replicated,
store across a cluster accessed with HTTP.

Milton VFS uses a similar data structure as git, where cryptographic hashing
functions are used to identify resources. The result of this is that the 
state of a repository is known at every point in time, and you get cheap copies,
branching, etc.

Files are chunked when persisted using hashsplitting (with the hashsplit4j project).
Hashsplitting chunks files by looking for boundary sequences which are stable
as files are edited. This means that for a given edit to a file only one chunk 
will change. Chunks are keyed on their SHA1 so we never store the same chunks twice,
which means we effectively have block level de-duplication.

The end result of all of this is a high performance, extremely scaleable, and very
efficient (in terms of storage space) distributed file system which can be easily
integrated into database oriented web apps.

How to use it
=============

1. Setup hibernate
Firstly you need to have hibernate all setup and configured. There's example config
using spring in the milton mini project here:
https://github.com/miltonio/milton2/tree/master/apps/milton-mini-server/src/main/resources

In particular the database.xml file:

https://github.com/miltonio/milton2/blob/master/apps/milton-mini-server/src/main/resources/database.xml

2. You need a BlobStore and a HashStore
A simple BlobStore is org.hashsplit4j.store.FileSystemBlobStore which you can see here:
https://github.com/HashSplit4J/hashsplit-lib/blob/master/src/main/java/org/hashsplit4j/store/FileSystemBlobStore.java

And you probably want to use the DbHashStore that comes with milton vfs:
https://github.com/miltonio/milton-cloud/blob/master/milton-cloud-vfs/src/main/java/io/milton/vfs/content/DbHashStore.java

3. Create base data
Milton VFS uses the concept of a Repository to hold files, and a Repository contains
Branches, which contain commits. The most recent commit is the content of the branch.

So before you can save any content you need a Repository. Repositories are owned
by an entity, ie an organisation or a user (Profile). So before you can create a
Repository you need an entity, lets use an organisation. Every action requires
a user which is represented as a Profile, so we'll need one of them too

Once you have your Repository you can grab the live branch, create a DataSession
and start adding files.

So putting all that together:
```
Session session = ... (get a session from hibernate)
Transaction tx = session.beginTransaction();

Organisation rootOrg = new Organisation();
rootOrg.setAdminDomain("helloworld");
rootOrg.setModifiedDate(new Date());
rootOrg.setCreatedDate(new Date());
session.save(rootOrg);            

Profile t = new Profile();
t.setName("admin");
t.setNickName("admin);
t.setCreatedDate(new Date());
t.setModifiedDate(new Date());
t.setEmail("admin@mydomain.com");
session.save(t);

Repository repo = rootOrg.createRepository("first", t, session);
Branch trunk = repo.liveBranch();

// Now we can create a data session to manipulate the branch content
DataSession dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
DataSession.DirectoryNode rootDir = dataSession.getRootDataNode();

// add a directory
DataSession.DirectoryNode dir = rootDir.addDirectory("mydocs");

// Add a file
Parser parser = new Parser();
String fileHash = parser.parse(inputStream, hashStore, blobStore); // chunk the file into the stores
DataSession.FileNode newFile = dir.addFile("holidays.doc", fileHash);

// List the children of the directory
for( DataSession.DataNode node : dir ) {
    System.out( dir.getName() );
}

// Save the data session to create a new commit
dataSession.save(t);

// And commit the transaction
tx.commit();
```


Using the version history
=========================
Milton VFS gives you a complete version history of each branch in the form of a
list of Commits. To find the version history for a single file you need to walk
the list of commits and look for changes to that file

```
Branch trunk = repo.liveBranch();
List<Commit> commits = Commit.findByBranch(trunk, session);
// Display the commits
for( Commit commit : commits ) {
    // You can create a DataSession from a commit:
    DataSession dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);    
    display( dataSession.getRootDataNode() ); // display the files
}
```


Concurrency
===========
Ultimately each update to a branch sets a single value on the branch, which is a
pointer to the commit. The value which is set there needs to be derived from the
content of the hierarchy.

So if 2 files are added in concurrent operations only the second to be written
will be shown. There will end up being 2 commits, the first will contain only 
the first file and the second will contain only the second file.

So concurrency issues need to be addressed at a higher level then milton vfs


Access Control
==============

The milton vfs library includes classes suitable for use in persisting and applying
an access control scheme:
```
io.milton.vfs.db.Group
io.milton.vfs.db.GroupRole
io.milton.vfs.db.GroupMembership
```

However, no access control rules are applie within milton vfs.


Prerequisites
=============
 - java 7 JDK (yes, must be version 7!)
 
